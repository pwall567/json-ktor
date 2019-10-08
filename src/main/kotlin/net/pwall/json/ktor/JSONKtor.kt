/*
 * @(#) JSONKtor.kt
 *
 * json-ktor JSON functionality for ktor
 * Copyright (c) 2019 Peter Wall
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

import kotlin.reflect.full.starProjectedType
import kotlinx.coroutines.io.ByteReadChannel

import java.nio.ByteBuffer

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.ContentConverter
import io.ktor.features.ContentNegotiation
import io.ktor.features.suitableCharset
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.request.contentCharset
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext

import net.pwall.json.JSON
import net.pwall.json.JSONConfig
import net.pwall.json.JSONDeserializer
import net.pwall.json.JSONSerializer

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
     * @return              the converted value as a [TextContent]
     */
    @KtorExperimentalAPI
    override suspend fun convertForSend(context: PipelineContext<Any, ApplicationCall>, contentType: ContentType,
            value: Any): Any? {
        if (contentType != ContentType.Application.Json)
            return null
        val json = JSONSerializer.serialize(value, config)?.toJSON() ?: "null"
        return TextContent(json, contentType.withCharset(context.call.suitableCharset()))
    }

    /**
     * Convert a received value (deserialize from JSON).
     *
     * @param   context     the [PipelineContext]
     * @return              the converted value
     */
    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val channel = request.value as? ByteReadChannel ?: return null
        val charSet = context.call.request.contentCharset() ?: config.charset
        val json = charSet.decode(readAll(channel, config.readBufferSize)).toString()
        return JSONDeserializer.deserialize(request.type.starProjectedType, JSON.parse(json), config)
    }

}

/**
 * Register the content converter and configure the [JSONConfig] used by it.
 *
 * @param   contentType the content type
 * @param   block       a block of code to initialise the [JSONConfig]
 */
fun ContentNegotiation.Configuration.jsonKtor(contentType: ContentType = ContentType.Application.Json,
        block: JSONConfig.() -> Unit = {}) {
    register(contentType, JSONKtor(JSONConfig().apply(block)))
}

/**
 * Read all data from a [ByteReadChannel], returning a [ByteBuffer].  Because the total data length is initially
 * unknown, the function first allocates a buffer of the size nominated; if this is filled the function reads multiple
 * buffers into a list and then aggregates them into a single buffer.
 *
 * There may be a standard library function that does something like this, but if so it wasn't obvious!
 *
 * @param   channel     the [ByteReadChannel]
 * @param   bufferSize  the buffer size
 * @return              the [ByteBuffer]
 */
suspend fun readAll(channel: ByteReadChannel, bufferSize: Int): ByteBuffer {
    var buffer = ByteBuffer.allocate(bufferSize)
    while (true) {
        channel.readAvailable(buffer)
        if (channel.isClosedForRead)
            break
        if (!buffer.hasRemaining()) {
            // too much for one buffer - need to start a list
            buffer.flip()
            val bufferList = arrayListOf(buffer)
            buffer = ByteBuffer.allocate(bufferSize)
            while (true) {
                channel.readAvailable(buffer)
                if (channel.isClosedForRead)
                    break
                if (!buffer.hasRemaining()) {
                    buffer.flip()
                    bufferList.add(buffer)
                    buffer = ByteBuffer.allocate(bufferSize)
                }
            }
            buffer.flip()
            bufferList.add(buffer)
            // now combine the buffers in the list into a single buffer
            buffer = ByteBuffer.allocate(bufferList.sumBy { it.remaining() })
            bufferList.forEach { buffer.put(it) }
            break
        }
    }
    buffer.flip()
    return buffer
}
