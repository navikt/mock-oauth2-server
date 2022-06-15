---
title: no.nav.security.mock.oauth2.http
---
//[mock-oauth2-server](../../index.html)/[no.nav.security.mock.oauth2.http](index.html)



# Package no.nav.security.mock.oauth2.http



## Types


| Name | Summary |
|---|---|
| [CorsInterceptor](-cors-interceptor/index.html) | [jvm]<br>class [CorsInterceptor](-cors-interceptor/index.html)(allowedMethods: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf(&quot;POST&quot;, &quot;GET&quot;, &quot;OPTIONS&quot;)) : [ResponseInterceptor](-response-interceptor/index.html) |
| [Interceptor](-interceptor/index.html) | [jvm]<br>interface [Interceptor](-interceptor/index.html) |
| [MockWebServerWrapper](-mock-web-server-wrapper/index.html) | [jvm]<br>class [MockWebServerWrapper](-mock-web-server-wrapper/index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(val ssl: [Ssl](-ssl/index.html)? = null) : [OAuth2HttpServer](-o-auth2-http-server/index.html) |
| [NettyWrapper](-netty-wrapper/index.html) | [jvm]<br>class [NettyWrapper](-netty-wrapper/index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(val ssl: [Ssl](-ssl/index.html)? = null) : [OAuth2HttpServer](-o-auth2-http-server/index.html) |
| [OAuth2HttpRequest](-o-auth2-http-request/index.html) | [jvm]<br>data class [OAuth2HttpRequest](-o-auth2-http-request/index.html)(val headers: Headers, val method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val originalUrl: HttpUrl, val body: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null) |
| [OAuth2HttpRequestHandler](-o-auth2-http-request-handler/index.html) | [jvm]<br>class [OAuth2HttpRequestHandler](-o-auth2-http-request-handler/index.html)(config: [OAuth2Config](../no.nav.security.mock.oauth2/-o-auth2-config/index.html)) |
| [OAuth2HttpResponse](-o-auth2-http-response/index.html) | [jvm]<br>data class [OAuth2HttpResponse](-o-auth2-http-response/index.html)(val headers: Headers = Headers.headersOf(), val status: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), val body: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null) |
| [OAuth2HttpServer](-o-auth2-http-server/index.html) | [jvm]<br>interface [OAuth2HttpServer](-o-auth2-http-server/index.html) : [AutoCloseable](https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html) |
| [OAuth2TokenResponse](-o-auth2-token-response/index.html) | [jvm]<br>data class [OAuth2TokenResponse](-o-auth2-token-response/index.html)(val tokenType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val issuedTokenType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, val idToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, val accessToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, val refreshToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, val expiresIn: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 0, val scope: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null) |
| [RequestHandler](index.html#111237332%2FClasslikes%2F863300109) | [jvm]<br>typealias [RequestHandler](index.html#111237332%2FClasslikes%2F863300109) = ([OAuth2HttpRequest](-o-auth2-http-request/index.html)) -&gt; [OAuth2HttpResponse](-o-auth2-http-response/index.html) |
| [RequestInterceptor](-request-interceptor/index.html) | [jvm]<br>fun interface [RequestInterceptor](-request-interceptor/index.html) : [Interceptor](-interceptor/index.html) |
| [RequestType](-request-type/index.html) | [jvm]<br>enum [RequestType](-request-type/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[RequestType](-request-type/index.html)&gt; |
| [ResponseInterceptor](-response-interceptor/index.html) | [jvm]<br>fun interface [ResponseInterceptor](-response-interceptor/index.html) : [Interceptor](-interceptor/index.html) |
| [Route](-route/index.html) | [jvm]<br>interface [Route](-route/index.html) : [Function1](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-function1/index.html)&lt;[OAuth2HttpRequest](-o-auth2-http-request/index.html), [OAuth2HttpResponse](-o-auth2-http-response/index.html)&gt; |
| [Ssl](-ssl/index.html) | [jvm]<br>class [Ssl](-ssl/index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(val sslKeystore: [SslKeystore](-ssl-keystore/index.html) = SslKeystore()) |
| [SslKeystore](-ssl-keystore/index.html) | [jvm]<br>class [SslKeystore](-ssl-keystore/index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(val keyPassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;&quot;, val keyStore: [KeyStore](https://docs.oracle.com/javase/8/docs/api/java/security/KeyStore.html) = generate(&quot;localhost&quot;, keyPassword)) |
| [WellKnown](-well-known/index.html) | [jvm]<br>data class [WellKnown](-well-known/index.html)(val issuer: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val authorizationEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val endSessionEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val tokenEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val userInfoEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val jwksUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val introspectionEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val responseTypesSupported: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf(&quot;query&quot;, &quot;fragment&quot;, &quot;form_post&quot;), val subjectTypesSupported: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf(&quot;public&quot;), val idTokenSigningAlgValuesSupported: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = (KeyGenerator.ecAlgorithmFamily + KeyGenerator.rsaAlgorithmFamily).map { it.name }.toList()) |


## Functions


| Name | Summary |
|---|---|
| [authenticationSuccess](authentication-success.html) | [jvm]<br>fun [authenticationSuccess](authentication-success.html)(authenticationSuccessResponse: AuthenticationSuccessResponse): [OAuth2HttpResponse](-o-auth2-http-response/index.html) |
| [get](get.html) | [jvm]<br>fun [get](get.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestHandler: [RequestHandler](index.html#111237332%2FClasslikes%2F863300109)): [Route](-route/index.html) |
| [html](html.html) | [jvm]<br>fun [html](html.html)(content: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [OAuth2HttpResponse](-o-auth2-http-response/index.html) |
| [json](json.html) | [jvm]<br>fun [json](json.html)(anyObject: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)): [OAuth2HttpResponse](-o-auth2-http-response/index.html) |
| [methodNotAllowed](method-not-allowed.html) | [jvm]<br>fun [methodNotAllowed](method-not-allowed.html)(): [OAuth2HttpResponse](-o-auth2-http-response/index.html) |
| [notFound](not-found.html) | [jvm]<br>fun [notFound](not-found.html)(body: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null): [OAuth2HttpResponse](-o-auth2-http-response/index.html) |
| [oauth2Error](oauth2-error.html) | [jvm]<br>fun [oauth2Error](oauth2-error.html)(error: ErrorObject): [OAuth2HttpResponse](-o-auth2-http-response/index.html) |
| [options](options.html) | [jvm]<br>fun [options](options.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestHandler: [RequestHandler](index.html#111237332%2FClasslikes%2F863300109)): [Route](-route/index.html) |
| [post](post.html) | [jvm]<br>fun [post](post.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestHandler: [RequestHandler](index.html#111237332%2FClasslikes%2F863300109)): [Route](-route/index.html) |
| [put](put.html) | [jvm]<br>fun [put](put.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestHandler: [RequestHandler](index.html#111237332%2FClasslikes%2F863300109)): [Route](-route/index.html) |
| [redirect](redirect.html) | [jvm]<br>fun [redirect](redirect.html)(location: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), headers: Headers = Headers.headersOf()): [OAuth2HttpResponse](-o-auth2-http-response/index.html) |
| [route](route.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [route](route.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, requestHandler: [RequestHandler](index.html#111237332%2FClasslikes%2F863300109)): [Route](-route/index.html) |
| [routes](routes.html) | [jvm]<br>fun [routes](routes.html)(vararg route: [Route](-route/index.html)): [Route](-route/index.html)<br>fun [routes](routes.html)(config: [Route.Builder](-route/-builder/index.html).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [Route](-route/index.html) |


## Properties


| Name | Summary |
|---|---|
| [objectMapper](object-mapper.html) | [jvm]<br>val [objectMapper](object-mapper.html): ObjectMapper |
| [templateMapper](template-mapper.html) | [jvm]<br>val [templateMapper](template-mapper.html): [TemplateMapper](../no.nav.security.mock.oauth2.templates/-template-mapper/index.html) |

