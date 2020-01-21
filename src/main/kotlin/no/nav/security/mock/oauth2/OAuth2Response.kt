package no.nav.security.mock.oauth2

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

data class WellKnown(
    val issuer: String,
    @JsonProperty("authorization_endpoint")
    val authorizationEndpoint: String?,
    @JsonProperty("token_endpoint")
    val tokenEndpoint: String?,
    @JsonProperty("jwks_uri")
    val jwksUri: String?,
    @JsonProperty("response_types_supported")
    val responseTypesSupported: List<String> = listOf("query", "fragment", "form_post"),
    @JsonProperty("subject_types_supported")
    val subjectTypesSupported: List<String> = listOf("public"),
    @JsonProperty("id_token_signing_alg_values_supported")
    val idTokenSigningAlgValuesSupported: List<String> = listOf("RS256")
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OAuth2TokenResponse(
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("id_token")
    val idToken: String? = null,
    @JsonProperty("access_token")
    val accessToken: String?,
    @JsonProperty("refresh_token")
    val refreshToken: String? = null,
    @JsonProperty("expires_in")
    val expiresIn: Long = 0,
    @JsonProperty("scope")
    val scope: String? = null
)