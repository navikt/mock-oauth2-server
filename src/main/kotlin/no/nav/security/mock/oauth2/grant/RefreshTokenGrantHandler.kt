package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.RefreshTokenGrant
import com.nimbusds.oauth2.sdk.TokenRequest
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.extensions.issuerId
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.HttpUrl

private val log = KotlinLogging.logger {}

internal class RefreshTokenGrantHandler(
    private val tokenProvider: OAuth2TokenProvider,
    private val refreshTokenManager: RefreshTokenManager,
    private val rotateRefreshToken: Boolean = false,
    private val enqueuedCallbackSupplier: ((issuerId: String) -> OAuth2TokenCallback?)? = null,
) : GrantHandler {
    override fun tokenResponse(
        request: OAuth2HttpRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback,
    ): OAuth2TokenResponse {
        val tokenRequest = request.asNimbusTokenRequest()
        var refreshToken = tokenRequest.refreshTokenGrant().refreshToken.value
        log.debug("issuing token for refreshToken=$refreshToken")
        val scope: String? = tokenRequest.scope?.toString()
        val issuerId = issuerUrl.issuerId()
        val (storedCallback, storedAuthRequestParams) = refreshTokenManager[refreshToken] ?: Pair(null, emptyMap())
        if (storedCallback != null && storedCallback.issuerId() != issuerId) {
            throw OAuth2Exception(OAuth2Error.INVALID_GRANT.setDescription("refresh_token was issued by a different issuer"), "refresh_token issuer mismatch")
        }
        val enqueuedCallback = enqueuedCallbackSupplier?.invoke(issuerId)
        val resolvedCallback =
            enqueuedCallback
                ?: storedCallback
                ?: throw OAuth2Exception(OAuth2Error.INVALID_GRANT.setDescription("unknown refresh_token"), "unknown refresh_token")

        val authRequestParams = storedAuthRequestParams

        if (rotateRefreshToken) {
            refreshToken = refreshTokenManager.rotate(refreshToken, resolvedCallback, authRequestParams)
        }
        val idToken: SignedJWT = tokenProvider.idToken(tokenRequest, issuerUrl, resolvedCallback, null, authRequestParams)
        val accessToken: SignedJWT = tokenProvider.accessToken(tokenRequest, issuerUrl, resolvedCallback, null, authRequestParams)

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
        (this.authorizationGrant as? RefreshTokenGrant)
            ?: throw OAuth2Exception(OAuth2Error.INVALID_GRANT.setDescription("grant_type ${GrantType.REFRESH_TOKEN} not supported."), "grant_type ${GrantType.REFRESH_TOKEN} not supported.")
}
