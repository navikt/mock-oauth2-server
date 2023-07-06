package no.nav.security.mock.oauth2.extensions

import com.nimbusds.oauth2.sdk.OAuth2Error
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.AUTHORIZATION
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.DEBUGGER
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.DEBUGGER_CALLBACK
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.END_SESSION
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.INTROSPECT
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.JWKS
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.OAUTH2_WELL_KNOWN
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.OIDC_WELL_KNOWN
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.REVOKE
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.TOKEN
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.USER_INFO
import okhttp3.HttpUrl

object OAuth2Endpoints {
    const val OAUTH2_WELL_KNOWN = "/.well-known/oauth-authorization-server"
    const val OIDC_WELL_KNOWN = "/.well-known/openid-configuration"
    const val AUTHORIZATION = "/authorize"
    const val TOKEN = "/token"
    const val END_SESSION = "/endsession"
    const val REVOKE = "/revoke"
    const val JWKS = "/jwks"
    const val USER_INFO = "/userinfo"
    const val INTROSPECT = "/introspect"
    const val DEBUGGER = "/debugger"
    const val DEBUGGER_CALLBACK = "/debugger/callback"

    val all = listOf(
        OAUTH2_WELL_KNOWN,
        OIDC_WELL_KNOWN,
        AUTHORIZATION,
        TOKEN,
        END_SESSION,
        REVOKE,
        JWKS,
        USER_INFO,
        INTROSPECT,
        DEBUGGER,
        DEBUGGER_CALLBACK,
    )
}

fun HttpUrl.isWellKnownUrl(): Boolean = this.endsWith(OAUTH2_WELL_KNOWN) || this.endsWith(OIDC_WELL_KNOWN)
fun HttpUrl.isAuthorizationEndpointUrl(): Boolean = this.endsWith(AUTHORIZATION)
fun HttpUrl.isTokenEndpointUrl(): Boolean = this.endsWith(TOKEN)
fun HttpUrl.isEndSessionEndpointUrl(): Boolean = this.endsWith(END_SESSION)
fun HttpUrl.isJwksUrl(): Boolean = this.endsWith(JWKS)
fun HttpUrl.isUserInfoUrl(): Boolean = this.endsWith(USER_INFO)
fun HttpUrl.isIntrospectUrl(): Boolean = this.endsWith(INTROSPECT)
fun HttpUrl.isDebuggerUrl(): Boolean = this.endsWith(DEBUGGER)
fun HttpUrl.isDebuggerCallbackUrl(): Boolean = this.endsWith(DEBUGGER_CALLBACK)

fun HttpUrl.toOAuth2AuthorizationServerMetadataUrl() = issuer(OAUTH2_WELL_KNOWN)
fun HttpUrl.toWellKnownUrl(): HttpUrl = issuer(OIDC_WELL_KNOWN)
fun HttpUrl.toAuthorizationEndpointUrl(): HttpUrl = issuer(AUTHORIZATION)
fun HttpUrl.toEndSessionEndpointUrl(): HttpUrl = issuer(END_SESSION)
fun HttpUrl.toRevocationEndpointUrl(): HttpUrl = issuer(REVOKE)
fun HttpUrl.toTokenEndpointUrl(): HttpUrl = issuer(TOKEN)
fun HttpUrl.toJwksUrl(): HttpUrl = issuer(JWKS)
fun HttpUrl.toIssuerUrl(): HttpUrl = issuer()
fun HttpUrl.toUserInfoUrl(): HttpUrl = issuer(USER_INFO)
fun HttpUrl.toIntrospectUrl(): HttpUrl = issuer(INTROSPECT)
fun HttpUrl.toDebuggerUrl(): HttpUrl = issuer(DEBUGGER)
fun HttpUrl.toDebuggerCallbackUrl(): HttpUrl = issuer(DEBUGGER_CALLBACK)

fun HttpUrl.issuerId(): String {
    val path = this.pathSegments.joinToString("/").trimPath()
    OAuth2Endpoints.all.forEach {
        if (path.endsWith(it)) {
            return path.substringBefore(it)
        }
    }
    return path
}

fun HttpUrl.Builder.removeAllEncodedQueryParams(vararg params: String) =
    apply { params.forEach { removeAllEncodedQueryParameters(it) } }

fun HttpUrl.endsWith(path: String): Boolean = this.pathSegments.joinToString("/").endsWith(path.trimPath())

private fun String.trimPath() = removePrefix("/").removeSuffix("/")

private fun HttpUrl.issuer(path: String = ""): HttpUrl =
    baseUrl().let {
        it.resolve(joinPaths(issuerId(), path))
            ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "cannot resolve path $path")
    }

private fun joinPaths(vararg path: String) =
    path.filter { it.isNotEmpty() }.joinToString("/") { it.trimPath() }

private fun HttpUrl.baseUrl(): HttpUrl =
    HttpUrl.Builder()
        .scheme(this.scheme)
        .host(this.host)
        .port(this.port)
        .build()
