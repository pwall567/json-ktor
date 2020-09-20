# Change Log

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [1.2] - 2020-09-20
### Changed
- `pom.xml`: Updated Kotlin and ktor versions to 1.4.0 (both)

## [1.1] - 2020-05-03
### Added
- `JSONKtorFunctions`: Functions extracted from `JSONKtor`
### Changed
- `JSONKtor`: Added streamed output

## [1.0] - 2020-04-22
### Changed
- Changes to library versions and package names for ktor 1.3.0
- `JSONKtor`: Added ability to parameterise target Content-Type

## [0.10] - 2020-04-21
### Changed
- `JSONKtor`: Added capability to deserialize into a Kotlin `Flow` or `Channel`

## [0.9] - 2020-02-02
### Changed
- `JSONKtor`: Switched read to use ByteArray instead of ByteBuffer
- `JSONKtor`: Updated for changes to `json-stream` library

## [0.8] - 2020-01-28
### Changed
- Bumped versions of dependencies

## [0.7] - 2020-01-15
### Changed
- `JSONKtor`: Removed premature optimisation
- Updated test classes
- Bumped versions of dependencies

## [0.6] - 2020-01-14
### Changed
- `JSONKtor`: Updated to use `json-stream` library
- Added more test classes

## [0.5] - 2019-11-17
### Changed
- `JSONKtor`: Change to `typeInfo` usage

## [0.4] - 2019-10-08
### Changed
- `JSONKtor`: Parameterize `charset`
- `pom.xml`: Updated dependencies

## [0.3] - 2019-09-12
### Changed
- `pom.xml`: Changed to use parent POM
- `pom.xml`: upgraded to version 0.10 of `json-kotlin`

## [0.2] - 2019-07-08
### Changed
- `JSONKtor`: Improved reading of JSON to be coroutine-safe

## [0.1] - 2019-06-02
### Added
- `JSONKtor`: JSON content converter for ktor
