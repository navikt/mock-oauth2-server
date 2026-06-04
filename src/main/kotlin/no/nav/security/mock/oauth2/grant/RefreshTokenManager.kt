package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

typealias RefreshToken = String
typealias Nonce = String

internal data class RefreshTokenManager(
    private val cache: MutableMap<RefreshToken, Pair<OAuth2TokenCallback, Map<String, String>>> = ConcurrentHashMap(),
) {
    operator fun get(refreshToken: RefreshToken) = cache[refreshToken]

    fun remove(refreshToken: RefreshToken) = cache.remove(refreshToken)

    fun refreshToken(
        tokenCallback: OAuth2TokenCallback,
        nonce: Nonce? = null,
        authRequestParams: Map<String, String> = emptyMap(),
    ): RefreshToken {
        val jti = UUID.randomUUID().toString()
        // added for compatibility with keycloak js client which expects a jwt with nonce
        val refreshToken = nonce?.let { plainJWT(jti, nonce) } ?: jti
        cache[refreshToken] = Pair(tokenCallback, authRequestParams)
        return refreshToken
    }

    fun rotate(
        refreshToken: RefreshToken,
        fallbackTokenCallback: OAuth2TokenCallback,
        authRequestParams: Map<String, String> = emptyMap(),
    ): RefreshToken {
        val (callback, params) = cache.remove(refreshToken) ?: Pair(fallbackTokenCallback, authRequestParams)
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
