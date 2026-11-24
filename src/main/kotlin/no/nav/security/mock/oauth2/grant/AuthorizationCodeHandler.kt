package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.authorizationCode
import no.nav.security.mock.oauth2.extensions.clientIdAsString
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.extensions.issuerId
import no.nav.security.mock.oauth2.extensions.verifyPkce
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.login.Login
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback
import no.nav.security.mock.oauth2.unsupportedResponseType
import okhttp3.HttpUrl
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

private val log = KotlinLogging.logger {}
private val jsonMapper: ObjectMapper = jacksonObjectMapper()

internal data class IssuedAuthorizationCode(
    val authenticationRequest: AuthenticationRequest,
    val issuerId: String,
    val issuedAt: Instant,
    val login: Login?,
)

internal class AuthorizationCodeHandler(
    private val tokenProvider: OAuth2TokenProvider,
    private val refreshTokenManager: RefreshTokenManager,
    private val enqueuedCallbackSupplier: ((issuerId: String) -> OAuth2TokenCallback?)? = null,
    // Must keep insertion order: the first key is the oldest code.
    private val codes: MutableMap<AuthorizationCode, IssuedAuthorizationCode> = LinkedHashMap(),
    // Null disables the limit.
    private val codeLimit: Int? = OAuth2Config.DEFAULT_AUTHORIZATION_CODE_LIMIT,
) : GrantHandler {
    private var lastPurge: Instant = Instant.MIN

    fun authorizationCodeResponse(
        issuerId: String,
        authenticationRequest: AuthenticationRequest,
        login: Login? = null,
    ): AuthenticationSuccessResponse {
        when {
            authenticationRequest.responseType.impliesCodeFlow() -> {
                val code = AuthorizationCode()
                log.debug { "issuing authorization code $code" }
                val issuedAt = tokenProvider.now()
                login?.also { log.debug { "adding user with username ${it.username} to cache" } }
                synchronized(codes) {
                    purgeExpired(issuedAt)
                    codes[code] = IssuedAuthorizationCode(authenticationRequest, issuerId, issuedAt, login)
                    evictOldest()
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
                unsupportedResponseType("only response_type=code is supported")
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
        log.debug { "issuing token for code=$code" }

        // Removing the code first makes it single-use, also when the request below is rejected.
        val issued =
            synchronized(codes) { codes.remove(code) }
                ?: throw OAuth2Exception(
                    OAuth2Error.INVALID_GRANT.setDescription("unknown or already-used authorization code"),
                    "unknown or already-used authorization code",
                )
        if (issued.isExpired(tokenProvider.now())) {
            invalidGrant("authorization code has expired")
        }
        val authenticationRequest = issued.authenticationRequest
        val authTime = issued.issuedAt.takeIf { authenticationRequest.maxAge >= 0 }

        val issuerId = request.url.issuerId()
        if (issued.issuerId != issuerId) {
            throw OAuth2Exception(
                OAuth2Error.INVALID_GRANT.setDescription("authorization code was issued by a different issuer"),
                "authorization code issuer mismatch",
            )
        }

        verifyClientAndRedirectUri(authenticationRequest, tokenRequest)
        authenticationRequest.verifyPkce(tokenRequest)

        val scope: String? = tokenRequest.scope?.toString()
        val nonce: String? = authenticationRequest.nonce?.value
        val tokenCallback = enqueuedCallbackSupplier?.invoke(issuerId) ?: oAuth2TokenCallback
        val loginTokenCallbackOrDefault = issued.login?.let { LoginOAuth2TokenCallback(it, tokenCallback) } ?: tokenCallback
        val idTokenSubject = loginTokenCallbackOrDefault.subject(tokenRequest) ?: UUID.randomUUID().toString()
        val idToken: SignedJWT = tokenProvider.idToken(tokenRequest, issuerUrl, loginTokenCallbackOrDefault, nonce, idTokenSubject, authTime)
        val accessToken: SignedJWT = tokenProvider.accessToken(tokenRequest, issuerUrl, loginTokenCallbackOrDefault, nonce)
        val refreshToken: RefreshToken =
            refreshTokenManager.refreshToken(
                RefreshTokenState(loginTokenCallbackOrDefault, tokenRequest.clientIdAsString(), idTokenSubject, authTime, authenticationRequest.scope),
                nonce,
            )

        return OAuth2TokenResponse(
            tokenType = "Bearer",
            idToken = idToken.serialize(),
            accessToken = accessToken.serialize(),
            refreshToken = refreshToken,
            expiresIn = accessToken.expiresIn(tokenProvider.now()),
            scope = scope,
        )
    }

    private fun verifyClientAndRedirectUri(
        authenticationRequest: AuthenticationRequest,
        tokenRequest: TokenRequest,
    ) {
        val tokenClientId = tokenRequest.clientAuthentication?.clientID ?: tokenRequest.clientID
        if (tokenClientId != authenticationRequest.clientID) {
            invalidGrant("authorization code was issued to a different client")
        }
        val originalRedirectUri = authenticationRequest.redirectionURI
        if (originalRedirectUri != null) {
            val grant = tokenRequest.authorizationGrant as AuthorizationCodeGrant
            if (grant.redirectionURI?.toString() != originalRedirectUri.toString()) {
                invalidGrant("redirect_uri does not match the authorization request")
            }
        }
    }

    private fun invalidGrant(message: String): Nothing = throw OAuth2Exception(OAuth2Error.INVALID_GRANT.setDescription(message), message)

    private fun IssuedAuthorizationCode.isExpired(at: Instant): Boolean = Duration.between(issuedAt, at) >= CODE_LIFETIME

    // Runs at most once per PURGE_INTERVAL, so issuing a code stays cheap with many codes pending.
    private fun purgeExpired(at: Instant) {
        if (Duration.between(lastPurge, at) < PURGE_INTERVAL) {
            return
        }
        lastPurge = at
        codes.values.removeIf { it.isExpired(at) }
    }

    // Bounds memory also when the token provider's clock is frozen and no code ever expires.
    private fun evictOldest() {
        val codeLimit = codeLimit ?: return
        val keys = codes.keys.iterator()
        while (codes.size > codeLimit && keys.hasNext()) {
            keys.next()
            keys.remove()
        }
    }

    internal companion object {
        // RFC 6749 section 4.1.2: "A maximum authorization code lifetime of 10 minutes is RECOMMENDED."
        val CODE_LIFETIME: Duration = Duration.ofMinutes(10)
        private val PURGE_INTERVAL: Duration = Duration.ofMinutes(1)
    }

    private class LoginOAuth2TokenCallback(
        val login: Login,
        val oAuth2TokenCallback: OAuth2TokenCallback,
    ) : OAuth2TokenCallback {
        private val resolvedDelegate: OAuth2TokenCallback =
            when (oAuth2TokenCallback) {
                is RequestMappingTokenCallback -> {
                    oAuth2TokenCallback.withExtraMatchParams(mapOf(RequestMappingTokenCallback.SUBJECT_PARAM to login.username))
                }

                else -> {
                    oAuth2TokenCallback
                }
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
                    } catch (exception: JacksonException) {
                        log.warn { "claims value $it could not be processed as JSON, details: ${exception.message}" }
                    }
                }
            }

        override fun tokenExpiry(): Long = resolvedDelegate.tokenExpiry()
    }
}
