package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.extensions.toIssuerUrl
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import java.time.LocalDateTime
import java.time.ZoneId

class TokenExchangeHandler(private val tokenProvider: OAuth2TokenProvider = OAuth2TokenProvider()) {

    fun tokenResponse(request: OAuth2HttpRequest): OAuth2TokenResponse {
        val form = request.formParameters.map
        val claims = JWTClaimsSet.Builder()
            .audience(form["audience"])
            .issuer(request.url.toIssuerUrl().toString())
            .expirationTime(LocalDateTime.now().plusHours(1).toUtilDate())
            .build()
        val accessToken = tokenProvider.createSignedJWT(claims)
        return OAuth2TokenResponse(
            tokenType = "Bearer",
            issuedTokenType = "urn:ietf:params:oauth:token-type:access_token",
            accessToken = accessToken.serialize(),
            expiresIn = accessToken.expiresIn()
        )
    }

    private fun LocalDateTime.toUtilDate() =
        java.util.Date.from(this.atZone(ZoneId.systemDefault()).toInstant())

}
