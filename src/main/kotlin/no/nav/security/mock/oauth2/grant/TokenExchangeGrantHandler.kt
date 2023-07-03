package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.invalidRequest
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.HttpUrl

internal class TokenExchangeGrantHandler(private val tokenProvider: OAuth2TokenProvider) : GrantHandler {

    override fun tokenResponse(
        request: OAuth2HttpRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback,
    ): OAuth2TokenResponse {
        val tokenRequest = request.asTokenExchangeRequest()
        val receivedClaimsSet = tokenRequest.subjectToken().jwtClaimsSet
        val accessToken = tokenProvider.exchangeAccessToken(
            tokenRequest,
            issuerUrl,
            receivedClaimsSet,
            oAuth2TokenCallback,
        )
        return OAuth2TokenResponse(
            tokenType = "Bearer",
            issuedTokenType = "urn:ietf:params:oauth:token-type:access_token",
            accessToken = accessToken.serialize(),
            expiresIn = accessToken.expiresIn(),
        )
    }
}

fun TokenRequest.subjectToken(): SignedJWT = SignedJWT.parse(this.tokenExchangeGrant().subjectToken)

fun TokenRequest.tokenExchangeGrant() = this.authorizationGrant as? TokenExchangeGrant ?: invalidRequest("missing token exchange grant")
