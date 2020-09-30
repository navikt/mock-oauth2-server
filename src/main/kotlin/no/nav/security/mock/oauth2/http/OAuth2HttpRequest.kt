package no.nav.security.mock.oauth2.http

import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import no.nav.security.mock.oauth2.extensions.isAuthorizationEndpointUrl
import no.nav.security.mock.oauth2.extensions.isDebuggerCallbackUrl
import no.nav.security.mock.oauth2.extensions.isDebuggerUrl
import no.nav.security.mock.oauth2.extensions.isJwksUrl
import no.nav.security.mock.oauth2.extensions.isTokenEndpointUrl
import no.nav.security.mock.oauth2.extensions.isWellKnownUrl
import no.nav.security.mock.oauth2.grant.TokenExchangeGrant
import no.nav.security.mock.oauth2.http.RequestType.ACCESS_TOKEN
import no.nav.security.mock.oauth2.http.RequestType.AUTHORIZATION
import no.nav.security.mock.oauth2.http.RequestType.DEBUGGER
import no.nav.security.mock.oauth2.http.RequestType.DEBUGGER_CALLBACK
import no.nav.security.mock.oauth2.http.RequestType.FAVICON
import no.nav.security.mock.oauth2.http.RequestType.JWKS
import no.nav.security.mock.oauth2.http.RequestType.UNKNOWN
import no.nav.security.mock.oauth2.http.RequestType.WELL_KNOWN
import okhttp3.Headers
import okhttp3.HttpUrl

data class OAuth2HttpRequest(
    val headers: Headers,
    val method: String,
    val url: HttpUrl,
    val body: String? = null
) {
    val formParameters: Parameters = Parameters(body)
    val cookies: Map<String, String> = headers["Cookie"]?.keyValuesToMap(";") ?: emptyMap()

    fun asTokenExchangeRequest(): TokenRequest {
        val httpRequest: HTTPRequest = this.asHTTPRequest()
        val clientAuthentication: ClientAuthentication = ClientAuthentication.parse(httpRequest)
        val tokenExchangeGrant = TokenExchangeGrant.parse(formParameters.map)
        return TokenRequest(
            this.url.toUri(),
            clientAuthentication,
            tokenExchangeGrant,
            Scope(),
            emptyList(),
            formParameters.map.mapValues { mutableListOf(it.value) }
        )
    }

    fun asHTTPRequest(): HTTPRequest {
        return HTTPRequest(HTTPRequest.Method.valueOf(method), url.toUrl())
            .apply {
                headers.forEach { header -> this.setHeader(header.first, header.second) }
                query = body
            }
    }

    fun asNimbusTokenRequest(): TokenRequest =
        TokenRequest.parse(
            this.asHTTPRequest()
        )

    fun asAuthenticationRequest(): AuthenticationRequest = AuthenticationRequest.parse(this.url.toUri())

    fun type() = when {
        url.isWellKnownUrl() -> WELL_KNOWN
        url.isAuthorizationEndpointUrl() -> AUTHORIZATION
        url.isTokenEndpointUrl() -> ACCESS_TOKEN
        url.isJwksUrl() -> JWKS
        url.isDebuggerUrl() -> DEBUGGER
        url.isDebuggerCallbackUrl() -> DEBUGGER_CALLBACK
        url.encodedPath == "/favicon.ico" -> FAVICON
        else -> UNKNOWN
    }

    fun grantType(): String? = this.formParameters.map["grant_type"]

    data class Parameters(val parameterString: String?) {
        val map: Map<String, String> = parameterString?.keyValuesToMap("&") ?: emptyMap()
        fun get(name: String): String? = map[name]
    }
}

private fun String.urlDecode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8)

private fun String.keyValuesToMap(listDelimiter: String): Map<String, String> =
    this.split(listDelimiter)
        .filter { it.contains("=") }
        .associate {
            val (key, value) = it.split("=")
            key.urlDecode().trim() to value.urlDecode().trim()
        }

enum class RequestType {
    WELL_KNOWN, AUTHORIZATION, ACCESS_TOKEN, JWKS,
    DEBUGGER, DEBUGGER_CALLBACK, FAVICON, UNKNOWN
}
