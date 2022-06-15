---
title: KeyGenerator
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.token](../index.html)/[KeyGenerator](index.html)



# KeyGenerator



[jvm]\
data class [KeyGenerator](index.html)(val algorithm: JWSAlgorithm = JWSAlgorithm.RS256, var keyGenerator: [KeyPairGenerator](https://docs.oracle.com/javase/8/docs/api/java/security/KeyPairGenerator.html) = generate(algorithm.name))



## Constructors


| | |
|---|---|
| [KeyGenerator](-key-generator.html) | [jvm]<br>fun [KeyGenerator](-key-generator.html)(algorithm: JWSAlgorithm = JWSAlgorithm.RS256, keyGenerator: [KeyPairGenerator](https://docs.oracle.com/javase/8/docs/api/java/security/KeyPairGenerator.html) = generate(algorithm.name)) |


## Types


| Name | Summary |
|---|---|
| [Companion](-companion/index.html) | [jvm]<br>object [Companion](-companion/index.html) |


## Functions


| Name | Summary |
|---|---|
| [generateKey](generate-key.html) | [jvm]<br>fun [generateKey](generate-key.html)(keyId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): JWK |


## Properties


| Name | Summary |
|---|---|
| [algorithm](algorithm.html) | [jvm]<br>val [algorithm](algorithm.html): JWSAlgorithm |
| [keyGenerator](key-generator.html) | [jvm]<br>var [keyGenerator](key-generator.html): [KeyPairGenerator](https://docs.oracle.com/javase/8/docs/api/java/security/KeyPairGenerator.html) |

