/*
 * @(#) JSONKtorCustomSerializationTest.kt
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
import kotlin.test.fail

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

import net.pwall.json.Dummy1
import net.pwall.json.JSONConfig
import net.pwall.json.JSONObject
import net.pwall.json.ktor.JSONKtorFunctions.jsonKtor
import net.pwall.json.test.JSONExpect.Companion.expectJSON

class JSONKtorCustomSerializationTest {

    @Test fun `test configuration with custom serialization and deserialization`() {

        withTestApplication(Application::testApp2) {

            val result = handleRequest(HttpMethod.Post, "/x") {
                addHeader("Content-Type", "application/json")
                setBody("""{"a":"ABC","b":789}""")
            }.response.content ?: fail("Response is null")
            expectJSON(result) {
                count(2)
                property("a", "XXXABCYYY")
                property("b", 1578)
            }

        }

    }

}

fun Application.testApp2() {

    val config = JSONConfig().apply {
        fromJSON { json ->
            (json as? JSONObject)?.let { Dummy1(it.getString("a"), it.getInt("b")) }
        }
        toJSON<Dummy1> {
            it?.let { JSONObject().putValue("a", it.field1).putValue("b", it.field2) }
        }
    }

    install(ContentNegotiation) {
        jsonKtor(config)
    }

    routing {
        post("/x") {
            val jsonInput = call.receive<Dummy1>()
            call.respond(Dummy1("XXX${jsonInput.field1}YYY", jsonInput.field2 * 2))
        }
    }

}
