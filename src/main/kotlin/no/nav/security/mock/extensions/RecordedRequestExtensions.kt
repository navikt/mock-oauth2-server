package no.nav.security.mock.extensions

import no.nav.security.mock.oauth2.OAuth2Exception
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import okhttp3.mockwebserver.RecordedRequest

fun RecordedRequest.issuerId(): String =
    this.requestUrl?.pathSegments
        ?.first()
        ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "issuerid must be first segment in url path")

fun RecordedRequest.asTokenRequest(): TokenRequest =
    TokenRequest.parse(fromFormParameters(this))

fun RecordedRequest.asAuthenticationRequest(): AuthenticationRequest =
    AuthenticationRequest.parse(this.requestUrl!!.toUri())

private fun fromFormParameters(request: RecordedRequest): HTTPRequest {
    val httpRequest = HTTPRequest(
        HTTPRequest.Method.valueOf(request.method!!),
        request.requestUrl!!.toUrl()
    )
    request.headers.forEach { httpRequest.setHeader(it.first, it.second) }
    httpRequest.query = request.body.readUtf8()
    return httpRequest
}
