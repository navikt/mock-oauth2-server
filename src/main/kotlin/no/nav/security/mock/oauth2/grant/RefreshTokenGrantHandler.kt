package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.RefreshTokenGrant
import com.nimbusds.oauth2.sdk.TokenRequest
import mu.KotlinLogging
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.invalidGrant
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.HttpUrl

private val log = KotlinLogging.logger {}

internal class RefreshTokenGrantHandler(
    private val tokenProvider: OAuth2TokenProvider,
    private val refreshTokenManager: RefreshTokenManager,
) : GrantHandler {

    override fun tokenResponse(
        request: OAuth2HttpRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback,
    ): OAuth2TokenResponse {
        val tokenRequest = request.asNimbusTokenRequest()
        val refreshToken = tokenRequest.refreshTokenGrant().refreshToken.value
        log.debug("issuing token for refreshToken=$refreshToken")
        val scope: String? = tokenRequest.scope?.toString()
        val refreshTokenCallbackOrDefault = refreshTokenManager[refreshToken] ?: oAuth2TokenCallback
        val idToken: SignedJWT = tokenProvider.idToken(tokenRequest, issuerUrl, refreshTokenCallbackOrDefault)
        val accessToken: SignedJWT = tokenProvider.accessToken(tokenRequest, issuerUrl, refreshTokenCallbackOrDefault)

        return OAuth2TokenResponse(
            tokenType = "Bearer",
            idToken = idToken.serialize(),
            accessToken = accessToken.serialize(),
            refreshToken = refreshToken,
            expiresIn = idToken.expiresIn(),
            scope = scope,
        )
    }

    private fun TokenRequest.refreshTokenGrant(): RefreshTokenGrant =
        (this.authorizationGrant as? RefreshTokenGrant) ?: invalidGrant(GrantType.REFRESH_TOKEN)
}
