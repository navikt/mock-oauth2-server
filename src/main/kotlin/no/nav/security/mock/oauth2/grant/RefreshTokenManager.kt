package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.Scope
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

typealias RefreshToken = String
typealias Nonce = String

internal data class RefreshTokenState(
    val tokenCallback: OAuth2TokenCallback,
    val idTokenSubject: String? = null,
    val authTime: Instant? = null,
    val grantedScope: Scope? = null,
    val clientId: String? = null,
)

internal data class RefreshTokenManager(
    private val cache: MutableMap<RefreshToken, RefreshTokenState> = ConcurrentHashMap(),
) {
    operator fun get(refreshToken: RefreshToken) = cache[refreshToken]?.tokenCallback

    fun state(refreshToken: RefreshToken) = cache[refreshToken]

    fun remove(refreshToken: RefreshToken) = cache.remove(refreshToken)

    fun refreshToken(
        tokenCallback: OAuth2TokenCallback,
        nonce: Nonce? = null,
        idTokenSubject: String? = null,
        authTime: Instant? = null,
        grantedScope: Scope? = null,
        clientId: String? = null,
    ): RefreshToken = refreshToken(RefreshTokenState(tokenCallback, idTokenSubject, authTime, grantedScope, clientId), nonce)

    fun rotate(refreshToken: RefreshToken): RefreshToken {
        val state =
            cache.remove(refreshToken)
                ?: throw OAuth2Exception(OAuth2Error.INVALID_GRANT.setDescription("refresh_token was already used"), "refresh_token was already used")
        return refreshToken(state, null)
    }

    private fun refreshToken(
        state: RefreshTokenState,
        nonce: Nonce?,
    ): RefreshToken {
        val jti = UUID.randomUUID().toString()
        // added for compatibility with keycloak js client which expects a jwt with nonce
        val refreshToken = nonce?.let { plainJWT(jti, nonce) } ?: jti
        cache[refreshToken] = state
        return refreshToken
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
