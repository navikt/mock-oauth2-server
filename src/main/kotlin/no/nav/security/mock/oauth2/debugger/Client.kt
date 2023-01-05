package no.nav.security.mock.oauth2.debugger

import com.nimbusds.oauth2.sdk.OAuth2Error
import no.nav.security.mock.oauth2.OAuth2Exception
import okhttp3.Credentials
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.toHostHeader
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import no.nav.security.mock.oauth2.http.Ssl

internal class TokenRequest(
    val url: HttpUrl,
    clientAuthentication: ClientAuthentication,
    parameters: Map<String, String>
) {
    val headers = when (clientAuthentication.clientAuthMethod) {
        ClientAuthentication.Method.CLIENT_SECRET_BASIC -> Headers.headersOf("Authorization", clientAuthentication.basic())
        else -> Headers.headersOf()
    }

    val body: String = if (clientAuthentication.clientAuthMethod == ClientAuthentication.Method.CLIENT_SECRET_POST) {
        parameters.toKeyValueString("&").plus("&${clientAuthentication.form()}")
    } else {
        parameters.toKeyValueString("&")
    }

    override fun toString(): String = "POST ${url.encodedPath} HTTP/1.1\n" +
        "Host: ${url.toHostHeader(true)}\n" +
        "Content-Type: application/x-www-form-urlencoded\n" +
        headers.joinToString("\n") {
            "${it.first}: ${it.second}"
        } +
        "\n\n$body"

    private fun Map<String, String>.toKeyValueString(entrySeparator: String): String =
        this.map { "${it.key}=${it.value}" }
            .toList().joinToString(entrySeparator)
}

internal data class ClientAuthentication(
    val clientId: String,
    val clientSecret: String,
    val clientAuthMethod: Method
) {
    fun form(): String = "client_id=${clientId.urlEncode()}&client_secret=${clientSecret.urlEncode()}"
    fun basic(): String = Credentials.basic(clientId, clientSecret, StandardCharsets.UTF_8)

    companion object {
        fun fromMap(map: Map<String, String>): ClientAuthentication =
            ClientAuthentication(
                map.require("client_id"),
                map.require("client_secret"),
                Method.valueOf(map.require("client_auth_method"))
            )

        private fun Map<String, String>.require(key: String): String =
            this[key] ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "missing required parameter $key")
    }

    enum class Method {
        CLIENT_SECRET_POST,
        CLIENT_SECRET_BASIC
    }
}

internal fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

internal fun OkHttpClient.post(tokenRequest: TokenRequest): String =
    this.newCall(
        Request.Builder()
            .headers(tokenRequest.headers)
            .url(tokenRequest.url)
            .post(tokenRequest.body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
    ).execute().body?.string() ?: throw RuntimeException("could not get response body from url=${tokenRequest.url}")

fun OkHttpClient.withSsl(ssl: Ssl, followRedirects: Boolean = false): OkHttpClient =
    newBuilder().apply {
        followRedirects(followRedirects)
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(ssl.sslKeystore.keyStore) }
        val sslContext = SSLContext.getInstance("TLS").apply { init(null, trustManagerFactory.trustManagers, null) }
        sslSocketFactory(sslContext.socketFactory, trustManagerFactory.trustManagers[0] as X509TrustManager)
    }.build()
