---
title: enqueueCallback
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2](../index.html)/[MockOAuth2Server](index.html)/[enqueueCallback](enqueue-callback.html)



# enqueueCallback



[jvm]\
fun [enqueueCallback](enqueue-callback.html)(oAuth2TokenCallback: [OAuth2TokenCallback](../../no.nav.security.mock.oauth2.token/-o-auth2-token-callback/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)



Enqueues a callback at the server's HTTP request handler. This allows for customization of the token that the server issues whenever a Relying Party requests a token from the [tokenEndpointUrl](token-endpoint-url.html).



## Parameters


jvm

| | |
|---|---|
| oAuth2TokenCallback | A callback that implements the [OAuth2TokenCallback](../../no.nav.security.mock.oauth2.token/-o-auth2-token-callback/index.html) interface. |




