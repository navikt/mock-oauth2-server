---
title: no.nav.security.mock.oauth2
---
//[mock-oauth2-server](../../index.html)/[no.nav.security.mock.oauth2](index.html)



# Package no.nav.security.mock.oauth2



## Types


| Name | Summary |
|---|---|
| [MockOAuth2Server](-mock-o-auth2-server/index.html) | [jvm]<br>open class [MockOAuth2Server](-mock-o-auth2-server/index.html)(val config: [OAuth2Config](-o-auth2-config/index.html) = OAuth2Config(), additionalRoutes: [Route](../no.nav.security.mock.oauth2.http/-route/index.html)) |
| [OAuth2Config](-o-auth2-config/index.html) | [jvm]<br>data class [OAuth2Config](-o-auth2-config/index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(val interactiveLogin: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, val loginPagePath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, val tokenProvider: [OAuth2TokenProvider](../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html) = OAuth2TokenProvider(), val tokenCallbacks: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[OAuth2TokenCallback](../no.nav.security.mock.oauth2.token/-o-auth2-token-callback/index.html)&gt; = emptySet(), val httpServer: [OAuth2HttpServer](../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html) = MockWebServerWrapper()) |
| [OAuth2Exception](-o-auth2-exception/index.html) | [jvm]<br>class [OAuth2Exception](-o-auth2-exception/index.html)(val errorObject: ErrorObject?, msg: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)?) : [RuntimeException](https://docs.oracle.com/javase/8/docs/api/java/lang/RuntimeException.html) |
| [StandaloneConfig](-standalone-config/index.html) | [jvm]<br>object [StandaloneConfig](-standalone-config/index.html) |


## Functions


| Name | Summary |
|---|---|
| [fromEnv](from-env.html) | [jvm]<br>fun [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html).[fromEnv](from-env.html)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>fun [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html).[fromEnv](from-env.html)(default: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [invalidGrant](invalid-grant.html) | [jvm]<br>fun [invalidGrant](invalid-grant.html)(grantType: GrantType): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html) |
| [invalidRequest](invalid-request.html) | [jvm]<br>fun [invalidRequest](invalid-request.html)(message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html) |
| [main](main.html) | [jvm]<br>fun [main](main.html)() |
| [missingParameter](missing-parameter.html) | [jvm]<br>fun [missingParameter](missing-parameter.html)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html) |
| [notFound](not-found.html) | [jvm]<br>fun [notFound](not-found.html)(message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html) |
| [withMockOAuth2Server](with-mock-o-auth2-server.html) | [jvm]<br>fun &lt;[R](with-mock-o-auth2-server.html)&gt; [withMockOAuth2Server](with-mock-o-auth2-server.html)(test: [MockOAuth2Server](-mock-o-auth2-server/index.html).() -&gt; [R](with-mock-o-auth2-server.html)): [R](with-mock-o-auth2-server.html) |

