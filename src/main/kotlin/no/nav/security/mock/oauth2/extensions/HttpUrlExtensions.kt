package no.nav.security.mock.oauth2.extensions

import com.nimbusds.oauth2.sdk.OAuth2Error
import no.nav.security.mock.oauth2.OAuth2Exception
import okhttp3.HttpUrl

fun HttpUrl.isWellKnownUrl(): Boolean = this == this.toWellKnownUrl()
fun HttpUrl.isAuthorizationEndpointUrl(): Boolean = this.withoutQuery() == this.toAuthorizationEndpointUrl()
fun HttpUrl.isTokenEndpointUrl(): Boolean = this == this.toTokenEndpointUrl()
fun HttpUrl.isJwksUrl(): Boolean = this == this.toJwksUrl()
fun HttpUrl.isDebuggerUrl(): Boolean = this.withoutQuery() == this.toDebuggerUrl()
fun HttpUrl.isDebuggerCallbackUrl(): Boolean = this.withoutQuery() == this.toDebuggerCallbackUrl()

fun HttpUrl.toWellKnownUrl(): HttpUrl = this.resolvePath("/${issuerId()}/.well-known/openid-configuration")
fun HttpUrl.toAuthorizationEndpointUrl(): HttpUrl = this.resolvePath("/${issuerId()}/authorize")
fun HttpUrl.toTokenEndpointUrl(): HttpUrl = this.resolvePath("/${issuerId()}/token")
fun HttpUrl.toJwksUrl(): HttpUrl = this.resolvePath("/${issuerId()}/jwks")
fun HttpUrl.toIssuerUrl(): HttpUrl = this.resolvePath("/${issuerId()}")
fun HttpUrl.toDebuggerUrl(): HttpUrl = this.resolvePath("/${issuerId()}/debugger")
fun HttpUrl.toDebuggerCallbackUrl(): HttpUrl = this.resolvePath("/${issuerId()}/debugger/callback")

fun HttpUrl.issuerId(): String = this.pathSegments.getOrNull(0)
    ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "issuerId must be first segment in url path")

private fun HttpUrl.withoutQuery(): HttpUrl = this.newBuilder().query(null).build()

private fun HttpUrl.resolvePath(path: String): HttpUrl {

    return HttpUrl.Builder()
        .scheme(this.scheme)
        .host(this.host)
        .port(this.port)
        .build()
        .resolve(path.removePrefix("/")) ?: throw OAuth2Exception(
        OAuth2Error.INVALID_REQUEST,
        "cannot resolve path $path"
    )
}
