package no.nav.security.mock.oauth2.http

import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import no.nav.security.mock.oauth2.extensions.clientAuthentication
import no.nav.security.mock.oauth2.extensions.keyValuesToMap
import no.nav.security.mock.oauth2.extensions.requirePrivateKeyJwt
import no.nav.security.mock.oauth2.extensions.toAuthorizationEndpointUrl
import no.nav.security.mock.oauth2.extensions.toEndSessionEndpointUrl
import no.nav.security.mock.oauth2.extensions.toIntrospectUrl
import no.nav.security.mock.oauth2.extensions.toIssuerUrl
import no.nav.security.mock.oauth2.extensions.toJwksUrl
import no.nav.security.mock.oauth2.extensions.toRevocationEndpointUrl
import no.nav.security.mock.oauth2.extensions.toTokenEndpointUrl
import no.nav.security.mock.oauth2.extensions.toUserInfoUrl
import no.nav.security.mock.oauth2.missingParameter
import okhttp3.Headers
import okhttp3.HttpUrl
import java.net.URI

data class OAuth2HttpRequest(
    val headers: Headers,
    val method: String,
    val originalUrl: HttpUrl,
    val body: String? = null,
) {
    val url: HttpUrl get() = proxyAwareUrl()
    val formParameters: Parameters = Parameters(body)
    val cookies: Map<String, String> = headers["Cookie"]?.keyValuesToMap(";") ?: emptyMap()

    fun asTokenExchangeRequest(): TokenRequest {
        val httpRequest: HTTPRequest = this.asNimbusHTTPRequest()
        val clientAuthentication = httpRequest.clientAuthentication()
        if (clientAuthentication.method == ClientAuthenticationMethod.PRIVATE_KEY_JWT) {
            clientAuthentication.requirePrivateKeyJwt(
                requiredAudience = this.url.toIssuerUrl().toString(),
                maxLifetimeSeconds = 120,
                additionalAcceptedAudience = this.url.toString(),
            )
        }
        return TokenRequest.parse(
            this.asNimbusHTTPRequest(),
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun asNimbusHTTPRequest(): HTTPRequest {
        val inputBody = body
        return HTTPRequest(HTTPRequest.Method.valueOf(method), url.toUrl())
            .apply {
                headers.forEach { header -> this.setHeader(header.first, header.second) }
                body = inputBody
            }
    }

    fun asNimbusTokenRequest(): TokenRequest =
        TokenRequest.parse(
            this.asNimbusHTTPRequest(),
        )

    fun asAuthenticationRequest(): AuthenticationRequest = AuthenticationRequest.parse(this.url.toUri())

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
            revocationEndpoint = this.proxyAwareUrl().toRevocationEndpointUrl().toString(),
            introspectionEndpoint = this.proxyAwareUrl().toIntrospectUrl().toString(),
            jwksUri = this.proxyAwareUrl().toJwksUrl().toString(),
            userInfoEndpoint = this.proxyAwareUrl().toUserInfoUrl().toString(),
        )

    internal fun proxyAwareUrl(): HttpUrl =
        HttpUrl
            .Builder()
            .scheme(resolveScheme())
            .host(resolveHost())
            .port(resolvePort())
            .encodedPath(originalUrl.encodedPath)
            .query(originalUrl.query)
            .build()

    private fun resolveScheme(): String = headers["x-forwarded-proto"] ?: originalUrl.scheme

    private fun resolveHost() = parseHostHeader()?.first ?: originalUrl.host

    private fun resolvePort(): Int {
        val xForwardedProto = this.headers["x-forwarded-proto"]
        val xForwardedPort = this.headers["x-forwarded-port"]?.toInt() ?: -1
        val hostHeaderPort = parseHostHeader()?.second ?: -1
        return when {
            xForwardedPort != -1 -> {
                xForwardedPort
            }

            hostHeaderPort != -1 -> {
                hostHeaderPort
            }

            xForwardedProto != null -> {
                if (xForwardedProto == "https") {
                    443
                } else {
                    80
                }
            }

            else -> {
                originalUrl.port
            }
        }
    }

    private fun parseHostHeader(): Pair<String, Int>? = hostAndPortFromHostHeader(this.headers["host"])

    data class Parameters(
        val parameterString: String?,
    ) {
        val map: Map<String, String> = parameterString?.keyValuesToMap("&") ?: emptyMap()

        fun get(name: String): String? = map[name]
    }
}

/**
 * Splits an HTTP `Host` header (RFC 9110 §7.2) into its host and port components.
 *
 * Returns `null` for a missing, blank or unparseable header. The port is `-1` when the header
 * carries none or an out-of-range one - the "no explicit port" sentinel the callers expect;
 * it is never a value okhttp's `HttpUrl.Builder.port` would reject.
 *
 * Bracketed IPv6 literals (`[::1]`, `[::1]:8080`) are delegated to [URI], which knows the
 * `[...]` grammar; anything [URI] rejects or does not treat as a server-based authority
 * (e.g. registry-style names with underscores) falls back to a plain colon split so
 * previously working Host headers keep working.
 */
internal fun hostAndPortFromHostHeader(hostHeader: String?): Pair<String, Int>? {
    val header = hostHeader?.takeIf { it.isNotBlank() } ?: return null

    runCatching { URI("//$header") }.getOrNull()?.let { uri ->
        if (uri.host != null) return uri.host to uri.port.asHostHeaderPort()
    }

    // URI could not parse it. A bracketed literal it rejected (unclosed bracket,
    // non-numeric port, ...) is not something the colon split can salvage either.
    if (header.startsWith("[")) return null

    val hostPort = header.split(":")
    val port = if (hostPort.size == 2) hostPort[1].toIntOrNull()?.asHostHeaderPort() ?: -1 else -1
    return hostPort[0] to port
}

/** A port is only meaningful when it is a real TCP port; anything else becomes the `-1` sentinel. */
private fun Int.asHostHeaderPort(): Int = if (this in 1..65535) this else -1
