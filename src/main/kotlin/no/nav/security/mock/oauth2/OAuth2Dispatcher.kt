package no.nav.security.mock.oauth2

import com.nimbusds.oauth2.sdk.ErrorObject
import com.nimbusds.oauth2.sdk.GeneralException
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.ParseException
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import mu.KotlinLogging
import no.nav.security.mock.callback.DefaultTokenCallback
import no.nav.security.mock.callback.TokenCallback
import no.nav.security.mock.extensions.asAuthenticationRequest
import no.nav.security.mock.extensions.asTokenRequest
import no.nav.security.mock.extensions.authenticationSuccess
import no.nav.security.mock.extensions.grantType
import no.nav.security.mock.extensions.isAuthorizationEndpointUrl
import no.nav.security.mock.extensions.isJwksUrl
import no.nav.security.mock.extensions.isTokenEndpointUrl
import no.nav.security.mock.extensions.isWellKnownUrl
import no.nav.security.mock.extensions.issuerId
import no.nav.security.mock.extensions.json
import no.nav.security.mock.extensions.oauth2Error
import no.nav.security.mock.extensions.toAuthorizationEndpointUrl
import no.nav.security.mock.extensions.toIssuerUrl
import no.nav.security.mock.extensions.toJwksUrl
import no.nav.security.mock.extensions.toTokenEndpointUrl
import no.nav.security.mock.oauth2.grant.AuthorizationCodeHandler
import no.nav.security.mock.oauth2.grant.ClientCredentialsGrantHandler
import no.nav.security.mock.oauth2.grant.GrantHandler
import no.nav.security.mock.oauth2.grant.JwtBearerGrantHandler
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

private val log = KotlinLogging.logger {}

// TODO: support more flows and oidc session management / logout
class OAuth2Dispatcher(
    private val tokenProvider: OAuth2TokenProvider = OAuth2TokenProvider(),
    // TODO rename to OAuth2DispatcherCallback?
    private val tokenCallbacks: Set<TokenCallback> = setOf(DefaultTokenCallback(audience = "default"))
) : Dispatcher() {

    private val tokenCallbackQueue: BlockingQueue<TokenCallback> = LinkedBlockingQueue()

    private val grantHandlers: Map<GrantType, GrantHandler> = mapOf(
        GrantType.AUTHORIZATION_CODE to AuthorizationCodeHandler(tokenProvider),
        GrantType.CLIENT_CREDENTIALS to ClientCredentialsGrantHandler(tokenProvider),
        GrantType.JWT_BEARER to JwtBearerGrantHandler(tokenProvider)
    )

    private fun takeJwtCallbackOrCreateDefault(issuerId: String): TokenCallback {
        if (tokenCallbackQueue.peek()?.issuerId() == issuerId) {
            return tokenCallbackQueue.take()
        }
        return tokenCallbacks.firstOrNull { it.issuerId() == issuerId }
            ?: DefaultTokenCallback(issuerId = issuerId)
    }

    fun enqueueJwtCallback(tokenCallback: TokenCallback) = tokenCallbackQueue.add(tokenCallback)

    override fun dispatch(request: RecordedRequest): MockResponse {
        return runCatching {
            handleRequest(request)
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error -> handleException(error) }
        )
    }

    private fun handleRequest(request: RecordedRequest): MockResponse {
        log.debug("received request on url=${request.requestUrl} with headers=${request.headers}")
        val issuerId: String = request.issuerId()
        val url = checkNotNull(request.requestUrl)

        return when {
            url.isWellKnownUrl() -> {
                log.debug("returning well-known json data for url=$url")
                MockResponse().json(wellKnown(request))
            }
            url.isAuthorizationEndpointUrl() -> {
                log.debug("redirecting to callback with auth code")
                val authRequest: AuthenticationRequest = request.asAuthenticationRequest()

                when {
                    authRequest.responseType.impliesCodeFlow() -> {
                        MockResponse().authenticationSuccess(
                            (grantHandlers[GrantType.AUTHORIZATION_CODE] as AuthorizationCodeHandler)
                                .authorizationCodeResponse(request.asAuthenticationRequest())
                        )
                    }
                    else -> throw OAuth2Exception(
                        OAuth2Error.INVALID_GRANT, "hybrid og implicit flow not supported (yet)."
                    )
                }
            }
            url.isTokenEndpointUrl() -> {
                log.debug("handle token request $request")
                val tokenCallback: TokenCallback = takeJwtCallbackOrCreateDefault(issuerId)
                val tokenRequest: TokenRequest = request.asTokenRequest().also {
                    log.debug("query in tokenreq: ${it.toHTTPRequest().query}")
                }
                val issuerUrl: HttpUrl = issuerUrl(request)
                MockResponse().json(
                    grantHandler(tokenRequest.grantType()).tokenResponse(tokenRequest, issuerUrl, tokenCallback)
                )
            }
            url.isJwksUrl() -> {
                log.debug("handle jwks request")
                MockResponse().json(tokenProvider.publicJwkSet().toJSONObject())
            }
            else -> {
                val msg = "path '${request.requestUrl}' not found"
                log.error(msg)
                MockResponse().setResponseCode(404).setBody(msg)
            }
        }
    }

    private fun handleException(error: Throwable): MockResponse {
        log.error("received exception when handling request.", error)
        val errorObject: ErrorObject = when (error) {
            is OAuth2Exception -> error.errorObject
            is ParseException -> error.errorObject
                ?: OAuth2Error.INVALID_REQUEST
                    .appendDescription(". received exception message: ${error.message}")
            is GeneralException -> error.errorObject
            else -> null
        } ?: OAuth2Error.SERVER_ERROR
            .appendDescription(". received exception message: ${error.message}")

        return MockResponse().oauth2Error(errorObject)
    }

    private fun wellKnown(request: RecordedRequest): WellKnown =
        WellKnown(
            issuer = request.requestUrl?.toIssuerUrl().toString(),
            authorizationEndpoint = request.requestUrl?.toAuthorizationEndpointUrl().toString(),
            tokenEndpoint = request.requestUrl?.toTokenEndpointUrl().toString(),
            jwksUri = request.requestUrl?.toJwksUrl().toString()
        )

    private fun issuerUrl(request: RecordedRequest): HttpUrl =
        request.requestUrl?.toIssuerUrl()
            ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "issuerid must be first segment in url path")

    private fun grantHandler(grantType: GrantType): GrantHandler =
        grantHandlers[grantType] ?: throw OAuth2Exception(
            OAuth2Error.INVALID_GRANT, "grant_type $grantType not supported."
        )
}