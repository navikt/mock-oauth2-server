---
title: DefaultOAuth2TokenCallback
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.token](../index.html)/[DefaultOAuth2TokenCallback](index.html)



# DefaultOAuth2TokenCallback



[jvm]\
open class [DefaultOAuth2TokenCallback](index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;default&quot;, subject: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = UUID.randomUUID().toString(), typeHeader: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = JOSEObjectType.JWT.type, audience: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null, claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; = emptyMap(), expiry: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) = 3600) : [OAuth2TokenCallback](../-o-auth2-token-callback/index.html)



## Constructors


| | |
|---|---|
| [DefaultOAuth2TokenCallback](-default-o-auth2-token-callback.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [DefaultOAuth2TokenCallback](-default-o-auth2-token-callback.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;default&quot;, subject: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = UUID.randomUUID().toString(), typeHeader: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = JOSEObjectType.JWT.type, audience: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null, claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; = emptyMap(), expiry: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) = 3600) |


## Functions


| Name | Summary |
|---|---|
| [addClaims](add-claims.html) | [jvm]<br>open override fun [addClaims](add-claims.html)(tokenRequest: TokenRequest): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; |
| [audience](audience.html) | [jvm]<br>open override fun [audience](audience.html)(tokenRequest: TokenRequest): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [issuerId](issuer-id.html) | [jvm]<br>open override fun [issuerId](issuer-id.html)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [subject](subject.html) | [jvm]<br>open override fun [subject](subject.html)(tokenRequest: TokenRequest): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [tokenExpiry](token-expiry.html) | [jvm]<br>open override fun [tokenExpiry](token-expiry.html)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |
| [typeHeader](type-header.html) | [jvm]<br>open override fun [typeHeader](type-header.html)(tokenRequest: TokenRequest): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

