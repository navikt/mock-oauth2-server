package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant
import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.token.AuthRequestAwareOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.token.resolveAudience
import no.nav.security.mock.oauth2.token.resolveClaims
import no.nav.security.mock.oauth2.token.resolveSubject
import no.nav.security.mock.oauth2.token.resolveTypeHeader
import okhttp3.HttpUrl

internal class PasswordGrantHandler(
    private val tokenProvider: OAuth2TokenProvider,
) : GrantHandler {
    override fun tokenResponse(
        request: OAuth2HttpRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback,
    ): OAuth2TokenResponse {
        val tokenRequest = request.asNimbusTokenRequest()
        val scope: String? = tokenRequest.scope?.toString()
        val username =
            tokenRequest.authorizationGrant
                ?.let { it as? ResourceOwnerPasswordCredentialsGrant }
                ?.username
        val passwordGrantTokenCallback = PasswordGrantTokenCallback(oAuth2TokenCallback, username)
        val accessToken: SignedJWT = tokenProvider.accessToken(tokenRequest, issuerUrl, passwordGrantTokenCallback)
        val idToken: SignedJWT = tokenProvider.idToken(tokenRequest, issuerUrl, passwordGrantTokenCallback, null)

        return OAuth2TokenResponse(
            tokenType = "Bearer",
            accessToken = accessToken.serialize(),
            idToken = idToken.serialize(),
            expiresIn = accessToken.expiresIn(),
            scope = scope,
        )
    }

    private class PasswordGrantTokenCallback(
        private val tokenCallback: OAuth2TokenCallback,
        private val username: String?,
    ) : AuthRequestAwareOAuth2TokenCallback {
        override fun issuerId(): String = tokenCallback.issuerId()

        override fun subject(
            tokenRequest: TokenRequest,
            authRequestParams: Map<String, String>,
        ): String? = username ?: tokenCallback.resolveSubject(tokenRequest, authRequestParams)

        override fun typeHeader(
            tokenRequest: TokenRequest,
            authRequestParams: Map<String, String>,
        ): String = tokenCallback.resolveTypeHeader(tokenRequest, authRequestParams.withUsername())

        override fun audience(
            tokenRequest: TokenRequest,
            authRequestParams: Map<String, String>,
        ): List<String> = tokenCallback.resolveAudience(tokenRequest, authRequestParams.withUsername())

        override fun addClaims(
            tokenRequest: TokenRequest,
            authRequestParams: Map<String, String>,
        ): Map<String, Any> = tokenCallback.resolveClaims(tokenRequest, authRequestParams.withUsername())

        override fun tokenExpiry(): Long = tokenCallback.tokenExpiry()

        private fun Map<String, String>.withUsername(): Map<String, String> =
            username?.let { this + ("subject" to it) } ?: this
    }
}
