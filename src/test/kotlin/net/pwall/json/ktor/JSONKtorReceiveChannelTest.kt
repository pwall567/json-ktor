/*
 * @(#) JSONKtorReceiveChannelTest.kt
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

import kotlin.test.Test
import kotlin.test.expect

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

import net.pwall.json.TestListEntry
import net.pwall.json.stringifyJSON
import net.pwall.json.ktor.JSONKtorFunctions.jsonKtor
import net.pwall.util.Strings

class JSONKtorReceiveChannelTest {

    @ExperimentalCoroutinesApi
    @Test fun `should receive JSON channel`() {
        withTestApplication(Application::testReceiveChannelModule) {
            val numbers: List<TestListEntry> = List(8000) { i -> TestListEntry(i, Strings.toEnglish(i)) }
            val body = numbers.stringifyJSON()

            expect("one") {
                handleRequest(HttpMethod.Post, "/1") {
                    addHeader("Content-Type", "application/json")
                    setBody(body)
                }.response.content
            }

            expect("six thousand, nine hundred and fifty-four") {
                handleRequest(HttpMethod.Post, "/6954") {
                    addHeader("Content-Type", "application/json")
                    setBody(body)
                }.response.content
            }
        }
    }
}

@ExperimentalCoroutinesApi
fun Application.testReceiveChannelModule() {
    install(ContentNegotiation) {
        jsonKtor {}
    }
    routing {
        post("/{num}") {
            val num = call.parameters["num"]?.toIntOrNull() ?: throw IllegalArgumentException()
            val channelInput = call.receive<ReceiveChannel<TestListEntry>>()
            while (true) {
                if (channelInput.isClosedForReceive) {
                    call.respond(HttpStatusCode.NotFound)
                    break
                }
                val entry = channelInput.receive()
                if (entry.number == num) {
                    channelInput.cancel()
                    call.respondText(entry.text, ContentType.Text.Plain)
                    break
                }
            }
        }
    }
}
