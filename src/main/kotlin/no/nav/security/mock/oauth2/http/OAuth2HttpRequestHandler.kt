package no.nav.security.mock.oauth2.http

import com.nimbusds.oauth2.sdk.ErrorObject
import com.nimbusds.oauth2.sdk.GeneralException
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.GrantType.AUTHORIZATION_CODE
import com.nimbusds.oauth2.sdk.GrantType.CLIENT_CREDENTIALS
import com.nimbusds.oauth2.sdk.GrantType.JWT_BEARER
import com.nimbusds.oauth2.sdk.GrantType.REFRESH_TOKEN
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.ParseException
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.debugger.DebuggerRequestHandler
import no.nav.security.mock.oauth2.extensions.isPrompt
import no.nav.security.mock.oauth2.extensions.issuerId
import no.nav.security.mock.oauth2.extensions.toIssuerUrl
import no.nav.security.mock.oauth2.grant.AuthorizationCodeHandler
import no.nav.security.mock.oauth2.grant.ClientCredentialsGrantHandler
import no.nav.security.mock.oauth2.grant.GrantHandler
import no.nav.security.mock.oauth2.grant.JwtBearerGrantHandler
import no.nav.security.mock.oauth2.grant.RefreshTokenGrantHandler
import no.nav.security.mock.oauth2.grant.RefreshTokenManager
import no.nav.security.mock.oauth2.grant.TOKEN_EXCHANGE
import no.nav.security.mock.oauth2.grant.TokenExchangeGrantHandler
import no.nav.security.mock.oauth2.http.RequestType.AUTHORIZATION
import no.nav.security.mock.oauth2.http.RequestType.DEBUGGER
import no.nav.security.mock.oauth2.http.RequestType.DEBUGGER_CALLBACK
import no.nav.security.mock.oauth2.http.RequestType.END_SESSION
import no.nav.security.mock.oauth2.http.RequestType.FAVICON
import no.nav.security.mock.oauth2.http.RequestType.JWKS
import no.nav.security.mock.oauth2.http.RequestType.TOKEN
import no.nav.security.mock.oauth2.http.RequestType.WELL_KNOWN
import no.nav.security.mock.oauth2.invalidGrant
import no.nav.security.mock.oauth2.invalidRequest
import no.nav.security.mock.oauth2.login.Login
import no.nav.security.mock.oauth2.login.LoginRequestHandler
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback

private val log = KotlinLogging.logger {}

class OAuth2HttpRequestHandler(
    private val config: OAuth2Config
) {
    private val loginRequestHandler = LoginRequestHandler(templateMapper)
    private val debuggerRequestHandler = DebuggerRequestHandler(templateMapper)
    private val tokenCallbackQueue: BlockingQueue<OAuth2TokenCallback> = LinkedBlockingQueue()
    private val refreshTokenManager = RefreshTokenManager()

    private val grantHandlers: Map<GrantType, GrantHandler> = mapOf(
        AUTHORIZATION_CODE to AuthorizationCodeHandler(config.tokenProvider, refreshTokenManager),
        CLIENT_CREDENTIALS to ClientCredentialsGrantHandler(config.tokenProvider),
        JWT_BEARER to JwtBearerGrantHandler(config.tokenProvider),
        TOKEN_EXCHANGE to TokenExchangeGrantHandler(config.tokenProvider),
        REFRESH_TOKEN to RefreshTokenGrantHandler(config.tokenProvider, refreshTokenManager)
    )

    fun handleRequest(request: OAuth2HttpRequest): OAuth2HttpResponse {
        return runCatching {
            log.debug("received request on url=${request.url} with headers=${request.headers}")
            return when (request.type()) {
                WELL_KNOWN -> json(request.toWellKnown()).also { log.debug("returning well-known json data for url=${request.url}") }
                AUTHORIZATION -> handleAuthenticationRequest(request)
                TOKEN -> handleTokenRequest(request)
                END_SESSION -> handleEndSessionRequest(request)
                JWKS -> handleJwksRequest(request)
                DEBUGGER -> debuggerRequestHandler.handleDebuggerForm(request).also { log.debug("handle debugger request") }
                DEBUGGER_CALLBACK -> debuggerRequestHandler.handleDebuggerCallback(request).also { log.debug("handle debugger callback request") }
                FAVICON -> OAuth2HttpResponse(status = 200)
                else -> notFound().also { log.error("path '${request.url}' not found") }
            }
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error -> handleException(error) }
        )
    }

    fun enqueueTokenCallback(oAuth2TokenCallback: OAuth2TokenCallback) = tokenCallbackQueue.add(oAuth2TokenCallback)

    private fun handleJwksRequest(request: OAuth2HttpRequest): OAuth2HttpResponse {
        log.debug("handle jwks request on url=${request.url}")
        val issuerId = request.url.issuerId()
        val jwkSet = config.tokenProvider.publicJwkSet(issuerId)
        return json(jwkSet.toJSONObject())
    }

    private fun handleEndSessionRequest(request: OAuth2HttpRequest): OAuth2HttpResponse {
        log.debug("handle end session request $request")
        val postLogoutRedirectUri = request.url.queryParameter("post_logout_redirect_uri")
        return postLogoutRedirectUri?.let {
            redirect(postLogoutRedirectUri)
        } ?: html("logged out")
    }

    private fun handleAuthenticationRequest(request: OAuth2HttpRequest): OAuth2HttpResponse {
        log.debug("received call to authorization endpoint")
        val authRequest: AuthenticationRequest = request.asAuthenticationRequest()
        val authorizationCodeHandler = grantHandlers[AUTHORIZATION_CODE] as AuthorizationCodeHandler
        return when (request.method) {
            "GET" -> {
                if (config.interactiveLogin || authRequest.isPrompt())
                    html(loginRequestHandler.loginHtml(
                        request,
                        config.loginHeader.joinToString(System.lineSeparator()),
                        config.loginFooter.joinToString(System.lineSeparator())))
                else {
                    authenticationSuccess(authorizationCodeHandler.authorizationCodeResponse(authRequest))
                }
            }
            "POST" -> {
                val login: Login = loginRequestHandler.loginSubmit(request)
                authenticationSuccess(authorizationCodeHandler.authorizationCodeResponse(authRequest, login))
            }
            else -> invalidRequest("Unsupported request method ${request.method}")
        }
    }

    private fun handleTokenRequest(request: OAuth2HttpRequest): OAuth2HttpResponse {
        log.debug("handle token request $request")
        val grantType = request.grantType()
        val tokenCallback: OAuth2TokenCallback = tokenCallbackFromQueueOrDefault(request.url.issuerId())
        val grantHandler: GrantHandler = grantHandlers[grantType] ?: invalidGrant(grantType)
        val tokenResponse = grantHandler.tokenResponse(request, request.url.toIssuerUrl(), tokenCallback)
        return json(tokenResponse)
    }

    private fun tokenCallbackFromQueueOrDefault(issuerId: String): OAuth2TokenCallback =
        when (issuerId) {
            tokenCallbackQueue.peek()?.issuerId() -> tokenCallbackQueue.take()
            else -> {
                config.tokenCallbacks.firstOrNull { it.issuerId() == issuerId } ?: DefaultOAuth2TokenCallback(issuerId = issuerId)
            }
        }

    private fun handleException(error: Throwable): OAuth2HttpResponse {
        log.error("received exception when handling request.", error)
        val msg = URLEncoder.encode(error.message, Charset.forName("UTF-8"))
        val errorObject: ErrorObject = when (error) {
            is OAuth2Exception -> error.errorObject
            is ParseException -> error.errorObject ?: OAuth2Error.INVALID_REQUEST.setDescription("failed to parse request: $msg")
            is GeneralException -> error.errorObject
            else -> null
        } ?: OAuth2Error.SERVER_ERROR.setDescription("unexpected exception with message: $msg")
        return oauth2Error(errorObject)
    }
}
