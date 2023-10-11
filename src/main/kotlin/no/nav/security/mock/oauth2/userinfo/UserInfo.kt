package no.nav.security.mock.oauth2.userinfo

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.ErrorObject
import com.nimbusds.oauth2.sdk.http.HTTPResponse
import com.nimbusds.oauth2.sdk.id.Issuer
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.USER_INFO
import no.nav.security.mock.oauth2.extensions.issuerId
import no.nav.security.mock.oauth2.extensions.toIssuerUrl
import no.nav.security.mock.oauth2.extensions.verifySignatureAndIssuer
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.Route
import no.nav.security.mock.oauth2.http.json
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.Headers

private val log = KotlinLogging.logger { }

internal fun Route.Builder.userInfo(tokenProvider: OAuth2TokenProvider) =
    get(USER_INFO) {
        log.debug("received request to userinfo endpoint, returning claims from token")
        val claims = it.verifyBearerToken(tokenProvider).claims
        json(claims)
    }

private fun OAuth2HttpRequest.verifyBearerToken(tokenProvider: OAuth2TokenProvider): JWTClaimsSet {
    val tokenString = this.headers.bearerToken()
    val issuer = url.toIssuerUrl()
    val jwkSet = tokenProvider.publicJwkSet(issuer.issuerId())
    val algorithm = tokenProvider.getAlgorithm()
    return try {
        SignedJWT.parse(tokenString).verifySignatureAndIssuer(Issuer(issuer.toString()), jwkSet, algorithm)
    } catch (e: Exception) {
        throw invalidToken(e.message ?: "could not verify bearer token")
    }
}

private fun Headers.bearerToken(): String =
    this["Authorization"]?.split("Bearer ")
        ?.takeIf { it.size == 2 }
        ?.last()
        ?: throw invalidToken("missing bearer token")

// OpenID Connect Core - https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse
// OAuth 2.0 Bearer Token Usage - https://datatracker.ietf.org/doc/html/rfc6750#section-3.1
private fun invalidToken(msg: String) =
    OAuth2Exception(
        ErrorObject(
            "invalid_token",
            msg,
            HTTPResponse.SC_UNAUTHORIZED,
        ),
        msg,
    )
