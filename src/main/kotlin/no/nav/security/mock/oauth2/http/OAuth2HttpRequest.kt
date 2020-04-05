package no.nav.security.mock.oauth2.http

import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import okhttp3.Headers
import okhttp3.HttpUrl
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class OAuth2HttpRequest(
    val headers: Headers,
    val method: String,
    val url: HttpUrl,
    val body: String? = null
) {
    val formParameters: Parameters = Parameters(body)
    val cookies: Map<String, String> = headers["Cookie"]?.keyValuesToMap(";") ?: emptyMap()

    fun asTokenRequest(): TokenRequest =
        TokenRequest.parse(
            HTTPRequest(HTTPRequest.Method.valueOf(method), url.toUrl())
                .apply {
                    headers.forEach { header -> this.setHeader(header.first, header.second) }
                    query = body
                }
        )

    fun asAuthenticationRequest(): AuthenticationRequest = AuthenticationRequest.parse(this.url.toUri())

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
