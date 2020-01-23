package no.nav.security.mock.oauth2

import OAuth2Exception
import OAuth2TokenIssuer
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.GeneralException
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.TokenRequest
import mu.KotlinLogging
import no.nav.security.mock.callback.DefaultJwtCallback
import no.nav.security.mock.callback.JwtCallback
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
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

private val log = KotlinLogging.logger {}

class OAuth2Dispatcher(
    // TODO rename to OAuth2DispatcherCallback?
    private val jwtCallbacks: Set<JwtCallback> = setOf(DefaultJwtCallback()),
    private val oAuth2TokenIssuer: OAuth2TokenIssuer = OAuth2TokenIssuer()
) : Dispatcher() {


    private val jwtCallbackQueue: BlockingQueue<JwtCallback> = LinkedBlockingQueue()

    private fun takeJwtCallbackOrCreateDefault(issuerId: String): JwtCallback {
        if (jwtCallbackQueue.peek()?.issuerId() == issuerId) {
            return jwtCallbackQueue.take()
        }
        return jwtCallbacks?.firstOrNull { it.issuerId() == issuerId }
            ?: DefaultJwtCallback()
    }

    fun enqueueJwtCallback(jwtCallback: JwtCallback) = jwtCallbackQueue.add(jwtCallback)

    override fun dispatch(request: RecordedRequest): MockResponse {
        return runCatching {
            handleRequest(request)
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error ->
                log.error("received exception when handling request.", error)
                val errorObject = (error as? OAuth2Exception)
                    ?.errorObject?.appendDescription(". ${error.message}")
                    ?: (error as? GeneralException)?.errorObject
                    ?: OAuth2Error.SERVER_ERROR
                        .appendDescription(". received exception message: ${error.message}")
                MockResponse().oauth2Error(errorObject)
            }
        )
    }

    private fun handleRequest(request: RecordedRequest): MockResponse {
        log.debug("received request on url=${request.requestUrl} with headers=${request.headers}")
        val issuerId: String = request.issuerId()
        val url = checkNotNull(request.requestUrl)
        // TODO validate issuerid against registered issuers???
        return when {
            url.isWellKnownUrl() -> {
                log.debug("returning well-known json data")
                MockResponse().json(wellKnown(request))
            }
            url.isAuthorizationEndpointUrl() -> {
                log.debug("redirecting to callback with auth code")
                MockResponse().authenticationSuccess(
                    oAuth2TokenIssuer.authorizationCodeResponse(request.asAuthenticationRequest())
                )
            }
            url.isTokenEndpointUrl() -> {
                log.debug("handle token request $request")
                val jwtCallback: JwtCallback = takeJwtCallbackOrCreateDefault(issuerId)
                val tokenRequest: TokenRequest = request.asTokenRequest()
                val issuerUrl: HttpUrl = request.requestUrl?.toIssuerUrl()
                    ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "issuerid must be first segment in url path")
                when {
                    tokenRequest.grantType() == AuthorizationCodeGrant.GRANT_TYPE -> {
                        MockResponse().json(
                            oAuth2TokenIssuer.authorizationCodeTokenResponse(
                                issuerUrl = issuerUrl,
                                tokenRequest = tokenRequest,
                                jwtCallback = jwtCallback
                            )
                        )
                    }
                    else -> {
                        val msg = "grant_type ${tokenRequest.grantType()} not supported."
                        log.error(msg)
                        throw OAuth2Exception(OAuth2Error.INVALID_GRANT, msg)
                    }
                }
            }
            url.isJwksUrl() -> {
                log.debug("handle jwks request")
                MockResponse().json(oAuth2TokenIssuer.jwks().toJSONObject())
            }
            else -> {
                val msg = "path '${request.requestUrl}' not found"
                log.error(msg)
                MockResponse().setResponseCode(404).setBody(msg)
            }
        }
    }

    private fun wellKnown(request: RecordedRequest): WellKnown =
        WellKnown(
            issuer = request.requestUrl?.toIssuerUrl().toString(),
            authorizationEndpoint = request.requestUrl?.toAuthorizationEndpointUrl().toString(),
            tokenEndpoint = request.requestUrl?.toTokenEndpointUrl().toString(),
            jwksUri = request.requestUrl?.toJwksUrl().toString()
        )
}