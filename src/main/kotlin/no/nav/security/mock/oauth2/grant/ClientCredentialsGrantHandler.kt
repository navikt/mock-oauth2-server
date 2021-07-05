package no.nav.security.mock.oauth2.grant

import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.HttpUrl

internal class ClientCredentialsGrantHandler(
    private val tokenProvider: OAuth2TokenProvider
) : GrantHandler {

    override fun tokenResponse(
        request: OAuth2HttpRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback
    ): OAuth2TokenResponse {
        val tokenRequest = request.asNimbusTokenRequest()
        val accessToken = tokenProvider.accessToken(
            tokenRequest,
            issuerUrl,
            oAuth2TokenCallback
        )
        return OAuth2TokenResponse(
            tokenType = "Bearer",
            accessToken = accessToken.serialize(),
            expiresIn = accessToken.expiresIn(),
            scope = tokenRequest.scope?.toString()
        )
    }
}
