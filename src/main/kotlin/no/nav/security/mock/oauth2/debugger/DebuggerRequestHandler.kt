package no.nav.security.mock.oauth2.debugger

import com.nimbusds.oauth2.sdk.OAuth2Error
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.DEBUGGER
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.DEBUGGER_CALLBACK
import no.nav.security.mock.oauth2.extensions.removeAllEncodedQueryParams
import no.nav.security.mock.oauth2.extensions.toAuthorizationEndpointUrl
import no.nav.security.mock.oauth2.extensions.toDebuggerCallbackUrl
import no.nav.security.mock.oauth2.extensions.toDebuggerUrl
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.http.Route
import no.nav.security.mock.oauth2.http.html
import no.nav.security.mock.oauth2.http.redirect
import no.nav.security.mock.oauth2.http.routes
import no.nav.security.mock.oauth2.templates.TemplateMapper
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

private val log = KotlinLogging.logger { }
private val client: OkHttpClient = OkHttpClient().newBuilder().build()

class DebuggerRequestHandler(private val templateMapper: TemplateMapper) : Route {

    private val sessionManager = SessionManager()

    private val routes = routes {
        debuggerForm()
        debuggerCallback()
    }

    override fun match(request: OAuth2HttpRequest): Boolean = routes.match(request)

    override fun invoke(request: OAuth2HttpRequest): OAuth2HttpResponse =
        try {
            routes.invoke(request)
        } catch (e: Exception) {
            handleException(request, e)
        }

    private fun Route.Builder.debuggerForm() = apply {
        get(DEBUGGER) {
            log.debug("handling GET request, return html form")
            val url = it.url.toAuthorizationEndpointUrl().newBuilder().query(
                "client_id=debugger" +
                    "&response_type=code" +
                    "&redirect_uri=${it.url.toDebuggerCallbackUrl()}" +
                    "&response_mode=query" +
                    "&scope=openid+somescope" +
                    "&state=1234" +
                    "&nonce=5678"
            ).build()
            html(templateMapper.debuggerFormHtml(url, "CLIENT_SECRET_BASIC"))
        }
        post(DEBUGGER) {
            log.debug("handling POST request, return redirect")
            val authorizeUrl = it.formParameters.get("authorize_url") ?: error("authorize_url is missing")
            val httpUrl = authorizeUrl.toHttpUrl().newBuilder()
                .encodedQuery(it.formParameters.parameterString)
                .removeAllEncodedQueryParams("authorize_url", "token_url", "client_secret", "client_auth_method")
                .build()

            log.debug("attempting to redirect to $httpUrl, setting received params in encrypted cookie")
            val session = sessionManager.session(it)
            session.putAll(it.formParameters.map)
            redirect(httpUrl.toString(), Headers.headersOf("Set-Cookie", session.asCookie()))
        }
    }

    private fun Route.Builder.debuggerCallback() =
        any(DEBUGGER_CALLBACK) {
            log.debug("handling ${it.method} request to debugger callback")
            val session = sessionManager.session(it)
            val tokenUrl: HttpUrl = session["token_url"].toHttpUrl()
            val code: String = it.url.queryParameter("code")
                ?: it.formParameters.get("code")
                ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "no code parameter present")
            val clientAuthentication = ClientAuthentication.fromMap(session.parameters)
            val request = TokenRequest(
                tokenUrl,
                clientAuthentication,
                mapOf(
                    "grant_type" to "authorization_code",
                    "code" to code,
                    "scope" to session["scope"].urlEncode(),
                    "redirect_uri" to session["redirect_uri"].urlEncode()
                )
            )
            val response = client.post(request)
            html(templateMapper.debuggerCallbackHtml(request.toString(), response))
        }

    private fun handleException(request: OAuth2HttpRequest, error: Throwable) =
        OAuth2HttpResponse(
            status = 500,
            headers = Headers.headersOf("Content-Type", "text/html", "Set-Cookie", sessionManager.session(request).asCookie()),
            body = templateMapper.debuggerErrorHtml(request.url.toDebuggerUrl(), error.stackTraceToString())
        ).also {
            log.error("received exception when handling url=${request.url}", error)
        }
}
