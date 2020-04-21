/*
 * @(#) JSONKtor.kt
 *
 * json-ktor JSON functionality for ktor
 * Copyright (c) 2019, 2020 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.pwall.json.ktor

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KType

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.io.ByteReadChannel

import java.nio.charset.Charset

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.ContentConverter
import io.ktor.features.ContentNegotiation
import io.ktor.features.suitableCharset
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.request.contentCharset
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext

import net.pwall.json.JSONConfig
import net.pwall.json.JSONDeserializer
import net.pwall.json.JSONException
import net.pwall.json.stream.JSONArrayCoPipeline
import net.pwall.json.stream.JSONDeserializerCoPipeline
import net.pwall.json.stream.JSONStream
import net.pwall.json.stringifyJSON
import net.pwall.util.pipeline.ChannelCoAcceptor
import net.pwall.util.pipeline.CoDecoderFactory
import net.pwall.util.pipeline.DecoderFactory
import net.pwall.util.pipeline.IntCoAcceptor
import net.pwall.util.pipeline.simpleCoAcceptor

/**
 * Content converter for ktor - converts from/to JSON using the [json-kotlin](https://github.com/pwall567/json-kotlin)
 * library.
 *
 * @property    config  an optional [JSONConfig]
 * @constructor         creates a `JSONKtor` object for use in ktor `ContentNegotiation` configuration.
 * @author  Peter Wall
 */
class JSONKtor(private val config: JSONConfig = JSONConfig.defaultConfig) : ContentConverter {

    /**
     * Convert a value for sending (serialize to JSON).
     *
     * @param   context     the [PipelineContext]
     * @param   contentType the content type (must be `application/json`)
     * @param   value       the value to be converted
     * @return              the converted value as an [OutgoingContent]
     */
    @KtorExperimentalAPI
    override suspend fun convertForSend(context: PipelineContext<Any, ApplicationCall>, contentType: ContentType,
            value: Any): OutgoingContent? {
        if (contentType != ContentType.Application.Json)
            return null
        val json = value.stringifyJSON(config)
        return TextContent(json, contentType.withCharset(context.call.suitableCharset()))
    }

    /**
     * Convert a received value (deserialize from JSON).  This uses a pipelining JSON library to parse JSON on the fly,
     * and in the case of [Channel] or [Flow] target types, starts a coroutine to produce the results asynchronously.
     *
     * @param   context     the [PipelineContext]
     * @return              the converted value
     */
    @ExperimentalCoroutinesApi
    @KtorExperimentalAPI
    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val channel = request.value as? ByteReadChannel ?: return null
        val charset = context.call.request.contentCharset() ?: config.charset
        return when (request.type) {
            Flow::class -> receiveFlow(request, channel, charset)
            Channel::class, ReceiveChannel::class -> receiveChannel(request, context.call, channel, charset)
            else -> receiveNormal(request, channel, charset)
        }
    }

    /**
     * Deserialize a "normal" type, i.e. not a [Channel] or [Flow].
     *
     * @param   request     the [ApplicationReceiveRequest]
     * @param   channel     the [ByteReadChannel]
     * @param   charset     the [Charset] to use
     * @return              the deserialized value
     */
    @KtorExperimentalAPI
    suspend fun receiveNormal(request: ApplicationReceiveRequest, channel: ByteReadChannel, charset: Charset): Any? {
        val buffer = ByteArray(config.readBufferSize)
        val pipeline = DecoderFactory.getDecoder(charset, JSONStream())
        while (true) {
            val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
            if (bytesRead < 0)
                break
            for (i in 0 until bytesRead)
                pipeline.acceptInt(buffer[i].toInt() and 0xFF)
            if (channel.isClosedForRead)
                break
        }
        pipeline.close()
        return JSONDeserializer.deserialize(request.typeInfo, pipeline.result, config)
    }

    /**
     * Deserialize a [Flow].
     *
     * @param   request     the [ApplicationReceiveRequest]
     * @param   channel     the [ByteReadChannel]
     * @param   charset     the [Charset] to use
     * @return              a [Flow] of the deserialized values
     */
    @KtorExperimentalAPI
    suspend fun receiveFlow(request: ApplicationReceiveRequest, channel: ByteReadChannel, charset: Charset):
            Flow<Any?> {
        val targetType = request.targetType()
        return flow {
            val pipeline = CoDecoderFactory.getDecoder(charset, JSONArrayCoPipeline(simpleCoAcceptor {
                emit(JSONDeserializer.deserialize(targetType, it, config))
            }))
            channel.copyToPipeline(pipeline)
        }
    }

    /**
     * Deserialize a [Channel].
     *
     * @param   request     the [ApplicationReceiveRequest]
     * @param   call        the [ApplicationCall]
     * @param   channel     the [ByteReadChannel]
     * @param   charset     the [Charset] to use
     * @return              a [Flow] of the deserialized values
     */
    @ExperimentalCoroutinesApi
    @KtorExperimentalAPI
    suspend fun receiveChannel(request: ApplicationReceiveRequest, call: ApplicationCall, channel: ByteReadChannel,
            charset: Charset): ReceiveChannel<Any?> {
        val targetType = request.targetType()
        return call.application.produce(JSONReceiveCoroutine(targetType)) {
            val jsonPipeline = JSONDeserializerCoPipeline<Any, Unit>(targetType, ChannelCoAcceptor(this), config)
            val pipeline = CoDecoderFactory.getDecoder(charset, JSONArrayCoPipeline(jsonPipeline))
            channel.copyToPipeline(pipeline)
        }
    }

    /**
     * Find the target type parameter for a generic class.
     */
    @KtorExperimentalAPI
    private fun ApplicationReceiveRequest.targetType() = typeInfo.arguments.firstOrNull()?.type ?:
            throw JSONException("Insufficient type information to deserialize generic class")

    /**
     * Copy data from the [ByteReadChannel] to an [IntCoAcceptor].
     */
    private suspend fun ByteReadChannel.copyToPipeline(acceptor: IntCoAcceptor<*>) {
        val buffer = ByteArray(config.readBufferSize)
        while (true) {
            val bytesRead = readAvailable(buffer, 0, buffer.size)
            if (bytesRead < 0)
                break
            for (i in 0 until bytesRead)
                acceptor.accept(buffer[i].toInt() and 0xFF)
            if (isClosedForRead)
                break
        }
        acceptor.close()
    }

}

/**
 * A [CoroutineContext] for [Channel] deserialization coroutines.
 */
data class JSONReceiveCoroutine(val type: KType) : AbstractCoroutineContextElement(JSONReceiveCoroutine) {
    companion object Key : CoroutineContext.Key<JSONReceiveCoroutine>
}

/**
 * Register the content converter, supplying the [JSONConfig] to be used by it.
 *
 * @param   contentType the content type
 * @param   config      a [JSONConfig]
 */
fun ContentNegotiation.Configuration.jsonKtor(config: JSONConfig,
        contentType: ContentType = ContentType.Application.Json) {
    register(contentType, JSONKtor(config))
}

/**
 * Register the content converter and configure a new [JSONConfig] to be used by it.
 *
 * @param   contentType the content type
 * @param   block       a block of code to initialise the [JSONConfig]
 */
fun ContentNegotiation.Configuration.jsonKtor(contentType: ContentType = ContentType.Application.Json,
        block: JSONConfig.() -> Unit = {}) {
    register(contentType, JSONKtor(JSONConfig().apply(block)))
}
