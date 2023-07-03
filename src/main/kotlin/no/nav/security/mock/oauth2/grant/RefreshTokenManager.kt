package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import java.util.UUID

typealias RefreshToken = String

internal data class RefreshTokenManager(
    private val cache: MutableMap<RefreshToken, OAuth2TokenCallback> = HashMap(),
) {
    operator fun get(refreshToken: RefreshToken) = cache[refreshToken]

    fun refreshToken(tokenCallback: OAuth2TokenCallback, nonce: String?): RefreshToken {
        val jti = UUID.randomUUID().toString()
        // added for compatibility with keycloak js client which expects a jwt with nonce
        val refreshToken = nonce?.let { plainJWT(jti, nonce) } ?: jti
        cache[refreshToken] = tokenCallback
        return refreshToken
    }

    private fun plainJWT(jti: String, nonce: String?): String =
        PlainJWT(
            JWTClaimsSet.parse(
                mapOf(
                    "jti" to jti,
                    "nonce" to nonce,
                ),
            ),
        ).serialize()
}
