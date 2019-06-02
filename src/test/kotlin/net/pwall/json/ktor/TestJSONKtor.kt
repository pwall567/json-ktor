/*
 * @(#) TestJSONKtor.kt
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

import kotlin.test.Test
import kotlin.test.assertEquals

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

import net.pwall.json.JSONAuto
import net.pwall.json.JSONObject
import net.pwall.util.Strings

class TestJSONKtor {

    @Test fun `test simple configuration`() {

        withTestApplication(Application::testApp) {

            val numbers: List<String> = List(8000) { i -> Strings.toEnglish(i) }
            val testCall1 = handleRequest(HttpMethod.Post, "/1") {
                addHeader("Content-Type", "application/json")
                setBody(JSONAuto.stringify(Dummy1(numbers)))
            }
            assertEquals("one", testCall1.response.content)

            val testCall2 = handleRequest(HttpMethod.Post, "/7682") {
                addHeader("Content-Type", "application/json")
                setBody(JSONAuto.stringify(Dummy1(numbers)))
            }
            assertEquals("seven thousand, six hundred and eighty-two", testCall2.response.content)

        }

    }

    @Test fun `test configuration with custom serialisation`() {

        withTestApplication(Application::testApp2) {

            val testCall1 = handleRequest(HttpMethod.Post, "/x") {
                addHeader("Content-Type", "application/json")
                setBody("""{"a":"ABC","b":789}""")
            }
            assertEquals("ABC=789", testCall1.response.content)

            val testCall2 = handleRequest(HttpMethod.Get, "/y")
            assertEquals("""{"a":"FGH","b":9000}""", testCall2.response.content)

        }

    }

}

fun Application.testApp() {
    install(ContentNegotiation) {
        jsonKtor {}
    }
    routing {
        post("/{num}") {
            val num = call.parameters["num"]?.toIntOrNull() ?: throw IllegalArgumentException()
            val jsonInput = call.receive<Dummy1>()
            call.respondText(jsonInput.list[num], ContentType.Text.Plain)
        }
    }
}

fun Application.testApp2() {
    install(ContentNegotiation) {
        jsonKtor {
            fromJSON { json ->
                (json as? JSONObject)?.let { Dummy2(it.getString("a"), it.getInt("b")) }
            }
            toJSON<Dummy2> {
                if (it == null) null else JSONObject().putValue("a", it.str).putValue("b", it.num)
            }
        }
    }
    routing {
        post("/x") {
            val jsonInput = call.receive<Dummy2>()
            call.respondText("${jsonInput.str}=${jsonInput.num}", ContentType.Text.Plain)
        }
        get("/y") {
            call.respond(Dummy2("FGH", 9000))
        }
    }
}

data class Dummy1(val list: List<String>)

data class Dummy2(val str: String, val num: Int)
