---
title: RequestMappingTokenCallback
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.token](../index.html)/[RequestMappingTokenCallback](index.html)



# RequestMappingTokenCallback



[jvm]\
data class [RequestMappingTokenCallback](index.html)(val issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val requestMappings: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[RequestMapping](../-request-mapping/index.html)&gt;, val tokenExpiry: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) = Duration.ofHours(1).toSeconds()) : [OAuth2TokenCallback](../-o-auth2-token-callback/index.html)



## Constructors


| | |
|---|---|
| [RequestMappingTokenCallback](-request-mapping-token-callback.html) | [jvm]<br>fun [RequestMappingTokenCallback](-request-mapping-token-callback.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestMappings: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[RequestMapping](../-request-mapping/index.html)&gt;, tokenExpiry: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) = Duration.ofHours(1).toSeconds()) |


## Functions


| Name | Summary |
|---|---|
| [addClaims](add-claims.html) | [jvm]<br>open override fun [addClaims](add-claims.html)(tokenRequest: TokenRequest): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; |
| [audience](audience.html) | [jvm]<br>open override fun [audience](audience.html)(tokenRequest: TokenRequest): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [issuerId](issuer-id.html) | [jvm]<br>open override fun [issuerId](issuer-id.html)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [subject](subject.html) | [jvm]<br>open override fun [subject](subject.html)(tokenRequest: TokenRequest): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [tokenExpiry](token-expiry.html) | [jvm]<br>open override fun [tokenExpiry](token-expiry.html)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |
| [typeHeader](type-header.html) | [jvm]<br>open override fun [typeHeader](type-header.html)(tokenRequest: TokenRequest): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |


## Properties


| Name | Summary |
|---|---|
| [issuerId](issuer-id.html) | [jvm]<br>val [issuerId](issuer-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [requestMappings](request-mappings.html) | [jvm]<br>val [requestMappings](request-mappings.html): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[RequestMapping](../-request-mapping/index.html)&gt; |
| [tokenExpiry](token-expiry.html) | [jvm]<br>val [tokenExpiry](token-expiry.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |

