package no.nav.security.mock.oauth2.testutils

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

fun Request.Builder.post(url: HttpUrl, headers: Headers, parameters: Map<String, String>) =
    this.url(url)
        .headers(headers)
        .post(FormBody.Builder().of(parameters))
        .build()

fun FormBody.Builder.of(parameters: Map<String, String>) =
    this.apply {
        parameters.forEach { (k, v) -> this.add(k, v) }
    }.build()
