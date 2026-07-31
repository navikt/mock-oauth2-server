package no.nav.security.mock.oauth2.debugger

import com.nimbusds.oauth2.sdk.OAuth2Error
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.DEBUGGER
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.DEBUGGER_CALLBACK
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.TOKEN
import no.nav.security.mock.oauth2.extensions.issuerId
import no.nav.security.mock.oauth2.extensions.removeAllEncodedQueryParams
import no.nav.security.mock.oauth2.extensions.toAuthorizationEndpointUrl
import no.nav.security.mock.oauth2.extensions.toDebuggerCallbackUrl
import no.nav.security.mock.oauth2.extensions.toDebuggerUrl
import no.nav.security.mock.oauth2.extensions.toTokenEndpointUrl
import no.nav.security.mock.oauth2.http.ExceptionHandler
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.http.OAuth2HttpServer
import no.nav.security.mock.oauth2.http.Route
import no.nav.security.mock.oauth2.http.Ssl
import no.nav.security.mock.oauth2.http.html
import no.nav.security.mock.oauth2.http.redirect
import no.nav.security.mock.oauth2.http.routes
import no.nav.security.mock.oauth2.http.templateMapper
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

private val log = KotlinLogging.logger { }
private val client: OkHttpClient = OkHttpClient().newBuilder().build()

class DebuggerRequestHandler(
    httpServer: OAuth2HttpServer,
    sessionManager: SessionManager = SessionManager(),
    route: Route =
        routes {
            exceptionHandler(handle(sessionManager))
            debuggerForm(sessionManager)
            debuggerCallback(sessionManager, httpServer::url, httpServer.sslConfig())
        },
) : Route by route

private fun handle(sessionManager: SessionManager): ExceptionHandler =
    { request, error ->
        OAuth2HttpResponse(
            status = 500,
            headers = Headers.headersOf("Content-Type", "text/html", "Set-Cookie", sessionManager.session(request).asCookie()),
            body = templateMapper.debuggerErrorHtml(request.url.toDebuggerUrl(), error.stackTraceToString()),
        ).also {
            log.error("received exception when handling url=${request.url}", error)
        }
    }

private fun Route.Builder.debuggerForm(sessionManager: SessionManager) =
    apply {
        get(DEBUGGER) {
            log.debug("handling GET request, return html form")
            val url =
                it.url
                    .toAuthorizationEndpointUrl()
                    .newBuilder()
                    .query(
                        "client_id=debugger" +
                            "&response_type=code" +
                            "&redirect_uri=${it.url.toDebuggerCallbackUrl()}" +
                            "&response_mode=query" +
                            "&scope=openid+somescope" +
                            "&state=1234" +
                            "&nonce=5678",
                    ).build()
            html(templateMapper.debuggerFormHtml(url, "CLIENT_SECRET_BASIC"))
        }
        post(DEBUGGER) {
            log.debug("handling POST request, return redirect")
            // the debugger only ever drives this server's own flow, so the endpoint comes from the request url
            // and not from the submitted form, which would make this an open redirect
            val callbackUrl = it.url.toDebuggerCallbackUrl().toString()
            val httpUrl =
                it.url
                    .toAuthorizationEndpointUrl()
                    .newBuilder()
                    .encodedQuery(it.formParameters.parameterString)
                    .removeAllEncodedQueryParams("authorize_url", "token_url", "client_secret", "client_auth_method", "redirect_uri")
                    // a callback elsewhere cannot validate state or nonce, as it never initiated the flow
                    .addQueryParameter("redirect_uri", callbackUrl)
                    .build()

            log.debug("attempting to redirect to $httpUrl, setting received params in encrypted cookie")
            val session = sessionManager.session(it)
            session.putAll(it.formParameters.map + ("redirect_uri" to callbackUrl))
            redirect(httpUrl.toString(), Headers.headersOf("Set-Cookie", session.asCookie()))
        }
    }

private fun Route.Builder.debuggerCallback(
    sessionManager: SessionManager,
    serverUrl: (String) -> HttpUrl,
    ssl: Ssl? = null,
) = any(DEBUGGER_CALLBACK) {
    log.debug("handling ${it.method} request to debugger callback")
    val session = sessionManager.session(it)
    // the request is sent to the server's own bound address, never to a url derived from the request itself:
    // request urls are proxy aware, so a client controlled Host header would otherwise pick the target
    val target = serverUrl(it.url.tokenEndpointPath()).viaLoopbackIfWildcard()
    val tokenUrl: HttpUrl = it.url.toTokenEndpointUrl()
    val code: String =
        it.url.queryParameter("code")
            ?: it.formParameters.get("code")
            ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "no code parameter present")
    val clientAuthentication = ClientAuthentication.fromMap(session.parameters)
    val request =
        TokenRequest(
            tokenUrl,
            clientAuthentication,
            mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "scope" to session["scope"].urlEncode(),
                "redirect_uri" to session["redirect_uri"].urlEncode(),
            ),
        )
    val response =
        if (ssl != null) {
            client.withSsl(ssl).post(request, target)
        } else {
            client.post(request, target)
        }
    html(templateMapper.debuggerCallbackHtml(request.toString(), response))
}

private fun HttpUrl.tokenEndpointPath(): String = issuerId().let { if (it.isEmpty()) TOKEN else "/$it$TOKEN" }

// a server bound to a wildcard address reports it as its host, which is not connectable
private fun HttpUrl.viaLoopbackIfWildcard(): HttpUrl = if (host in setOf("0.0.0.0", "::")) newBuilder().host("localhost").build() else this
