---
title: wellKnownUrl
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2](../index.html)/[MockOAuth2Server](index.html)/[wellKnownUrl](well-known-url.html)



# wellKnownUrl



[jvm]\
fun [wellKnownUrl](well-known-url.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl



Returns the authorization server's well-known OpenID Connect metadata discovery URL for the given [issuerId](well-known-url.html).



E.g. http://localhost:8080/some-issuer/.well-known/openid-configuration.



See also [OpenID Provider metadata](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata).



## Parameters


jvm

| | |
|---|---|
| issuerId | The path or identifier for the issuer. |




