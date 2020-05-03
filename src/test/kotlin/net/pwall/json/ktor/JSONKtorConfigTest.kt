/*
 * @(#) JSONKtorConfigTest.kt
 *
 * json-ktor JSON functionality for ktor
 * Copyright (c) 2020 Peter Wall
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
import kotlin.test.fail

import io.ktor.application.Application
import io.ktor.application.feature
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.server.testing.withTestApplication

import net.pwall.json.ktor.JSONKtorFunctions.jsonKtor

class JSONKtorConfigTest {

    @Test fun `should configure JSONConfig when adding feature`() {
        withTestApplication(Application::testConfig) {}
    }

}

fun Application.testConfig() {
    val specialContentType = ContentType("application", "x-json-special")
    install(ContentNegotiation) {
        jsonKtor(specialContentType) {
            readBufferSize = 1280
            bigIntegerString = true
        }
    }
    val converter = findJSONKtor() ?: fail("Can't locate JSONKtor")
    expect(specialContentType) { converter.contentType }
    val config = converter.config
    expect(1280) { config.readBufferSize }
    expect(true) { config.bigIntegerString }
}

private fun Application.findJSONKtor(): JSONKtor? {
    feature(ContentNegotiation).registrations.forEach { registration ->
        registration.converter.let { if (it is JSONKtor) return it }
    }
    return null
}
