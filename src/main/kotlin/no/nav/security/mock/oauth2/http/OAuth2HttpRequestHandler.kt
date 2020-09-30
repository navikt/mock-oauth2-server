package no.nav.security.mock.oauth2.http

import com.nimbusds.oauth2.sdk.ErrorObject
import com.nimbusds.oauth2.sdk.GeneralException
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.GrantType.AUTHORIZATION_CODE
import com.nimbusds.oauth2.sdk.GrantType.CLIENT_CREDENTIALS
import com.nimbusds.oauth2.sdk.GrantType.JWT_BEARER
import com.nimbusds.oauth2.sdk.GrantType.parse
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.ParseException
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.debugger.DebuggerRequestHandler
import no.nav.security.mock.oauth2.extensions.isPrompt
import no.nav.security.mock.oauth2.extensions.issuerId
import no.nav.security.mock.oauth2.extensions.toAuthorizationEndpointUrl
import no.nav.security.mock.oauth2.extensions.toIssuerUrl
import no.nav.security.mock.oauth2.extensions.toJwksUrl
import no.nav.security.mock.oauth2.extensions.toTokenEndpointUrl
import no.nav.security.mock.oauth2.grant.AuthorizationCodeHandler
import no.nav.security.mock.oauth2.grant.ClientCredentialsGrantHandler
import no.nav.security.mock.oauth2.grant.GrantHandler
import no.nav.security.mock.oauth2.grant.JwtBearerGrantHandler
import no.nav.security.mock.oauth2.grant.TOKEN_EXCHANGE
import no.nav.security.mock.oauth2.grant.TokenExchangeGrantHandler
import no.nav.security.mock.oauth2.http.RequestType.ACCESS_TOKEN
import no.nav.security.mock.oauth2.http.RequestType.AUTHORIZATION
import no.nav.security.mock.oauth2.http.RequestType.DEBUGGER
import no.nav.security.mock.oauth2.http.RequestType.DEBUGGER_CALLBACK
import no.nav.security.mock.oauth2.http.RequestType.FAVICON
import no.nav.security.mock.oauth2.http.RequestType.JWKS
import no.nav.security.mock.oauth2.http.RequestType.WELL_KNOWN
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
    private val oAuth2TokenCallbackQueue: BlockingQueue<OAuth2TokenCallback> = LinkedBlockingQueue()

    private val grantHandlers: Map<GrantType, GrantHandler> = mapOf(
        AUTHORIZATION_CODE to AuthorizationCodeHandler(config.tokenProvider),
        CLIENT_CREDENTIALS to ClientCredentialsGrantHandler(config.tokenProvider),
        JWT_BEARER to JwtBearerGrantHandler(config.tokenProvider),
        TOKEN_EXCHANGE to TokenExchangeGrantHandler(config.tokenProvider)
    )

    fun handleRequest(request: OAuth2HttpRequest): OAuth2HttpResponse {
        return runCatching {
            log.debug("received request on url=${request.url} with headers=${request.headers}")
            return when (request.type()) {
                WELL_KNOWN -> json(wellKnown(request)).also { log.debug("returning well-known json data for url=${request.url}") }
                AUTHORIZATION -> {
                    log.debug("received call to authorization endpoint")
                    val authRequest: AuthenticationRequest = request.asAuthenticationRequest()
                    val authorizationCodeHandler = (grantHandler(authRequest) as AuthorizationCodeHandler)
                    return when (request.method) {
                        "GET" -> {
                            if (config.interactiveLogin || authRequest.isPrompt())
                                html(loginRequestHandler.loginHtml(request))
                            else {
                                authenticationSuccess(authorizationCodeHandler.authorizationCodeResponse(authRequest))
                            }
                        }
                        "POST" -> {
                            val login: Login = loginRequestHandler.loginSubmit(request)
                            authenticationSuccess(authorizationCodeHandler.authorizationCodeResponse(authRequest, login))
                        }
                        else -> throw OAuth2Exception(
                            OAuth2Error.INVALID_REQUEST,
                            "Unsupported request method ${request.method}"
                        )
                    }
                }
                ACCESS_TOKEN -> {
                    log.debug("handle token request $request")
                    val oAuth2TokenCallback: OAuth2TokenCallback = takeTokenCallbackOrCreateDefault(request.url.issuerId())
                    json(grantHandler(request).tokenResponse(request, request.url.toIssuerUrl(), oAuth2TokenCallback))
                }
                JWKS -> json(config.tokenProvider.publicJwkSet().toJSONObject()).also { log.debug("handle jwks request") }
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

    fun enqueueTokenCallback(oAuth2TokenCallback: OAuth2TokenCallback) = oAuth2TokenCallbackQueue.add(oAuth2TokenCallback)

    private fun takeTokenCallbackOrCreateDefault(issuerId: String): OAuth2TokenCallback {
        if (oAuth2TokenCallbackQueue.peek()?.issuerId() == issuerId) {
            return oAuth2TokenCallbackQueue.take()
        }
        return config.oAuth2TokenCallbacks.firstOrNull { it.issuerId() == issuerId }
            ?: DefaultOAuth2TokenCallback(issuerId = issuerId)
    }

    private fun handleException(error: Throwable): OAuth2HttpResponse {
        log.error("received exception when handling request.", error)
        val errorObject: ErrorObject = when (error) {
            is OAuth2Exception -> error.errorObject
            is ParseException ->
                error.errorObject
                    ?: OAuth2Error.INVALID_REQUEST
                        .appendDescription(". received exception message: ${error.message}")
            is GeneralException -> error.errorObject
            else -> null
        } ?: OAuth2Error.SERVER_ERROR
            .appendDescription(". received exception message: ${error.message}")
        return oauth2Error(errorObject)
    }

    private fun grantHandler(authenticationRequest: AuthenticationRequest): GrantHandler =
        if (authenticationRequest.responseType.impliesCodeFlow()) {
            (grantHandlers[AUTHORIZATION_CODE] as AuthorizationCodeHandler)
        } else throw OAuth2Exception(
            OAuth2Error.INVALID_GRANT,
            "hybrid og implicit flow not supported (yet)."
        )

    // TODO null check on granttype from request
    private fun grantHandler(request: OAuth2HttpRequest): GrantHandler =
        grantHandlers[parse(request.grantType())] ?: throw OAuth2Exception(
            OAuth2Error.INVALID_GRANT,
            "grant_type ${request.grantType()} not supported."
        )

    private fun wellKnown(request: OAuth2HttpRequest): WellKnown =
        WellKnown(
            issuer = request.url.toIssuerUrl().toString(),
            authorizationEndpoint = request.url.toAuthorizationEndpointUrl().toString(),
            tokenEndpoint = request.url.toTokenEndpointUrl().toString(),
            jwksUri = request.url.toJwksUrl().toString()
        )
}
