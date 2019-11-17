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

## Dependency Specification

The latest version of the library is 2.0, and it may be found the the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>net.pwall.json</groupId>
      <artifactId>json-ktor</artifactId>
      <version>0.5</version>
    </dependency>
```
### Gradle
```groovy
    implementation "net.pwall.json:json-ktor:0.5"
```
### Gradle (kts)
```kotlin
    implementation("net.pwall.json:json-ktor:0.5")
```

Peter Wall

2019-11-17
