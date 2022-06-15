---
title: issueToken
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2](../index.html)/[MockOAuth2Server](index.html)/[issueToken](issue-token.html)



# issueToken



[jvm]\
fun [issueToken](issue-token.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), tokenCallback: [OAuth2TokenCallback](../../no.nav.security.mock.oauth2.token/-o-auth2-token-callback/index.html)): SignedJWT



Issues a signed JWT as part of the authorization code grant.



## Parameters


jvm

| | |
|---|---|
| issuerId | The path or identifier for the issuer. |
| clientId | The identifier for the client or Relying Party that requests the token. |
| tokenCallback | A callback that implements the [OAuth2TokenCallback](../../no.nav.security.mock.oauth2.token/-o-auth2-token-callback/index.html) interface for token customization. |





[jvm]\




@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)



fun [issueToken](issue-token.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;default&quot;, subject: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = UUID.randomUUID().toString(), audience: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = &quot;default&quot;, claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; = emptyMap(), expiry: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) = 3600): SignedJWT



Convenience method for issuing a signed JWT with default values.



See [issueToken](issue-token.html).




