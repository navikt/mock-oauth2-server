package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

typealias RefreshToken = String
typealias Nonce = String

internal data class StoredRefreshToken(
    val callback: OAuth2TokenCallback,
    val authRequestParams: Map<String, String>,
    val nonce: Nonce? = null,
)

private const val DEFAULT_MAX_STORED_AUTH_REQUEST_PARAMS = 20
private const val DEFAULT_MAX_STORED_AUTH_REQUEST_PARAM_VALUE_LENGTH = 512
private const val DEFAULT_MAX_STORED_AUTH_REQUEST_PARAMS_TOTAL_LENGTH = 4096
private val DEFAULT_AUTH_REQUEST_PARAMS_EXCLUDED_FROM_STORAGE = setOf("claims", "request", "client_assertion")

data class AuthRequestParamsStoragePolicy
    @JvmOverloads
    constructor(
        val maxStoredParams: Int = DEFAULT_MAX_STORED_AUTH_REQUEST_PARAMS,
        val maxValueLength: Int = DEFAULT_MAX_STORED_AUTH_REQUEST_PARAM_VALUE_LENGTH,
        val maxTotalLength: Int = DEFAULT_MAX_STORED_AUTH_REQUEST_PARAMS_TOTAL_LENGTH,
        val excludedKeys: Set<String> = DEFAULT_AUTH_REQUEST_PARAMS_EXCLUDED_FROM_STORAGE,
    ) {
        init {
            require(maxStoredParams >= 0) { "maxStoredParams must be >= 0" }
            require(maxValueLength >= 0) { "maxValueLength must be >= 0" }
            require(maxTotalLength > 0) { "maxTotalLength must be > 0" }
        }
    }

internal data class RefreshTokenManager(
    private val cache: MutableMap<RefreshToken, StoredRefreshToken> = ConcurrentHashMap(),
    private val authRequestParamsStoragePolicy: AuthRequestParamsStoragePolicy = AuthRequestParamsStoragePolicy(),
) {
    operator fun get(refreshToken: RefreshToken) = cache[refreshToken]

    fun remove(refreshToken: RefreshToken) = cache.remove(refreshToken)

    fun refreshToken(
        tokenCallback: OAuth2TokenCallback,
        nonce: Nonce? = null,
        authRequestParams: Map<String, String> = emptyMap(),
    ): RefreshToken =
        refreshTokenInternal(
            tokenCallback = tokenCallback,
            nonce = nonce,
            sanitizedAuthRequestParams = sanitizeAuthRequestParams(authRequestParams),
        )

    private fun refreshTokenInternal(
        tokenCallback: OAuth2TokenCallback,
        nonce: Nonce? = null,
        sanitizedAuthRequestParams: Map<String, String>,
    ): RefreshToken {
        val jti = UUID.randomUUID().toString()
        // added for compatibility with keycloak js client which expects a jwt with nonce
        val refreshToken = nonce?.let { plainJWT(jti, nonce) } ?: jti
        cache[refreshToken] = StoredRefreshToken(tokenCallback, sanitizedAuthRequestParams, nonce)
        return refreshToken
    }

    fun rotate(
        refreshToken: RefreshToken,
        fallbackTokenCallback: OAuth2TokenCallback,
        authRequestParams: Map<String, String> = emptyMap(),
        callbackOverride: OAuth2TokenCallback? = null,
    ): RefreshToken {
        val storedWithSanitizedParams =
            cache.remove(refreshToken)
                ?: StoredRefreshToken(fallbackTokenCallback, sanitizeAuthRequestParams(authRequestParams))
        return refreshTokenInternal(
            tokenCallback = callbackOverride ?: storedWithSanitizedParams.callback,
            nonce = storedWithSanitizedParams.nonce,
            sanitizedAuthRequestParams = storedWithSanitizedParams.authRequestParams,
        )
    }

    private fun sanitizeAuthRequestParams(authRequestParams: Map<String, String>): Map<String, String> {
        var totalLength = 0
        val sanitized = linkedMapOf<String, String>()

        for ((key, value) in authRequestParams) {
            if (sanitized.size >= authRequestParamsStoragePolicy.maxStoredParams) break
            if (totalLength >= authRequestParamsStoragePolicy.maxTotalLength) break
            if (key in authRequestParamsStoragePolicy.excludedKeys) continue

            val boundedValue = value.take(authRequestParamsStoragePolicy.maxValueLength)
            val nextLength = totalLength + key.length + boundedValue.length
            if (nextLength > authRequestParamsStoragePolicy.maxTotalLength) continue

            sanitized[key] = boundedValue
            totalLength = nextLength

            if (sanitized.size >= authRequestParamsStoragePolicy.maxStoredParams) break
            if (totalLength >= authRequestParamsStoragePolicy.maxTotalLength) break
        }

        return sanitized
    }

    private fun plainJWT(
        jti: String,
        nonce: String?,
    ): String =
        PlainJWT(
            JWTClaimsSet.parse(
                mapOf(
                    "jti" to jti,
                    "nonce" to nonce,
                ),
            ),
        ).serialize()
}
