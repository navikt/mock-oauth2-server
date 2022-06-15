---
title: MockOAuth2Server
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2](../index.html)/[MockOAuth2Server](index.html)



# MockOAuth2Server



[jvm]\
open class [MockOAuth2Server](index.html)(val config: [OAuth2Config](../-o-auth2-config/index.html) = OAuth2Config(), additionalRoutes: [Route](../../no.nav.security.mock.oauth2.http/-route/index.html))



## Constructors


| | |
|---|---|
| [MockOAuth2Server](-mock-o-auth2-server.html) | [jvm]<br>fun [MockOAuth2Server](-mock-o-auth2-server.html)(vararg additionalRoutes: [Route](../../no.nav.security.mock.oauth2.http/-route/index.html)) |
| [MockOAuth2Server](-mock-o-auth2-server.html) | [jvm]<br>fun [MockOAuth2Server](-mock-o-auth2-server.html)(config: [OAuth2Config](../-o-auth2-config/index.html) = OAuth2Config(), vararg additionalRoutes: [Route](../../no.nav.security.mock.oauth2.http/-route/index.html)) |


## Types


| Name | Summary |
|---|---|
| [Companion](-companion/index.html) | [jvm]<br>object [Companion](-companion/index.html) |


## Functions


| Name | Summary |
|---|---|
| [anyToken](any-token.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [anyToken](any-token.html)(issuerUrl: HttpUrl, claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, expiry: [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html) = Duration.ofHours(1)): SignedJWT<br>Issues a signed JWT for a given [issuerUrl](any-token.html) containing the input set of [claims](any-token.html). The JWT's signature can be verified with the server's keys found at the [jwksUrl](jwks-url.html) endpoint. |
| [authorizationEndpointUrl](authorization-endpoint-url.html) | [jvm]<br>fun [authorizationEndpointUrl](authorization-endpoint-url.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl<br>Returns the authorization server's authorization_endpoint for the given [issuerId](authorization-endpoint-url.html). |
| [baseUrl](base-url.html) | [jvm]<br>fun [baseUrl](base-url.html)(): HttpUrl<br>Returns the base URL for this server. |
| [endSessionEndpointUrl](end-session-endpoint-url.html) | [jvm]<br>fun [endSessionEndpointUrl](end-session-endpoint-url.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl<br>Returns the authorization server's end_session_endpoint for the given [issuerId](end-session-endpoint-url.html). |
| [enqueueCallback](enqueue-callback.html) | [jvm]<br>fun [enqueueCallback](enqueue-callback.html)(oAuth2TokenCallback: [OAuth2TokenCallback](../../no.nav.security.mock.oauth2.token/-o-auth2-token-callback/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Enqueues a callback at the server's HTTP request handler. This allows for customization of the token that the server issues whenever a Relying Party requests a token from the [tokenEndpointUrl](token-endpoint-url.html). |
| [enqueueResponse](enqueue-response.html) | [jvm]<br>~~fun~~ [~~enqueueResponse~~](enqueue-response.html)~~(~~response: MockResponse~~)~~ |
| [issuerUrl](issuer-url.html) | [jvm]<br>fun [issuerUrl](issuer-url.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl<br>Returns the authorization server's issuer for the given [issuerId](issuer-url.html). |
| [issueToken](issue-token.html) | [jvm]<br>fun [issueToken](issue-token.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), tokenCallback: [OAuth2TokenCallback](../../no.nav.security.mock.oauth2.token/-o-auth2-token-callback/index.html)): SignedJWT<br>Issues a signed JWT as part of the authorization code grant.<br>[jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [issueToken](issue-token.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;default&quot;, subject: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = UUID.randomUUID().toString(), audience: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = &quot;default&quot;, claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; = emptyMap(), expiry: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) = 3600): SignedJWT<br>Convenience method for issuing a signed JWT with default values. |
| [jwksUrl](jwks-url.html) | [jvm]<br>fun [jwksUrl](jwks-url.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl<br>Returns the authorization server's jwks_uri for the given [issuerId](jwks-url.html). |
| [oauth2AuthorizationServerMetadataUrl](oauth2-authorization-server-metadata-url.html) | [jvm]<br>fun [oauth2AuthorizationServerMetadataUrl](oauth2-authorization-server-metadata-url.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl<br>Returns the authorization server's well-known OAuth2 metadata discovery URL for the given [issuerId](oauth2-authorization-server-metadata-url.html). |
| [shutdown](shutdown.html) | [jvm]<br>fun [shutdown](shutdown.html)()<br>Gracefully shuts down the [MockOAuth2Server](index.html). |
| [start](start.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [start](start.html)(port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 0)<br>Starts the [MockOAuth2Server](index.html) on the localhost interface.<br>[jvm]<br>fun [start](start.html)(inetAddress: [InetAddress](https://docs.oracle.com/javase/8/docs/api/java/net/InetAddress.html), port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))<br>Starts the [MockOAuth2Server](index.html) on the given [inetAddress](start.html) IP address at the given [port](start.html). |
| [takeRequest](take-request.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [takeRequest](take-request.html)(timeout: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) = 2, unit: [TimeUnit](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeUnit.html) = TimeUnit.SECONDS): RecordedRequest<br>Awaits the next HTTP request (waiting up to the specified wait time if necessary), removes it from the queue, and returns it. Callers should use this to verify the request was sent as intended within the given time. |
| [tokenEndpointUrl](token-endpoint-url.html) | [jvm]<br>fun [tokenEndpointUrl](token-endpoint-url.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl<br>Returns the authorization server's token_endpoint for the given [issuerId](token-endpoint-url.html). |
| [url](url.html) | [jvm]<br>fun [url](url.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl<br>Returns the authorization server's issuer identifier for the given [path](url.html). The identifier is a URL without query or fragment components, e.g. http://localhost:8080/some-issuer. |
| [userInfoUrl](user-info-url.html) | [jvm]<br>fun [userInfoUrl](user-info-url.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl<br>Returns the authorization server's userinfo_endpoint for the given [issuerId](user-info-url.html). |
| [wellKnownUrl](well-known-url.html) | [jvm]<br>fun [wellKnownUrl](well-known-url.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl<br>Returns the authorization server's well-known OpenID Connect metadata discovery URL for the given [issuerId](well-known-url.html). |


## Properties


| Name | Summary |
|---|---|
| [config](config.html) | [jvm]<br>val [config](config.html): [OAuth2Config](../-o-auth2-config/index.html) |

