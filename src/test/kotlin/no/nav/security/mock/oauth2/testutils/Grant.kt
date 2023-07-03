package no.nav.security.mock.oauth2.testutils

import com.nimbusds.oauth2.sdk.pkce.CodeChallenge
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import okhttp3.HttpUrl

fun HttpUrl.authenticationRequest(
    clientId: String = "defautlClient",
    redirectUri: String = "http://defaultRedirectUri",
    scope: List<String> = listOf("openid"),
    responseType: String = "code",
    responseMode: String = "query",
    state: String = "1234",
    nonce: String = "5678",
    pkce: Pkce? = null,
): HttpUrl = newBuilder()
    .addQueryParameter("client_id", clientId)
    .addQueryParameter("response_type", responseType)
    .addQueryParameter("redirect_uri", redirectUri)
    .addQueryParameter("response_mode", responseMode)
    .addQueryParameter("scope", scope.joinToString(" "))
    .addQueryParameter("state", state)
    .addQueryParameter("nonce", nonce)
    .apply {
        if (pkce != null) {
            addQueryParameter("code_challenge", pkce.challenge.value)
            addQueryParameter("code_challenge_method", pkce.method.value)
        }
    }
    .build()

data class Pkce(
    val verifier: CodeVerifier = CodeVerifier(),
    val method: CodeChallengeMethod = CodeChallengeMethod.S256,
) {
    val challenge: CodeChallenge = CodeChallenge.compute(method, verifier)
}
