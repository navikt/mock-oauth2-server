package no.nav.security.mock.oauth2.http

import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import no.nav.security.mock.oauth2.extensions.isAuthorizationEndpointUrl
import no.nav.security.mock.oauth2.extensions.isDebuggerCallbackUrl
import no.nav.security.mock.oauth2.extensions.isDebuggerUrl
import no.nav.security.mock.oauth2.extensions.isEndSessionEndpointUrl
import no.nav.security.mock.oauth2.extensions.isJwksUrl
import no.nav.security.mock.oauth2.extensions.isTokenEndpointUrl
import no.nav.security.mock.oauth2.extensions.isWellKnownUrl
import no.nav.security.mock.oauth2.extensions.keyValuesToMap
import no.nav.security.mock.oauth2.extensions.requirePrivateKeyJwt
import no.nav.security.mock.oauth2.extensions.toAuthorizationEndpointUrl
import no.nav.security.mock.oauth2.extensions.toEndSessionEndpointUrl
import no.nav.security.mock.oauth2.extensions.toIssuerUrl
import no.nav.security.mock.oauth2.extensions.toJwksUrl
import no.nav.security.mock.oauth2.extensions.toTokenEndpointUrl
import no.nav.security.mock.oauth2.grant.TokenExchangeGrant
import no.nav.security.mock.oauth2.http.RequestType.AUTHORIZATION
import no.nav.security.mock.oauth2.http.RequestType.DEBUGGER
import no.nav.security.mock.oauth2.http.RequestType.DEBUGGER_CALLBACK
import no.nav.security.mock.oauth2.http.RequestType.END_SESSION
import no.nav.security.mock.oauth2.http.RequestType.FAVICON
import no.nav.security.mock.oauth2.http.RequestType.JWKS
import no.nav.security.mock.oauth2.http.RequestType.TOKEN
import no.nav.security.mock.oauth2.http.RequestType.UNKNOWN
import no.nav.security.mock.oauth2.http.RequestType.WELL_KNOWN
import no.nav.security.mock.oauth2.missingParameter
import okhttp3.Headers
import okhttp3.HttpUrl

data class OAuth2HttpRequest(
    val headers: Headers,
    val method: String,
    val originalUrl: HttpUrl,
    val body: String? = null
) {
    val url: HttpUrl get() = proxyAwareUrl()
    val formParameters: Parameters = Parameters(body)
    val cookies: Map<String, String> = headers["Cookie"]?.keyValuesToMap(";") ?: emptyMap()

    fun asTokenExchangeRequest(): TokenRequest {
        val httpRequest: HTTPRequest = this.asNimbusHTTPRequest()
        val clientAuthentication = ClientAuthentication.parse(httpRequest).requirePrivateKeyJwt(this.url.toString(), 120)
        val tokenExchangeGrant = TokenExchangeGrant.parse(formParameters.map)

        return TokenRequest(
            this.url.toUri(),
            clientAuthentication,
            tokenExchangeGrant,
            null,
            emptyList(),
            formParameters.map.mapValues { mutableListOf(it.value) }
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun asNimbusHTTPRequest(): HTTPRequest {
        return HTTPRequest(HTTPRequest.Method.valueOf(method), url.toUrl())
            .apply {
                headers.forEach { header -> this.setHeader(header.first, header.second) }
                query = body
            }
    }

    fun asNimbusTokenRequest(): TokenRequest =
        TokenRequest.parse(
            this.asNimbusHTTPRequest()
        )

    fun asAuthenticationRequest(): AuthenticationRequest = AuthenticationRequest.parse(this.url.toUri())

    fun type() = when {
        url.isWellKnownUrl() -> WELL_KNOWN
        url.isAuthorizationEndpointUrl() -> AUTHORIZATION
        url.isTokenEndpointUrl() -> TOKEN
        url.isEndSessionEndpointUrl() -> END_SESSION
        url.isJwksUrl() -> JWKS
        url.isDebuggerUrl() -> DEBUGGER
        url.isDebuggerCallbackUrl() -> DEBUGGER_CALLBACK
        url.encodedPath == "/favicon.ico" -> FAVICON
        else -> UNKNOWN
    }

    fun grantType(): GrantType =
        this.formParameters.map["grant_type"]
            ?.ifBlank { null }
            ?.let { GrantType(it) }
            ?: missingParameter("grant_type")

    fun toWellKnown() =
        WellKnown(
            issuer = this.proxyAwareUrl().toIssuerUrl().toString(),
            authorizationEndpoint = this.proxyAwareUrl().toAuthorizationEndpointUrl().toString(),
            tokenEndpoint = this.proxyAwareUrl().toTokenEndpointUrl().toString(),
            endSessionEndpoint = this.proxyAwareUrl().toEndSessionEndpointUrl().toString(),
            jwksUri = this.proxyAwareUrl().toJwksUrl().toString()
        )

    internal fun proxyAwareUrl(): HttpUrl {
        val hostheader = this.headers["host"]
        val proto = this.headers["x-forwarded-proto"]
        val port = this.headers["x-forwarded-port"]
        return if (hostheader != null && proto != null) {
            HttpUrl.Builder()
                .scheme(proto)
                .host(hostheader)
                .apply {
                    port?.toInt()?.let { port(it) }
                }
                .encodedPath(originalUrl.encodedPath)
                .query(originalUrl.query).build()
        } else {
            originalUrl
        }
    }

    data class Parameters(val parameterString: String?) {
        val map: Map<String, String> = parameterString?.keyValuesToMap("&") ?: emptyMap()
        fun get(name: String): String? = map[name]
    }
}

enum class RequestType {
    WELL_KNOWN, AUTHORIZATION, TOKEN, END_SESSION, JWKS,
    DEBUGGER, DEBUGGER_CALLBACK, FAVICON, UNKNOWN
}
