---
title: OAuth2TokenCallback
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.token](../index.html)/[OAuth2TokenCallback](index.html)



# OAuth2TokenCallback



[jvm]\
interface [OAuth2TokenCallback](index.html)



## Functions


| Name | Summary |
|---|---|
| [addClaims](add-claims.html) | [jvm]<br>abstract fun [addClaims](add-claims.html)(tokenRequest: TokenRequest): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; |
| [audience](audience.html) | [jvm]<br>abstract fun [audience](audience.html)(tokenRequest: TokenRequest): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [issuerId](issuer-id.html) | [jvm]<br>abstract fun [issuerId](issuer-id.html)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [subject](subject.html) | [jvm]<br>abstract fun [subject](subject.html)(tokenRequest: TokenRequest): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [tokenExpiry](token-expiry.html) | [jvm]<br>abstract fun [tokenExpiry](token-expiry.html)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |
| [typeHeader](type-header.html) | [jvm]<br>abstract fun [typeHeader](type-header.html)(tokenRequest: TokenRequest): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |


## Inheritors


| Name |
|---|
| [DefaultOAuth2TokenCallback](../-default-o-auth2-token-callback/index.html) |
| [RequestMappingTokenCallback](../-request-mapping-token-callback/index.html) |

