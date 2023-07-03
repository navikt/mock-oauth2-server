package no.nav.security.mock.oauth2.grant

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.authorizationCode
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.extensions.verifyPkce
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.login.Login
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.HttpUrl
import kotlin.collections.set

private val log = KotlinLogging.logger {}
private val jsonMapper: ObjectMapper = jacksonObjectMapper()

internal class AuthorizationCodeHandler(
    private val tokenProvider: OAuth2TokenProvider,
    private val refreshTokenManager: RefreshTokenManager,
) : GrantHandler {

    private val codeToAuthRequestCache: MutableMap<AuthorizationCode, AuthenticationRequest> = HashMap()
    private val codeToLoginCache: MutableMap<AuthorizationCode, Login> = HashMap()

    fun authorizationCodeResponse(authenticationRequest: AuthenticationRequest, login: Login? = null): AuthenticationSuccessResponse {
        when {
            authenticationRequest.responseType.impliesCodeFlow() -> {
                val code = AuthorizationCode()
                log.debug("issuing authorization code $code")
                codeToAuthRequestCache[code] = authenticationRequest
                login?.also {
                    log.debug("adding user with username ${it.username} to cache")
                    codeToLoginCache[code] = login
                }
                return AuthenticationSuccessResponse(
                    authenticationRequest.redirectionURI,
                    code,
                    null,
                    null,
                    authenticationRequest.state,
                    null,
                    authenticationRequest.responseMode,
                )
            }
            else -> throw OAuth2Exception(
                OAuth2Error.INVALID_GRANT,
                "hybrid og implicit flow not supported (yet).",
            )
        }
    }

    override fun tokenResponse(
        request: OAuth2HttpRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback,
    ): OAuth2TokenResponse {
        val tokenRequest = request.asNimbusTokenRequest()
        val code = tokenRequest.authorizationCode()
        log.debug("issuing token for code=$code")
        val authenticationRequest = takeAuthenticationRequestFromCache(code)
        authenticationRequest?.verifyPkce(tokenRequest)
        val scope: String? = tokenRequest.scope?.toString()
        val nonce: String? = authenticationRequest?.nonce?.value
        val loginTokenCallbackOrDefault = getLoginTokenCallbackOrDefault(code, oAuth2TokenCallback)
        val idToken: SignedJWT = tokenProvider.idToken(tokenRequest, issuerUrl, loginTokenCallbackOrDefault, nonce)
        val accessToken: SignedJWT = tokenProvider.accessToken(tokenRequest, issuerUrl, loginTokenCallbackOrDefault, nonce)
        val refreshToken: RefreshToken = refreshTokenManager.refreshToken(loginTokenCallbackOrDefault, nonce)

        return OAuth2TokenResponse(
            tokenType = "Bearer",
            idToken = idToken.serialize(),
            accessToken = accessToken.serialize(),
            refreshToken = refreshToken,
            expiresIn = idToken.expiresIn(),
            scope = scope,
        )
    }

    private fun getLoginTokenCallbackOrDefault(code: AuthorizationCode, OAuth2TokenCallback: OAuth2TokenCallback): OAuth2TokenCallback {
        return takeLoginFromCache(code)?.let {
            LoginOAuth2TokenCallback(it, OAuth2TokenCallback)
        } ?: OAuth2TokenCallback
    }

    private fun takeLoginFromCache(code: AuthorizationCode): Login? = codeToLoginCache.remove(code)
    private fun takeAuthenticationRequestFromCache(code: AuthorizationCode): AuthenticationRequest? = codeToAuthRequestCache.remove(code)

    private class LoginOAuth2TokenCallback(val login: Login, val oAuth2TokenCallback: OAuth2TokenCallback) : OAuth2TokenCallback {
        override fun issuerId(): String = oAuth2TokenCallback.issuerId()
        override fun subject(tokenRequest: TokenRequest): String = login.username
        override fun typeHeader(tokenRequest: TokenRequest): String = oAuth2TokenCallback.typeHeader(tokenRequest)
        override fun audience(tokenRequest: TokenRequest): List<String> = oAuth2TokenCallback.audience(tokenRequest)
        override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> =
            oAuth2TokenCallback.addClaims(tokenRequest).toMutableMap().apply {
                login.claims?.let {
                    try {
                        jsonMapper.readTree(it)
                            .fields()
                            .forEach { field ->
                                put(field.key, jsonMapper.readValue(field.value.toString()))
                            }
                    } catch (exception: JsonProcessingException) {
                        log.warn("claims value $it could not be processed as JSON, details: ${exception.message}")
                    }
                }
            }

        override fun tokenExpiry(): Long = oAuth2TokenCallback.tokenExpiry()
    }
}
