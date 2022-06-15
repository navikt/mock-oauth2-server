---
title: no.nav.security.mock.oauth2.token
---
//[mock-oauth2-server](../../index.html)/[no.nav.security.mock.oauth2.token](index.html)



# Package no.nav.security.mock.oauth2.token



## Types


| Name | Summary |
|---|---|
| [DefaultOAuth2TokenCallback](-default-o-auth2-token-callback/index.html) | [jvm]<br>open class [DefaultOAuth2TokenCallback](-default-o-auth2-token-callback/index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;default&quot;, subject: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = UUID.randomUUID().toString(), typeHeader: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = JOSEObjectType.JWT.type, audience: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null, claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; = emptyMap(), expiry: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) = 3600) : [OAuth2TokenCallback](-o-auth2-token-callback/index.html) |
| [KeyGenerator](-key-generator/index.html) | [jvm]<br>data class [KeyGenerator](-key-generator/index.html)(val algorithm: JWSAlgorithm = JWSAlgorithm.RS256, var keyGenerator: [KeyPairGenerator](https://docs.oracle.com/javase/8/docs/api/java/security/KeyPairGenerator.html) = generate(algorithm.name)) |
| [KeyProvider](-key-provider/index.html) | [jvm]<br>open class [KeyProvider](-key-provider/index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(initialKeys: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;JWK&gt; = keysFromFile(INITIAL_KEYS_FILE), algorithm: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = JWSAlgorithm.RS256.name) |
| [OAuth2TokenCallback](-o-auth2-token-callback/index.html) | [jvm]<br>interface [OAuth2TokenCallback](-o-auth2-token-callback/index.html) |
| [OAuth2TokenProvider](-o-auth2-token-provider/index.html) | [jvm]<br>class [OAuth2TokenProvider](-o-auth2-token-provider/index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(keyProvider: [KeyProvider](-key-provider/index.html) = KeyProvider()) |
| [RequestMapping](-request-mapping/index.html) | [jvm]<br>data class [RequestMapping](-request-mapping/index.html)(requestParam: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), match: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;*&quot;, val claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; = emptyMap(), val typeHeader: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = JOSEObjectType.JWT.type) |
| [RequestMappingTokenCallback](-request-mapping-token-callback/index.html) | [jvm]<br>data class [RequestMappingTokenCallback](-request-mapping-token-callback/index.html)(val issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val requestMappings: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[RequestMapping](-request-mapping/index.html)&gt;, val tokenExpiry: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) = Duration.ofHours(1).toSeconds()) : [OAuth2TokenCallback](-o-auth2-token-callback/index.html) |

