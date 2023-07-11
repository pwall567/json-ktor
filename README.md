# json-ktor

[![Build Status](https://travis-ci.com/pwall567/json-ktor.svg?branch=master)](https://travis-ci.com/github/pwall567/json-ktor)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.7.21&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v1.7.21)
[![Maven Central](https://img.shields.io/maven-central/v/net.pwall.json/json-ktor?label=Maven%20Central)](https://search.maven.org/search?q=g:%22net.pwall.json%22%20AND%20a:%22json-ktor%22)

JSON functionality for ktor

This library provides ktor integration for the [`json-kotlin`](https://github.com/pwall567/json-kotlin) library.

## Quick Start

In the `Application` module function, use:
```kotlin
    install(ContentNegotiation) {
        jsonKtor {}
    }
```

Customizations (e.g custom serialization or deserialization) may be specified within the lambda supplied to the
`jsonKtor` function:
```kotlin
    install(ContentNegotiation) {
        jsonKtor {
            fromJSON { json ->
                require(json is JSONObject) { "Must be JSONObject" }
                Example(json.getString("custom1"), json.getInt("custom2"))
            }
        }
    }
```
For more details see the `json-kotlin` library.

## Streaming Input

From version 0.6 onwards, this library uses the `json-streaming` library for on-the-fly JSON parsing.
This means that the input data is parsed into an internal form as it is being read, and avoids the need to allocate
memory for the entire JSON text.

From version 0.10, the library will stream asynchronously to a Kotlin `Flow` or `ReceiveChannel`, using the Kotlin
coroutine functionality.
If the data stream consists of a data array (a sequence of objects in square brackets), it may be deserialized as
follows:
```kotlin
    val flowInput = call.receive<Flow<BusinessObject>>()
    flowInput.collect { businessObject ->
        // process businessObject
    }
```
Or (slightly more complicated):
```kotlin
    val channel = call.receive<ReceiveChannel<BusinessObject>>()
    while (!channel.isClosedForReceive) {
        val businessObject = channel.receive()
        // process businessObject
    }
```

## Streaming Output

From version 1.1, the library will (optionally) stream output using a non-blocking output library
([`json-kotlin-nonblocking`](https://github.com/pwall567/json-kotlin-nonblocking)).
Using this option, ktor response data will be sent as soon as it is available - this is particularly useful when data is
produced asynchronously:
```kotlin
    val result = flow<BusinessObject> {
        val businessObject = businessFunction()
        emit(businessObject)
    }
    call.respond(result)
```
Or:
```kotlin
    val result = scope.produce<BusinessObject> {
        val businessObject = businessFunction()
        send(businessObject)
    }
    call.respond(result)
```
Because there is a performance penalty for using streamed output, this is an option that must be enabled in the
`JSONConfig`:
```kotlin
    install(ContentNegotiation) {
        jsonKtor {
            streamOutput = true
        }
    }
```

## ktor Version

Version 1.0 (and later) of this library uses ktor version 1.3.0.
This release of ktor introduced a number of breaking changes, and earlier versions of ktor will not work with
`json-ktor` 1.0 and above.

Anyone requiring ktor 1.2.4 support will need to use `json-ktor` version 0.10.

Version 1.2 and later of this library uses ktor version 1.4.0.

## Dependency Specification

The latest version of the library is 1.4, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>net.pwall.json</groupId>
      <artifactId>json-ktor</artifactId>
      <version>1.4</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'net.pwall.json:json-ktor:1.4'
```
### Gradle (kts)
```kotlin
    implementation("net.pwall.json:json-ktor:1.4")
```

Peter Wall

2023-07-12
