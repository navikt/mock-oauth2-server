package no.nav.security.mock.extensions

import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockWebServer
import java.net.URLEncoder

fun MockWebServer.wellKnownUrl(issuerId: String): HttpUrl = this.url(issuerId).toWellKnownUrl()
fun MockWebServer.tokenEndpointUrl(issuerId: String): HttpUrl = this.url(issuerId).toTokenEndpointUrl()
fun MockWebServer.jwksUrl(issuerId: String): HttpUrl = this.url(issuerId).toJwksUrl()
fun MockWebServer.issuerUrl(issuerId: String): HttpUrl = this.url(issuerId)
fun MockWebServer.authorizationEndpointUrl(issuerId: String): HttpUrl = this.url(issuerId).toAuthorizationEndpointUrl()

private fun MockWebServer.url(issuerId: String, path: String): HttpUrl =
    this.url("${URLEncoder.encode(issuerId, "UTF-8")}/${path.removePrefix("/")}")