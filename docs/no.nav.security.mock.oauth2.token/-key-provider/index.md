---
title: KeyProvider
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.token](../index.html)/[KeyProvider](index.html)



# KeyProvider



[jvm]\
open class [KeyProvider](index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(initialKeys: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;JWK&gt; = keysFromFile(INITIAL_KEYS_FILE), algorithm: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = JWSAlgorithm.RS256.name)



## Constructors


| | |
|---|---|
| [KeyProvider](-key-provider.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [KeyProvider](-key-provider.html)(initialKeys: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;JWK&gt; = keysFromFile(INITIAL_KEYS_FILE), algorithm: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = JWSAlgorithm.RS256.name) |


## Types


| Name | Summary |
|---|---|
| [Companion](-companion/index.html) | [jvm]<br>object [Companion](-companion/index.html) |


## Functions


| Name | Summary |
|---|---|
| [algorithm](algorithm.html) | [jvm]<br>fun [algorithm](algorithm.html)(): JWSAlgorithm |
| [generate](generate.html) | [jvm]<br>fun [generate](generate.html)(algorithm: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [keyType](key-type.html) | [jvm]<br>fun [keyType](key-type.html)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [signingKey](signing-key.html) | [jvm]<br>fun [signingKey](signing-key.html)(keyId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): JWK |

