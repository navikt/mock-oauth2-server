package no.nav.security.mock.oauth2

import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

fun OkHttpClient.tokenRequest(url: HttpUrl, userPwd: Pair<String, String>, parameters: Map<String, String>) =
    this.newCall(
        Request.Builder().post(
            url = url,
            headers = Headers.headersOf("Authorization", Credentials.basic(userPwd.first, userPwd.second)),
            parameters = parameters
        )
    ).execute()

fun Request.Builder.post(url: HttpUrl, headers: Headers, parameters: Map<String, String>) =
    this.url(url)
        .headers(headers)
        .post(FormBody.Builder().of(parameters))
        .build()

fun FormBody.Builder.of(parameters: Map<String, String>) =
    this.apply {
        parameters.forEach { (k, v) -> this.add(k, v) }
    }.build()
