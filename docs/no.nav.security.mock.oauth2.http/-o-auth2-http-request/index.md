---
title: OAuth2HttpRequest
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.http](../index.html)/[OAuth2HttpRequest](index.html)



# OAuth2HttpRequest



[jvm]\
data class [OAuth2HttpRequest](index.html)(val headers: Headers, val method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val originalUrl: HttpUrl, val body: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null)



## Constructors


| | |
|---|---|
| [OAuth2HttpRequest](-o-auth2-http-request.html) | [jvm]<br>fun [OAuth2HttpRequest](-o-auth2-http-request.html)(headers: Headers, method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), originalUrl: HttpUrl, body: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null) |


## Types


| Name | Summary |
|---|---|
| [Parameters](-parameters/index.html) | [jvm]<br>data class [Parameters](-parameters/index.html)(val parameterString: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?) |


## Functions


| Name | Summary |
|---|---|
| [asAuthenticationRequest](as-authentication-request.html) | [jvm]<br>fun [asAuthenticationRequest](as-authentication-request.html)(): AuthenticationRequest |
| [asNimbusHTTPRequest](as-nimbus-h-t-t-p-request.html) | [jvm]<br>fun [asNimbusHTTPRequest](as-nimbus-h-t-t-p-request.html)(): HTTPRequest |
| [asNimbusTokenRequest](as-nimbus-token-request.html) | [jvm]<br>fun [asNimbusTokenRequest](as-nimbus-token-request.html)(): TokenRequest |
| [asTokenExchangeRequest](as-token-exchange-request.html) | [jvm]<br>fun [asTokenExchangeRequest](as-token-exchange-request.html)(): TokenRequest |
| [grantType](grant-type.html) | [jvm]<br>fun [grantType](grant-type.html)(): GrantType |
| [toWellKnown](to-well-known.html) | [jvm]<br>fun [toWellKnown](to-well-known.html)(): [WellKnown](../-well-known/index.html) |
| [type](type.html) | [jvm]<br>fun [type](type.html)(): [RequestType](../-request-type/index.html) |


## Properties


| Name | Summary |
|---|---|
| [body](body.html) | [jvm]<br>val [body](body.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [cookies](cookies.html) | [jvm]<br>val [cookies](cookies.html): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [formParameters](form-parameters.html) | [jvm]<br>val [formParameters](form-parameters.html): [OAuth2HttpRequest.Parameters](-parameters/index.html) |
| [headers](headers.html) | [jvm]<br>val [headers](headers.html): Headers |
| [method](method.html) | [jvm]<br>val [method](method.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [originalUrl](original-url.html) | [jvm]<br>val [originalUrl](original-url.html): HttpUrl |
| [url](url.html) | [jvm]<br>val [url](url.html): HttpUrl |

