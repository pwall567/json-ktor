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
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext

import net.pwall.json.JSON
import net.pwall.json.JSONConfig
import net.pwall.json.JSONDeserializer
import net.pwall.json.JSONSerializer
import net.pwall.util.Strings

/**
 * Content converter for ktor - converts JSON using the `json-kotlin` library.
 *
 * @author  Peter Wall
 */
class JSONKtor(val config: JSONConfig? = null) : ContentConverter {

    @KtorExperimentalAPI
    override suspend fun convertForSend(context: PipelineContext<Any, ApplicationCall>, contentType: ContentType,
            value: Any): Any? {
        if (contentType != ContentType.Application.Json)
            return null
        val json = JSONSerializer.serialize(value, config)?.toJSON() ?: "null"
        return TextContent(json, contentType.withCharset(context.call.suitableCharset()))
    }

    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val channel = request.value as? ByteReadChannel ?: return null
        return JSONDeserializer.deserialize(request.type.starProjectedType, JSON.parse(readAll(channel)), config)
    }

}

/**
 * Configure the [JSONConfig] used for this converter.
 */
fun ContentNegotiation.Configuration.jsonKtor(contentType: ContentType = ContentType.Application.Json,
        block: JSONConfig.() -> Unit = {}) {
    register(contentType, JSONKtor(JSONConfig().apply(block)))
}

const val bufferSize = 8192

/**
 * Read all data from the `ByteReadChannel` and convert from UTF-8.
 *
 * There may be a pre-existing function that does this, but if so it wasn't obvious!
 *
 * @param   channel the `ByteReadChannel`
 * @return          the data as a string, decoded from UTF-8
 */
suspend fun readAll(channel: ByteReadChannel): String {
    val bufferList: MutableList<ByteBuffer> = ArrayList()
    var buffer = ByteBuffer.allocate(bufferSize)
    while (!channel.isClosedForRead) {
        channel.readAvailable(buffer)
        if (!buffer.hasRemaining()) {
            buffer.flip()
            bufferList.add(buffer)
            buffer = ByteBuffer.allocate(bufferSize)
        }
    }
    buffer.flip()
    bufferList.add(buffer)
    return Strings.fromUTF8(bufferList.toTypedArray())
}
