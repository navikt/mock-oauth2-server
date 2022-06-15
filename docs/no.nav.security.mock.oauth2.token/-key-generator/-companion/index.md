---
title: Companion
---
//[mock-oauth2-server](../../../../index.html)/[no.nav.security.mock.oauth2.token](../../index.html)/[KeyGenerator](../index.html)/[Companion](index.html)



# Companion



[jvm]\
object [Companion](index.html)



## Types


| Name | Summary |
|---|---|
| [Algorithm](-algorithm/index.html) | [jvm]<br>data class [Algorithm](-algorithm/index.html)(val family: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;JWSAlgorithm&gt;, val keyType: KeyType) |


## Functions


| Name | Summary |
|---|---|
| [generate](generate.html) | [jvm]<br>fun [generate](generate.html)(algorithm: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [KeyPairGenerator](https://docs.oracle.com/javase/8/docs/api/java/security/KeyPairGenerator.html) |
| [isSupported](is-supported.html) | [jvm]<br>fun [isSupported](is-supported.html)(algorithm: JWSAlgorithm): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |


## Properties


| Name | Summary |
|---|---|
| [ecAlgorithmFamily](ec-algorithm-family.html) | [jvm]<br>val [ecAlgorithmFamily](ec-algorithm-family.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;JWSAlgorithm&gt; |
| [rsaAlgorithmFamily](rsa-algorithm-family.html) | [jvm]<br>val [rsaAlgorithmFamily](rsa-algorithm-family.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;JWSAlgorithm&gt; |

