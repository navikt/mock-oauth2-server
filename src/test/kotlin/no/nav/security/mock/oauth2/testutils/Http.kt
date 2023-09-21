package no.nav.security.mock.oauth2.testutils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.security.mock.oauth2.extensions.keyValuesToMap
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

fun Response.toTokenResponse(): ParsedTokenResponse = ParsedTokenResponse(
    this.code,
    checkNotNull(this.body).string(),
)

inline fun <reified T> Response.parse(): T = jacksonObjectMapper().readValue(checkNotNull(body.string()))

val Response.authorizationCode: String?
    get() =
        this.headers["location"]?.let {
            it.substringAfter("?").keyValuesToMap("&")["code"]
        }

fun client(followRedirects: Boolean = false): OkHttpClient =
    OkHttpClient()
        .newBuilder()
        .followRedirects(followRedirects)
        .build()

fun OkHttpClient.withTrustStore(keyStore: KeyStore, followRedirects: Boolean = false): OkHttpClient =
    newBuilder().apply {
        followRedirects(followRedirects)
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(keyStore) }
        val sslContext = SSLContext.getInstance("TLS").apply { init(null, trustManagerFactory.trustManagers, null) }
        sslSocketFactory(sslContext.socketFactory, trustManagerFactory.trustManagers[0] as X509TrustManager)
    }.build()

fun OkHttpClient.tokenRequest(url: HttpUrl, parameters: Map<String, String>): Response =
    tokenRequest(url, Headers.headersOf(), parameters)

fun OkHttpClient.tokenRequest(
    url: HttpUrl,
    headers: Headers,
    parameters: Map<String, String>,
): Response =
    this.newCall(
        Request.Builder().post(
            url = url,
            headers = headers,
            parameters = parameters,
        ),
    ).execute()

fun OkHttpClient.tokenRequest(
    url: HttpUrl,
    basicAuth: Pair<String, String>,
    parameters: Map<String, String>,
): Response =
    tokenRequest(
        url,
        Headers.headersOf("Authorization", Credentials.basic(basicAuth.first, basicAuth.second)),
        parameters,
    )

fun OkHttpClient.post(
    url: HttpUrl,
    parameters: Map<String, String>,
): Response =
    this.newCall(
        Request.Builder().post(
            url = url,
            headers = Headers.headersOf(),
            parameters = parameters,
        ),
    ).execute()

fun OkHttpClient.get(
    url: HttpUrl,
    headers: Headers = Headers.headersOf(),
    parameters: Map<String, String> = emptyMap(),
): Response =
    this.newCall(
        Request.Builder().get(
            url,
            headers,
            parameters,
        ),
    ).execute()

fun OkHttpClient.options(
    url: HttpUrl,
    headers: Headers = Headers.headersOf(),
): Response =
    this.newCall(
        Request.Builder().options(
            url,
            headers,
        ),
    ).execute()

fun Request.Builder.get(url: HttpUrl, headers: Headers = Headers.headersOf(), parameters: Map<String, String> = emptyMap()) =
    this.url(url.of(parameters))
        .headers(headers)
        .get()
        .build()

fun Request.Builder.get(url: HttpUrl, parameters: Map<String, String>) =
    this.url(url.of(parameters))
        .get()
        .build()

fun Request.Builder.post(url: HttpUrl, headers: Headers, parameters: Map<String, String>) =
    this.url(url)
        .headers(headers)
        .post(FormBody.Builder().of(parameters))
        .build()

fun Request.Builder.options(url: HttpUrl, headers: Headers = Headers.headersOf()) =
    this.url(url)
        .headers(headers)
        .method("OPTIONS", null)
        .build()

fun HttpUrl.of(parameters: Map<String, String>) =
    this.newBuilder().apply {
        parameters.forEach { (k, v) -> this.addEncodedQueryParameter(k, URLEncoder.encode(v, "UTF-8")) }
    }.build()

fun FormBody.Builder.of(parameters: Map<String, String>) =
    this.apply {
        parameters.forEach { (k, v) -> this.add(k, v) }
    }.build()
