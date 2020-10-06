package no.nav.security.mock.oauth2.testutils

import java.net.URLEncoder
import no.nav.security.mock.oauth2.extensions.keyValuesToMap
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

fun Response.toTokenResponse(): ParsedTokenResponse = ParsedTokenResponse(
    this.code,
    checkNotNull(this.body).string()
)

val Response.authorizationCode: String?
    get() =
        this.headers["location"]?.let {
            it.substringAfter("?").keyValuesToMap("&")["code"]
        }

fun OkHttpClient.tokenRequest(url: HttpUrl, parameters: Map<String, String>): Response =
    tokenRequest(url, Headers.headersOf(), parameters)

fun OkHttpClient.tokenRequest(
    url: HttpUrl,
    headers: Headers,
    parameters: Map<String, String>
): Response =
    this.newCall(
        Request.Builder().post(
            url = url,
            headers = headers,
            parameters = parameters
        )
    ).execute()

fun OkHttpClient.tokenRequest(
    url: HttpUrl,
    basicAuth: Pair<String, String>,
    parameters: Map<String, String>
): Response =
    tokenRequest(
        url,
        Headers.headersOf("Authorization", Credentials.basic(basicAuth.first, basicAuth.second)),
        parameters
    )

fun OkHttpClient.authenticationRequest(
    url: HttpUrl,
    subject: String,
    clientId: String = "defaultClient",
    redirectUri: String = "https://defaultUri",
    scope: String = "openid"
) = this.post(
    url.of(
        mapOf(
            "client_id" to clientId,
            "response_type" to "code",
            "redirect_uri" to redirectUri,
            "response_mode" to "query",
            "scope" to URLEncoder.encode(scope, "UTF-8"),
            "state" to "1234",
            "nonce" to "5678"
        )
    ),
    mapOf(
        "username" to subject,
    )
)

fun OkHttpClient.post(
    url: HttpUrl,
    parameters: Map<String, String>
): Response =
    this.newCall(
        Request.Builder().post(
            url = url,
            headers = Headers.headersOf(),
            parameters = parameters
        )
    ).execute()

fun OkHttpClient.get(
    url: HttpUrl,
    parameters: Map<String, String>
): Response =
    this.newCall(
        Request.Builder().get(
            url,
            parameters
        )
    ).execute()

fun Request.Builder.get(url: HttpUrl, parameters: Map<String, String>) =
    this.url(url.of(parameters))
        .get()
        .build()

fun Request.Builder.post(url: HttpUrl, headers: Headers, parameters: Map<String, String>) =
    this.url(url)
        .headers(headers)
        .post(FormBody.Builder().of(parameters))
        .build()

fun HttpUrl.of(parameters: Map<String, String>) =
    this.newBuilder().apply {
        parameters.forEach { (k, v) -> this.addEncodedQueryParameter(k, URLEncoder.encode(v, "UTF-8")) }
    }.build()

fun FormBody.Builder.of(parameters: Map<String, String>) =
    this.apply {
        parameters.forEach { (k, v) -> this.add(k, v) }
    }.build()
