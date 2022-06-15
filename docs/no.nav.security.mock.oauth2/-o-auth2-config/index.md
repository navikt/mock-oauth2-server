---
title: OAuth2Config
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2](../index.html)/[OAuth2Config](index.html)



# OAuth2Config



[jvm]\
data class [OAuth2Config](index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(val interactiveLogin: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, val loginPagePath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, val tokenProvider: [OAuth2TokenProvider](../../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html) = OAuth2TokenProvider(), val tokenCallbacks: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[OAuth2TokenCallback](../../no.nav.security.mock.oauth2.token/-o-auth2-token-callback/index.html)&gt; = emptySet(), val httpServer: [OAuth2HttpServer](../../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html) = MockWebServerWrapper())



## Constructors


| | |
|---|---|
| [OAuth2Config](-o-auth2-config.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [OAuth2Config](-o-auth2-config.html)(interactiveLogin: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, loginPagePath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, tokenProvider: [OAuth2TokenProvider](../../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html) = OAuth2TokenProvider(), tokenCallbacks: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[OAuth2TokenCallback](../../no.nav.security.mock.oauth2.token/-o-auth2-token-callback/index.html)&gt; = emptySet(), httpServer: [OAuth2HttpServer](../../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html) = MockWebServerWrapper()) |


## Types


| Name | Summary |
|---|---|
| [Companion](-companion/index.html) | [jvm]<br>object [Companion](-companion/index.html) |
| [OAuth2HttpServerDeserializer](-o-auth2-http-server-deserializer/index.html) | [jvm]<br>class [OAuth2HttpServerDeserializer](-o-auth2-http-server-deserializer/index.html) : JsonDeserializer&lt;[OAuth2HttpServer](../../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html)&gt; |
| [OAuth2TokenProviderDeserializer](-o-auth2-token-provider-deserializer/index.html) | [jvm]<br>class [OAuth2TokenProviderDeserializer](-o-auth2-token-provider-deserializer/index.html) : JsonDeserializer&lt;[OAuth2TokenProvider](../../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html)&gt; |


## Properties


| Name | Summary |
|---|---|
| [httpServer](http-server.html) | [jvm]<br>val [httpServer](http-server.html): [OAuth2HttpServer](../../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html) |
| [interactiveLogin](interactive-login.html) | [jvm]<br>val [interactiveLogin](interactive-login.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [loginPagePath](login-page-path.html) | [jvm]<br>val [loginPagePath](login-page-path.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [tokenCallbacks](token-callbacks.html) | [jvm]<br>val [tokenCallbacks](token-callbacks.html): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[OAuth2TokenCallback](../../no.nav.security.mock.oauth2.token/-o-auth2-token-callback/index.html)&gt; |
| [tokenProvider](token-provider.html) | [jvm]<br>val [tokenProvider](token-provider.html): [OAuth2TokenProvider](../../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html) |

