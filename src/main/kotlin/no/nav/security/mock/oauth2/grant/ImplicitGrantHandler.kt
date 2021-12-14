package no.nav.security.mock.oauth2.grant

import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse
import mu.KotlinLogging
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.extensions.toAccessToken
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.HttpUrl

private val log = KotlinLogging.logger {}

internal class ImplicitGrantHandler(
    private val tokenProvider: OAuth2TokenProvider,
) : GrantHandler {

    fun implicitResponse(authorizationRequest: AuthorizationRequest, tokenResponse: OAuth2TokenResponse): AuthorizationSuccessResponse {
        log.debug("issuing implicit with response type: ${authorizationRequest.responseType}")
        return ConfigurableAuthorizationResponse(
            authorizationRequest,
            tokenResponse.toAccessToken(),
            tokenResponse.expiresIn
        )
    }

    override fun tokenResponse(
        request: OAuth2HttpRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback
    ): OAuth2TokenResponse {
        val authorizationRequest = request.asAuthorizationRequest()
        val accessToken = tokenProvider.accessToken(
            ImplicitGrant.asTokenRequest(request.url, authorizationRequest),
            issuerUrl,
            oAuth2TokenCallback
        )
        return OAuth2TokenResponse(
            tokenType = "Bearer",
            accessToken = accessToken.serialize(),
            expiresIn = accessToken.expiresIn(),
            scope = authorizationRequest.scope?.toString()
        )
    }
}
