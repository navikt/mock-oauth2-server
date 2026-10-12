package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

typealias RefreshToken = String
typealias Nonce = String

internal data class StoredCallback(
    val callback: OAuth2TokenCallback,
    val extraMatchParams: Map<String, String> = emptyMap(),
)

internal data class RefreshTokenManager(
    private val cache: MutableMap<RefreshToken, StoredCallback> = ConcurrentHashMap(),
) {
    operator fun get(refreshToken: RefreshToken) = cache[refreshToken]

    fun remove(refreshToken: RefreshToken) = cache.remove(refreshToken)

    fun refreshToken(
        tokenCallback: OAuth2TokenCallback,
        nonce: Nonce? = null,
        extraMatchParams: Map<String, String> = emptyMap(),
    ): RefreshToken {
        val jti = UUID.randomUUID().toString()
        // added for compatibility with keycloak js client which expects a jwt with nonce
        val refreshToken = nonce?.let { plainJWT(jti, nonce) } ?: jti
        cache[refreshToken] = StoredCallback(tokenCallback, extraMatchParams)
        return refreshToken
    }

    fun rotate(
        refreshToken: RefreshToken,
        fallbackTokenCallback: OAuth2TokenCallback,
        extraMatchParams: Map<String, String> = emptyMap(),
    ): RefreshToken {
        val stored = cache.remove(refreshToken)
        val callback = stored?.callback ?: fallbackTokenCallback
        val params = stored?.extraMatchParams ?: extraMatchParams
        return refreshToken(callback, null, params)
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
