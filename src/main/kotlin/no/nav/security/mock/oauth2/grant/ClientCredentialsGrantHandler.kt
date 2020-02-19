package no.nav.security.mock.oauth2.grant

import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.callback.TokenCallback
import no.nav.security.mock.extensions.expiresIn
import no.nav.security.mock.oauth2.OAuth2TokenProvider
import no.nav.security.mock.oauth2.OAuth2TokenResponse
import okhttp3.HttpUrl
import java.util.UUID

class ClientCredentialsGrantHandler(
    private val tokenProvider: OAuth2TokenProvider
) : GrantHandler {

    override fun tokenResponse(
        tokenRequest: TokenRequest,
        issuerUrl: HttpUrl,
        tokenCallback: TokenCallback
    ): OAuth2TokenResponse {
        val accessToken = tokenProvider.accessToken(
            tokenRequest,
            issuerUrl,
            null,
            tokenCallback
        )
        return OAuth2TokenResponse(
            tokenType = "Bearer",
            idToken = null,
            accessToken = accessToken.serialize(),
            refreshToken = UUID.randomUUID().toString(),
            expiresIn = accessToken.expiresIn(),
            scope = tokenRequest.scope.toString()
        )
    }
}
