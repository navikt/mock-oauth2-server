---
title: OAuth2TokenProvider
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.token](../index.html)/[OAuth2TokenProvider](index.html)



# OAuth2TokenProvider



[jvm]\
class [OAuth2TokenProvider](index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(keyProvider: [KeyProvider](../-key-provider/index.html) = KeyProvider())



## Constructors


| | |
|---|---|
| [OAuth2TokenProvider](-o-auth2-token-provider.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [OAuth2TokenProvider](-o-auth2-token-provider.html)(keyProvider: [KeyProvider](../-key-provider/index.html) = KeyProvider()) |


## Functions


| Name | Summary |
|---|---|
| [accessToken](access-token.html) | [jvm]<br>fun [accessToken](access-token.html)(tokenRequest: TokenRequest, issuerUrl: HttpUrl, oAuth2TokenCallback: [OAuth2TokenCallback](../-o-auth2-token-callback/index.html), nonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null): SignedJWT |
| [exchangeAccessToken](exchange-access-token.html) | [jvm]<br>fun [exchangeAccessToken](exchange-access-token.html)(tokenRequest: TokenRequest, issuerUrl: HttpUrl, claimsSet: JWTClaimsSet, oAuth2TokenCallback: [OAuth2TokenCallback](../-o-auth2-token-callback/index.html)): SignedJWT |
| [idToken](id-token.html) | [jvm]<br>fun [idToken](id-token.html)(tokenRequest: TokenRequest, issuerUrl: HttpUrl, oAuth2TokenCallback: [OAuth2TokenCallback](../-o-auth2-token-callback/index.html), nonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null): SignedJWT |
| [jwt](jwt.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [jwt](jwt.html)(claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, expiry: [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html) = Duration.ofHours(1), issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;default&quot;): SignedJWT |
| [publicJwkSet](public-jwk-set.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [publicJwkSet](public-jwk-set.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;default&quot;): JWKSet |

