package no.nav.security.mock.oauth2.grant

import com.nimbusds.oauth2.sdk.AuthorizationGrant
import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse
import com.nimbusds.oauth2.sdk.GrantType.IMPLICIT
import com.nimbusds.oauth2.sdk.TokenRequest
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
        val accessToken = tokenResponse.toAccessToken()
        return AuthorizationSuccessResponse(
            authorizationRequest.redirectionURI,
            null,
            accessToken,
            authorizationRequest.state,
            null,
            authorizationRequest.impliedResponseMode()
        )
    }

    override fun tokenResponse(
        request: OAuth2HttpRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback
    ): OAuth2TokenResponse {
        val authorizationRequest = request.asAuthorizationRequest()
        val accessToken = tokenProvider.accessToken(
            asTokenRequest(request.url, authorizationRequest),
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

    private fun OAuth2TokenResponse.toAccessToken() =
        BearerAccessToken.parse("${this.tokenType} ${this.accessToken}")

    private fun asTokenRequest(url: HttpUrl, authorizationRequest: AuthorizationRequest): TokenRequest = TokenRequest(
        url.toUri(),
        authorizationRequest.clientID,
        ImplicitGrant(),
        authorizationRequest.scope
    )

    internal class ImplicitGrant : AuthorizationGrant(IMPLICIT) {
        override fun toParameters(): MutableMap<String, MutableList<String>> {
            return mutableMapOf()
        }
    }
}
