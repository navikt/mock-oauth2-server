package no.nav.security.mock.oauth2.introspect

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.id.Issuer
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.INTROSPECT
import no.nav.security.mock.oauth2.extensions.issuerId
import no.nav.security.mock.oauth2.extensions.toIssuerUrl
import no.nav.security.mock.oauth2.extensions.verifySignatureAndIssuer
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.Route
import no.nav.security.mock.oauth2.http.json
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.Headers

private val log = KotlinLogging.logger { }

internal fun Route.Builder.introspect(tokenProvider: OAuth2TokenProvider) =
    post(INTROSPECT) { request ->
        log.debug("received request to introspect endpoint, returning active and claims from token")

        if (!request.headers.authenticated()) {
            val msg = "The client authentication was invalid"
            throw OAuth2Exception(OAuth2Error.INVALID_CLIENT.setDescription(msg), msg)
        }

        request.verifyToken(tokenProvider)?.let {
            val claims = it.claims
            json(
                IntrospectResponse(
                    true,
                    claims["scope"].toString(),
                    claims["client_id"].toString(),
                    claims["username"].toString(),
                    claims["token_type"].toString(),
                    claims["exp"] as? Long,
                    claims["iat"] as? Long,
                    claims["nbf"] as? Long,
                    claims["sub"].toString(),
                    claims["aud"].toString(),
                    claims["iss"].toString(),
                    claims["jti"].toString()
                )
            )
        } ?: json(IntrospectResponse(false))
    }

private fun OAuth2HttpRequest.verifyToken(tokenProvider: OAuth2TokenProvider): JWTClaimsSet? {
    val tokenString = this.formParameters.get("token")
    val issuer = url.toIssuerUrl()
    val jwkSet = tokenProvider.publicJwkSet(issuer.issuerId())
    return try {
        SignedJWT.parse(tokenString).verifySignatureAndIssuer(Issuer(issuer.toString()), jwkSet)
    } catch (e: Exception) {
        log.debug("token_introspection: failed signature validation")
        return null
    }
}

private fun Headers.authenticated(): Boolean {
    return this["Authorization"]?.let { authHeader ->
        authHeader.auth("Bearer ")?.isNotEmpty()
            ?: authHeader.auth("Basic ")?.isNotEmpty()
            ?: false
    } ?: false
}

private fun String.auth(method: String): String? {
    return this.split(method)
        .takeIf { it.size == 2 }
        ?.last()
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class IntrospectResponse(
    @JsonProperty("active")
    val active: Boolean,
    @JsonProperty("scope")
    val scope: String? = null,
    @JsonProperty("client_id")
    val clientId: String? = null,
    @JsonProperty("username")
    val username: String? = null,
    @JsonProperty("token_type")
    val tokenType: String? = null,
    @JsonProperty("exp")
    val exp: Long? = null,
    @JsonProperty("iat")
    val iat: Long? = null,
    @JsonProperty("nbf")
    val nbf: Long? = null,
    @JsonProperty("sub")
    val sub: String? = null,
    @JsonProperty("aud")
    val aud: String? = null,
    @JsonProperty("iss")
    val iss: String? = null,
    @JsonProperty("jti")
    val jti: String? = null
)
