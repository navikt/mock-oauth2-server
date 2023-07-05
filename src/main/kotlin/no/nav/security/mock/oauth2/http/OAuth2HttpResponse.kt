package no.nav.security.mock.oauth2.http

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.oauth2.sdk.ErrorObject
import com.nimbusds.oauth2.sdk.ResponseMode
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse
import io.netty.handler.codec.http.HttpHeaderNames
import no.nav.security.mock.oauth2.templates.TemplateMapper
import no.nav.security.mock.oauth2.token.KeyGenerator
import okhttp3.Headers

val objectMapper: ObjectMapper = jacksonObjectMapper()
val templateMapper: TemplateMapper = TemplateMapper.create {}

data class OAuth2HttpResponse(
    val headers: Headers = Headers.headersOf(),
    val status: Int,
    val body: String? = null,
)

data class WellKnown(
    val issuer: String,
    @JsonProperty("authorization_endpoint")
    val authorizationEndpoint: String,
    @JsonProperty("end_session_endpoint")
    val endSessionEndpoint: String,
    @JsonProperty("token_endpoint")
    val tokenEndpoint: String,
    @JsonProperty("userinfo_endpoint")
    val userInfoEndpoint: String,
    @JsonProperty("jwks_uri")
    val jwksUri: String,
    @JsonProperty("introspection_endpoint")
    val introspectionEndpoint: String,
    @JsonProperty("response_types_supported")
    val responseTypesSupported: List<String> = listOf("query", "fragment", "form_post"),
    @JsonProperty("subject_types_supported")
    val subjectTypesSupported: List<String> = listOf("public"),
    @JsonProperty("id_token_signing_alg_values_supported")
    val idTokenSigningAlgValuesSupported: List<String> = (KeyGenerator.ecAlgorithmFamily + KeyGenerator.rsaAlgorithmFamily).map { it.name }.toList(),
    @JsonProperty("code_challenge_methods_supported")
    val codeChallengeMethodsSupported: List<String> = listOf("plain", "S256"),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OAuth2TokenResponse(
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("issued_token_type")
    val issuedTokenType: String? = null,
    @JsonProperty("id_token")
    val idToken: String? = null,
    @JsonProperty("access_token")
    val accessToken: String?,
    @JsonProperty("refresh_token")
    val refreshToken: String? = null,
    @JsonProperty("expires_in")
    val expiresIn: Int = 0,
    @JsonProperty("scope")
    val scope: String? = null,
)

fun json(anyObject: Any): OAuth2HttpResponse = OAuth2HttpResponse(
    headers = Headers.headersOf(
        HttpHeaderNames.CONTENT_TYPE.toString(),
        "application/json;charset=UTF-8",
    ),
    status = 200,
    body = when (anyObject) {
        is String -> anyObject
        else ->
            objectMapper
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(anyObject)
    },
)

fun html(content: String): OAuth2HttpResponse = OAuth2HttpResponse(
    headers = Headers.headersOf(
        HttpHeaderNames.CONTENT_TYPE.toString(),
        "text/html;charset=UTF-8",
    ),
    status = 200,
    body = content,
)

fun redirect(location: String, headers: Headers = Headers.headersOf()): OAuth2HttpResponse = OAuth2HttpResponse(
    headers = Headers.headersOf(HttpHeaderNames.LOCATION.toString(), location).newBuilder().addAll(headers).build(),
    status = 302,
)

fun notFound(body: String? = null): OAuth2HttpResponse = OAuth2HttpResponse(status = 404, body = body)
fun methodNotAllowed(): OAuth2HttpResponse = OAuth2HttpResponse(status = 405, body = "method not allowed")

fun authenticationSuccess(authenticationSuccessResponse: AuthenticationSuccessResponse): OAuth2HttpResponse {
    return when (authenticationSuccessResponse.responseMode) {
        ResponseMode.FORM_POST -> {
            OAuth2HttpResponse(
                status = 200,
                body = templateMapper.authorizationCodeResponseHtml(
                    authenticationSuccessResponse.redirectionURI.toString(),
                    authenticationSuccessResponse.authorizationCode.value,
                    authenticationSuccessResponse.state.value,
                ),
            )
        }
        else -> OAuth2HttpResponse(
            headers = Headers.headersOf(HttpHeaderNames.LOCATION.toString(), authenticationSuccessResponse.toURI().toString()),
            status = 302,
        )
    }
}

fun oauth2Error(error: ErrorObject): OAuth2HttpResponse {
    val responseCode = error.httpStatusCode.takeUnless { it == 302 } ?: 400
    return OAuth2HttpResponse(
        headers = Headers.headersOf(
            HttpHeaderNames.CONTENT_TYPE.toString(),
            "application/json;charset=UTF-8",
        ),
        status = responseCode,
        body = objectMapper
            .enable(SerializationFeature.INDENT_OUTPUT)
            .writeValueAsString(error.toJSONObject())
            .lowercase(),
    )
}
