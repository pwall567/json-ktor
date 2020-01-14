/*
 * @(#) JSONKtorReturnTest.kt
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

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

import net.pwall.json.parseJSON

class JSONKtorReturnTest {

    /**
     * This test confirms that a simple JSON object is serialized and returned correctly.
     */
    @Test
    fun `should return JSON object`() {
        withTestApplication(Application::testReturnModule) {
            expect(ReturnData("FGH", 9000)) {
                handleRequest(HttpMethod.Get, "/q").response.content?.parseJSON<ReturnData>()
            }
        }
    }

}

/**
 * Application module to test JSON returned data functionality.  Defines a single endpoint that responds with a simple
 * JSON object.
 */
fun Application.testReturnModule() {
    install(ContentNegotiation) {
        jsonKtor {}
    }
    routing {
        get("/q") {
            call.respond(ReturnData("FGH", 9000))
        }
    }
}

data class ReturnData(val field1: String, val field2: Int)
