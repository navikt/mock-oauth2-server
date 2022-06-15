---
title: anyToken
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2](../index.html)/[MockOAuth2Server](index.html)/[anyToken](any-token.html)



# anyToken



[jvm]\




@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)



fun [anyToken](any-token.html)(issuerUrl: HttpUrl, claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, expiry: [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html) = Duration.ofHours(1)): SignedJWT



Issues a signed JWT for a given [issuerUrl](any-token.html) containing the input set of [claims](any-token.html). The JWT's signature can be verified with the server's keys found at the [jwksUrl](jwks-url.html) endpoint.




