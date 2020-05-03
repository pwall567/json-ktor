/*
 * @(#) JSONKtorFunctions.kt
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

import java.nio.charset.Charset

import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.withCharset
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel

import net.pwall.json.JSONCoStringify.outputJSON
import net.pwall.json.JSONConfig
import net.pwall.util.pipeline.AbstractIntCoAcceptor
import net.pwall.util.pipeline.CoEncoderFactory
import net.pwall.util.pipeline.IntCoAcceptor

@Suppress("unused")
object JSONKtorFunctions {

    /**
     * Create an [OutgoingContent] to output the JSON for the value to a provided [ByteWriteChannel].
     */
    fun createOutgoingContent(value: Any?, contentType: ContentType = ContentType.Application.Json,
            charset: Charset = Charsets.UTF_8, config: JSONConfig = JSONConfig.defaultConfig): OutgoingContent {
        return object : OutgoingContent.WriteChannelContent() {
            override val contentType: ContentType = contentType.withCharset(charset)
            override suspend fun writeTo(channel: ByteWriteChannel) {
                val pipeline = CoEncoderFactory.getEncoder(charset, ByteChannelCoAcceptor(channel))
                pipeline.outputJSON(value, config)
                pipeline.close()
            }
        }
    }

    /**
     * Copy data from the [ByteReadChannel] to an [IntCoAcceptor].
     */
    suspend fun ByteReadChannel.copyToPipeline(acceptor: IntCoAcceptor<*>, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        val buffer = ByteArray(bufferSize)
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

    /**
     * Register the content converter, supplying the [JSONConfig] to be used by it.
     *
     * @param   config      a [JSONConfig]
     * @param   contentType the content type (default `application/json`)
     * @receiver            the [ContentNegotiation.Configuration] being used to configure ktor
     */
    fun ContentNegotiation.Configuration.jsonKtor(config: JSONConfig,
            contentType: ContentType = ContentType.Application.Json) {
        register(contentType, JSONKtor(contentType, config))
    }

    /**
     * Register the content converter and configure a new [JSONConfig] to be used by it.
     *
     * @param   contentType the content type (default `application/json`)
     * @param   block       a block of code to initialise the [JSONConfig]
     * @receiver            the [ContentNegotiation.Configuration] being used to configure ktor
     */
    fun ContentNegotiation.Configuration.jsonKtor(contentType: ContentType = ContentType.Application.Json,
            block: JSONConfig.() -> Unit = {}) {
        register(contentType, JSONKtor(contentType, JSONConfig().apply(block)))
    }

    /**
     * An implementation of [IntCoAcceptor] that sends the value to a [ByteWriteChannel].
     *
     * The [Int] values are expected to be in the range 0..255, i.e. byte values.  That makes this class suitable as the
     * downstream acceptor for an encoder pipeline.
     */
    private class ByteChannelCoAcceptor(private val channel: ByteWriteChannel) : AbstractIntCoAcceptor<Unit>() {

        /**
         * Accept a value, after `closed` check and test for end of data.  Send the value to the [ByteWriteChannel].
         *
         * @param   value       the input value
         */
        override suspend fun acceptInt(value: Int) {
            channel.writeByte(value.toByte())
        }

        /**
         * Close the acceptor.
         */
        override fun close() {
            super.close()
            channel.close(null)
        }

        /**
         * Flush the output to the channel.
         */
        override fun flush() {
            channel.flush()
        }

    }

}
