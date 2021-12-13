package no.nav.security.mock.oauth2.grant

import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.HttpUrl

internal class ImplicitGrantHandler(
    private val tokenProvider: OAuth2TokenProvider,
) : GrantHandler {

    fun implicitResponse(authorizationRequest: AuthorizationRequest, tokenResponse: OAuth2TokenResponse): AuthorizationSuccessResponse {
        return AuthorizationSuccessResponse(
            authorizationRequest.redirectionURI,
            null,
            BearerAccessToken.parse("${tokenResponse.tokenType} ${tokenResponse.accessToken}"),
            authorizationRequest.state,
            null,
            authorizationRequest.responseMode
        )
    }

    override fun tokenResponse(
        request: OAuth2HttpRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback
    ): OAuth2TokenResponse {
        val authorizationRequest = request.asAuthorizationRequest()
        val accessToken = tokenProvider.implicitAccessToken(
            authorizationRequest,
            issuerUrl,
            oAuth2TokenCallback
        )
        return OAuth2TokenResponse(
            tokenType = "Bearer",
            accessToken = accessToken.serialize(),
            expiresIn = accessToken.expiresIn(),
            scope = request.asAuthorizationRequest().scope.toString()
        )
    }
}
