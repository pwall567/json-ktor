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

Later versions will allow JSON array content to be streamed to a Kotlin co-routine `Channel` for asynchronous
processing.
Watch this space.

## Dependency Specification

The latest version of the library is 0.8, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>net.pwall.json</groupId>
      <artifactId>json-ktor</artifactId>
      <version>0.8</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'net.pwall.json:json-ktor:0.8'
```
### Gradle (kts)
```kotlin
    implementation("net.pwall.json:json-ktor:0.8")
```

Peter Wall

2020-01-28
