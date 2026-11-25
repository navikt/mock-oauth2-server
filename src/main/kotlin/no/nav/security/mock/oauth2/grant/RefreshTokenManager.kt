package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.Scope
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import java.time.Duration
import java.time.Instant
import java.util.UUID

typealias RefreshToken = String
typealias Nonce = String

internal data class RefreshTokenState(
    val tokenCallback: OAuth2TokenCallback,
    val clientId: String,
    val idTokenSubject: String? = null,
    val authTime: Instant? = null,
    val grantedScope: Scope? = null,
    val lastUsedAt: Instant? = null,
)

internal class RefreshTokenManager(
    private val leastRecentlyUsedFirst: LinkedHashMap<RefreshToken, RefreshTokenState> = LinkedHashMap(),
    private val expiryAfterLastUse: Duration? = Duration.ofSeconds(OAuth2Config.DEFAULT_REFRESH_TOKEN_EXPIRY),
    private val limit: Int? = OAuth2Config.DEFAULT_REFRESH_TOKEN_LIMIT,
    private val now: () -> Instant = Instant::now,
) {
    private var lastPurge: Instant = Instant.MIN

    operator fun get(refreshToken: RefreshToken) = state(refreshToken)?.tokenCallback

    fun state(refreshToken: RefreshToken): RefreshTokenState? =
        synchronized(leastRecentlyUsedFirst) {
            leastRecentlyUsedFirst[refreshToken]?.also {
                if (it.isExpired(now())) {
                    leastRecentlyUsedFirst.remove(refreshToken)
                    throw expired()
                }
            }
        }

    fun markUsed(refreshToken: RefreshToken) {
        synchronized(leastRecentlyUsedFirst) {
            val state = leastRecentlyUsedFirst.remove(refreshToken) ?: return
            leastRecentlyUsedFirst[refreshToken] = state.copy(lastUsedAt = now())
        }
    }

    fun revoke(
        refreshToken: RefreshToken,
        clientId: String,
    ) {
        synchronized(leastRecentlyUsedFirst) {
            leastRecentlyUsedFirst.computeIfPresent(refreshToken) { _, state ->
                state.takeUnless { it.clientId == clientId }
            }
        }
    }

    fun rotate(refreshToken: RefreshToken): RefreshToken {
        val state =
            synchronized(leastRecentlyUsedFirst) { leastRecentlyUsedFirst.remove(refreshToken) }
                ?: throw OAuth2Exception(OAuth2Error.INVALID_GRANT.setDescription("refresh_token was already used"), "refresh_token was already used")
        if (state.isExpired(now())) {
            throw expired()
        }
        return refreshToken(state, null)
    }

    fun refreshToken(
        state: RefreshTokenState,
        nonce: Nonce? = null,
    ): RefreshToken {
        val jti = UUID.randomUUID().toString()
        // added for compatibility with keycloak js client which expects a jwt with nonce
        val refreshToken = nonce?.let { plainJWT(jti, nonce) } ?: jti
        val issuedAt = now()
        synchronized(leastRecentlyUsedFirst) {
            purgeExpiredOncePerInterval(issuedAt)
            leastRecentlyUsedFirst[refreshToken] = state.copy(lastUsedAt = issuedAt)
            evictLeastRecentlyUsedBeyondLimit()
        }
        return refreshToken
    }

    private fun RefreshTokenState.isExpired(at: Instant): Boolean {
        val lastUsedAt = lastUsedAt ?: return false
        return expiryAfterLastUse != null && Duration.between(lastUsedAt, at) >= expiryAfterLastUse
    }

    private fun purgeExpiredOncePerInterval(at: Instant) {
        if (expiryAfterLastUse == null || Duration.between(lastPurge, at) < PURGE_INTERVAL) {
            return
        }
        leastRecentlyUsedFirst.values.removeIf { it.isExpired(at) }
        lastPurge = at
    }

    private fun evictLeastRecentlyUsedBeyondLimit() {
        val limit = limit ?: return
        val keys = leastRecentlyUsedFirst.keys.iterator()
        while (leastRecentlyUsedFirst.size > limit && keys.hasNext()) {
            keys.next()
            keys.remove()
        }
    }

    private fun expired() = OAuth2Exception(OAuth2Error.INVALID_GRANT.setDescription("refresh_token has expired"), "refresh_token has expired")

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

    private companion object {
        val PURGE_INTERVAL: Duration = Duration.ofMinutes(1)
    }
}
