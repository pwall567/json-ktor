# json-ktor

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

## ktor Version

Versions of this library up to (and including) 0.10 use ktor version 1.2.4.

ktor version 1.3.0 introduced some breaking changes, and future versions of this library will use ktor 1.3.0.
It is important to use the correct version to match the version of ktor.

## Dependency Specification

The latest version of the library is 0.10, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>net.pwall.json</groupId>
      <artifactId>json-ktor</artifactId>
      <version>0.10</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'net.pwall.json:json-ktor:0.10'
```
### Gradle (kts)
```kotlin
    implementation("net.pwall.json:json-ktor:0.10")
```

Peter Wall

2020-04-21
