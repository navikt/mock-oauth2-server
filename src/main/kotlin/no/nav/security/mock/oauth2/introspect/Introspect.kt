package no.nav.security.mock.oauth2.introspect

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.oauth2.sdk.OAuth2Error
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.INTROSPECT
import no.nav.security.mock.oauth2.extensions.toIssuerUrl
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
            json(
                IntrospectResponse(
                    true,
                    it.getStringClaim("scope"),
                    it.getStringClaim("client_id"),
                    it.getStringClaim("username"),
                    it.getStringClaim("token_type") ?: "Bearer",
                    it.expirationTime?.time?.div(1000),
                    it.issueTime?.time?.div(1000),
                    it.notBeforeTime?.time?.div(1000),
                    it.subject,
                    it.audience,
                    it.issuer,
                    it.jwtid,
                ),
            )
        } ?: json(IntrospectResponse(false))
    }

private fun OAuth2HttpRequest.verifyToken(tokenProvider: OAuth2TokenProvider): JWTClaimsSet? {
    return try {
        this.formParameters.get("token")?.let {
            tokenProvider.verify(url.toIssuerUrl(), it)
        }
    } catch (e: Exception) {
        log.debug("token_introspection: failed signature validation")
        return null
    }
}

private fun Headers.authenticated(): Boolean =
    this["Authorization"]?.let { authHeader ->
        authHeader.auth("Bearer ")?.isNotEmpty()
            ?: authHeader.auth("Basic ")?.isNotEmpty()
            ?: false
    } ?: false

private fun String.auth(method: String): String? =
    this
        .split(method)
        .takeIf { it.size == 2 }
        ?.last()

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
    @JsonFormat(with = [JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED])
    val aud: List<String>? = null,
    @JsonProperty("iss")
    val iss: String? = null,
    @JsonProperty("jti")
    val jti: String? = null,
)
