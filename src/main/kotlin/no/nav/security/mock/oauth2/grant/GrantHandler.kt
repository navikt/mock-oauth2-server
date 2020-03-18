package no.nav.security.mock.oauth2.grant

import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import okhttp3.HttpUrl

interface GrantHandler {
    fun tokenResponse(
        tokenRequest: TokenRequest,
        issuerUrl: HttpUrl,
        OAuth2TokenCallback: OAuth2TokenCallback
    ): OAuth2TokenResponse
}
