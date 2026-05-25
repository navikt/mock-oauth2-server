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
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

private val log = KotlinLogging.logger {}
private val jsonMapper: ObjectMapper = jacksonObjectMapper()

internal class AuthorizationCodeHandler(
    private val tokenProvider: OAuth2TokenProvider,
    private val refreshTokenManager: RefreshTokenManager,
) : GrantHandler {
    private val codeToAuthRequestCache: MutableMap<AuthorizationCode, AuthenticationRequest> = ConcurrentHashMap()
    private val codeToLoginCache: MutableMap<AuthorizationCode, Login> = ConcurrentHashMap()

    fun authorizationCodeResponse(
        authenticationRequest: AuthenticationRequest,
        login: Login? = null,
    ): AuthenticationSuccessResponse {
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

            else -> {
                throw OAuth2Exception(
                    OAuth2Error.INVALID_GRANT,
                    "hybrid og implicit flow not supported (yet).",
                )
            }
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

        val authenticationRequest =
            codeToAuthRequestCache.remove(code)
                ?: throw OAuth2Exception(
                    OAuth2Error.INVALID_GRANT.setDescription("unknown or already-used authorization code"),
                    "unknown or already-used authorization code",
                )

        try {
            authenticationRequest.verifyPkce(tokenRequest)
        } catch (e: OAuth2Exception) {
            codeToLoginCache.remove(code)
            throw e
        }

        val scope: String? = tokenRequest.scope?.toString()
        val nonce: String? = authenticationRequest.nonce?.value
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

    private fun getLoginTokenCallbackOrDefault(
        code: AuthorizationCode,
        oAuth2TokenCallback: OAuth2TokenCallback,
    ): OAuth2TokenCallback =
        takeLoginFromCache(code)?.let {
            LoginOAuth2TokenCallback(it, oAuth2TokenCallback)
        } ?: oAuth2TokenCallback

    private fun takeLoginFromCache(code: AuthorizationCode): Login? = codeToLoginCache.remove(code)

    private class LoginOAuth2TokenCallback(
        val login: Login,
        val oAuth2TokenCallback: OAuth2TokenCallback,
    ) : OAuth2TokenCallback {
        private val resolvedDelegate: OAuth2TokenCallback =
            when (oAuth2TokenCallback) {
                is RequestMappingTokenCallback ->
                    oAuth2TokenCallback.withExtraMatchParams(mapOf(RequestMappingTokenCallback.SUBJECT_PARAM to login.username))
                else -> oAuth2TokenCallback
            }

        private val subjectResolver: (TokenRequest) -> String =
            when (oAuth2TokenCallback) {
                is RequestMappingTokenCallback -> { req -> resolvedDelegate.subject(req) ?: login.username }
                else -> { _ -> login.username }
            }

        override fun issuerId(): String = resolvedDelegate.issuerId()

        override fun subject(tokenRequest: TokenRequest): String = subjectResolver(tokenRequest)

        override fun typeHeader(tokenRequest: TokenRequest): String = resolvedDelegate.typeHeader(tokenRequest)

        override fun audience(tokenRequest: TokenRequest): List<String> = resolvedDelegate.audience(tokenRequest)

        override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> =
            resolvedDelegate.addClaims(tokenRequest).toMutableMap().apply {
                // Claim precedence: mapping/callback claims win over login-page claims.
                // login.claims can add new claims but cannot overwrite claims already set by the mapping.
                login.claims?.let {
                    try {
                        jsonMapper
                            .readTree(it)
                            .properties()
                            .forEach { field ->
                                putIfAbsent(field.key, jsonMapper.readValue(field.value.toString()))
                            }
                    } catch (exception: JsonProcessingException) {
                        log.warn("claims value $it could not be processed as JSON, details: ${exception.message}")
                    }
                }
            }

        override fun tokenExpiry(): Long = resolvedDelegate.tokenExpiry()
    }
}
