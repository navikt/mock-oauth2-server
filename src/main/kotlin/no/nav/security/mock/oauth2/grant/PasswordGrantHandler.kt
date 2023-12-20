package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant
import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
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
        val passwordGrantTokenCallback = PasswordGrantTokenCallback(oAuth2TokenCallback)
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
    ) : OAuth2TokenCallback by tokenCallback {
        override fun subject(tokenRequest: TokenRequest) =
            tokenRequest.authorizationGrant
                ?.let { it as? ResourceOwnerPasswordCredentialsGrant }
                ?.username ?: tokenCallback.subject(tokenRequest)
    }
}
