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

import kotlinx.coroutines.io.ByteReadChannel

import java.nio.charset.Charset

import io.ktor.application.ApplicationCall
import io.ktor.features.ContentConverter
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.util.pipeline.PipelineContext

import net.pwall.json.JSONAuto
import net.pwall.json.JSONConfig
import net.pwall.json.JSONSerializer

/**
 * Content converter for ktor - converts JSON using the `json-kotlin` library.
 *
 * @author  Peter Wall
 */
class JSONKtor(val config: JSONConfig? = null) : ContentConverter {

    override suspend fun convertForSend(context: PipelineContext<Any, ApplicationCall>, contentType: ContentType,
            value: Any): Any? {
        if (contentType != ContentType.Application.Json)
            return null
        val json = JSONSerializer.serialize(value, config)?.toJSON() ?: "null"
        return TextContent(json, contentType.withCharset(Charset.forName("UTF-8")))
    }

    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val value = request.value as? ByteReadChannel ?: return null
        val content = value.readRemaining(20000, 2000).readText() // 20000 is arbitrary maximum size (as is 2000)
        return JSONAuto.parse(request.type, content) // TODO change to use config
    }

}

/**
 * Configure the [JSONConfig] used for this converter.
 */
fun ContentNegotiation.Configuration.jsonKtor(contentType: ContentType = ContentType.Application.Json,
        block: JSONConfig.() -> Unit = {}) {
    register(contentType, JSONKtor(JSONConfig().apply(block)))
}
