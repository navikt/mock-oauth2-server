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
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.debugger.DebuggerRequestHandler
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.AUTHORIZATION
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.END_SESSION
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.JWKS
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.OAUTH2_WELL_KNOWN
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.OIDC_WELL_KNOWN
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.TOKEN
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
import no.nav.security.mock.oauth2.invalidGrant
import no.nav.security.mock.oauth2.login.Login
import no.nav.security.mock.oauth2.login.LoginRequestHandler
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.userinfo.userInfo
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

private val log = KotlinLogging.logger {}

class OAuth2HttpRequestHandler(private val config: OAuth2Config) {

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

    private val exceptionHandler: ExceptionHandler = { request, error ->
        log.error("received exception when handling request: ${request.url}.", error)
        val msg = URLEncoder.encode(error.message, Charset.forName("UTF-8"))
        val errorObject: ErrorObject = when (error) {
            is OAuth2Exception -> error.errorObject
            is ParseException -> error.errorObject ?: OAuth2Error.INVALID_REQUEST.setDescription("failed to parse request: $msg")
            is GeneralException -> error.errorObject
            else -> null
        } ?: OAuth2Error.SERVER_ERROR.setDescription("unexpected exception with message: $msg")
        oauth2Error(errorObject)
    }

    val authorizationServer: Route = routes {
        exceptionHandler(exceptionHandler)
        wellKnown()
        jwks()
        authorization()
        token()
        endSession()
        userInfo(config.tokenProvider)
        debugger()
        preflight()
        get("/favicon.ico") { OAuth2HttpResponse(status = 200) }
    }

    fun enqueueTokenCallback(oAuth2TokenCallback: OAuth2TokenCallback) = tokenCallbackQueue.add(oAuth2TokenCallback)

    private fun Route.Builder.wellKnown() = get(OIDC_WELL_KNOWN, OAUTH2_WELL_KNOWN) {
        log.debug("returning well-known json data for url=${it.url}")
        json(it.toWellKnown())
    }

    private fun Route.Builder.jwks() = get(JWKS) {
        log.debug("handle jwks request on url=${it.url}")
        val issuerId = it.url.issuerId()
        val jwkSet = config.tokenProvider.publicJwkSet(issuerId)
        json(jwkSet.toJSONObject())
    }

    private fun Route.Builder.authorization() = apply {
        val authorizationCodeHandler = grantHandlers[AUTHORIZATION_CODE] as AuthorizationCodeHandler
        get(AUTHORIZATION) {
            val authRequest: AuthenticationRequest = it.asAuthenticationRequest()
            if (config.interactiveLogin || authRequest.isPrompt())
                html(loginRequestHandler.loginHtml(it))
            else {
                authenticationSuccess(authorizationCodeHandler.authorizationCodeResponse(authRequest))
            }
        }
        post(AUTHORIZATION) {
            val authRequest: AuthenticationRequest = it.asAuthenticationRequest()
            val login: Login = loginRequestHandler.loginSubmit(it)
            authenticationSuccess(authorizationCodeHandler.authorizationCodeResponse(authRequest, login))
        }
    }

    private fun Route.Builder.endSession() = any(END_SESSION) {
        log.debug("handle end session request $it")
        val postLogoutRedirectUri = it.url.queryParameter("post_logout_redirect_uri")
        postLogoutRedirectUri?.let {
            redirect(postLogoutRedirectUri)
        } ?: html("logged out")
    }

    private fun Route.Builder.token() = apply {
        get(TOKEN) {
            OAuth2HttpResponse(status = 405, body = "unsupported method")
        }
        post(TOKEN) {
            log.debug("handle token request $it")
            val grantType = it.grantType()
            val tokenCallback: OAuth2TokenCallback = tokenCallbackFromQueueOrDefault(it.url.issuerId())
            val grantHandler: GrantHandler = grantHandlers[grantType] ?: invalidGrant(grantType)
            val tokenResponse = grantHandler.tokenResponse(it, it.url.toIssuerUrl(), tokenCallback)
            json(tokenResponse)
        }
    }

    private fun Route.Builder.debugger() = apply {
        attach(debuggerRequestHandler)
    }

    private fun Route.Builder.preflight() = apply {
        // TODO

        /*
        * PREFLIGHT -> OAuth2HttpResponse(status = 200, headers = Headers.headersOf(
                    HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "*",
                    HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString(), "*",
                    HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS.toString(), "*"
                ))
        * */
    }

    private fun tokenCallbackFromQueueOrDefault(issuerId: String): OAuth2TokenCallback =
        when (issuerId) {
            tokenCallbackQueue.peek()?.issuerId() -> tokenCallbackQueue.take()
            else -> {
                config.tokenCallbacks.firstOrNull { it.issuerId() == issuerId } ?: DefaultOAuth2TokenCallback(issuerId = issuerId)
            }
        }
}
