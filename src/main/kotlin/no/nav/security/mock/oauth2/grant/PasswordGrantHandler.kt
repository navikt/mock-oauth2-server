package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant
import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback
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
    ) : OAuth2TokenCallback {
        private val resolvedDelegate: OAuth2TokenCallback =
            when {
                username != null && tokenCallback is RequestMappingTokenCallback -> {
                    tokenCallback.withExtraMatchParams(mapOf(RequestMappingTokenCallback.SUBJECT_PARAM to username))
                }

                else -> {
                    tokenCallback
                }
            }

        override fun issuerId(): String = tokenCallback.issuerId()

        override fun subject(tokenRequest: TokenRequest) = username ?: tokenCallback.subject(tokenRequest)

        override fun typeHeader(tokenRequest: TokenRequest): String = resolvedDelegate.typeHeader(tokenRequest)

        override fun audience(tokenRequest: TokenRequest): List<String> = resolvedDelegate.audience(tokenRequest)

        override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> = resolvedDelegate.addClaims(tokenRequest)

        override fun tokenExpiry(): Long = tokenCallback.tokenExpiry()
    }
}
