package no.nav.security.mock.oauth2.testutils

import okhttp3.HttpUrl

fun HttpUrl.authenticationRequest(
    clientId: String = "defautlClient",
    redirectUri: String = "http://defaultRedirectUri",
    scope: List<String> = listOf("openid"),
    responseType: String = "code",
    responseMode: String = "query",
    state: String = "1234",
    nonce: String = "5678"
): HttpUrl = newBuilder()
    .addQueryParameter("client_id", clientId)
    .addQueryParameter("response_type", responseType)
    .addQueryParameter("redirect_uri", redirectUri)
    .addQueryParameter("response_mode", responseMode)
    .addQueryParameter("scope", scope.joinToString(" "))
    .addQueryParameter("state", state)
    .addQueryParameter("nonce", nonce)
    .build()
