---
title: oauth2AuthorizationServerMetadataUrl
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2](../index.html)/[MockOAuth2Server](index.html)/[oauth2AuthorizationServerMetadataUrl](oauth2-authorization-server-metadata-url.html)



# oauth2AuthorizationServerMetadataUrl



[jvm]\
fun [oauth2AuthorizationServerMetadataUrl](oauth2-authorization-server-metadata-url.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl



Returns the authorization server's well-known OAuth2 metadata discovery URL for the given [issuerId](oauth2-authorization-server-metadata-url.html).



E.g. http://localhost:8080/some-issuer/.well-known/oauth-authorization-server.



See also [RFC8414 - OAuth 2.0 Authorization Server Metadata](https://datatracker.ietf.org/doc/html/rfc8414).



## Parameters


jvm

| | |
|---|---|
| issuerId | The path or identifier for the issuer. |




