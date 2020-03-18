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
    val body: String?
) {
    val formParameters: Parameters = Parameters(body)

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

        private val map: Map<String, String> =
            parameterString?.split("&")
                ?.filter { it.contains("=") }
                ?.associate {
                    val (left, right) = it.split("=")
                    decode(left) to decode(right)
                } ?: emptyMap()

        fun get(name: String): String? = map[name]
        private fun decode(string: String): String = URLDecoder.decode(string, StandardCharsets.UTF_8)
    }
}
