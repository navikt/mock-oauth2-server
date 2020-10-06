package no.nav.security.mock.oauth2.grant

import java.util.UUID
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback

typealias RefreshToken = String

data class RefreshTokenManager(
    val cache: MutableMap<RefreshToken, OAuth2TokenCallback> = HashMap()
) {
    fun refreshToken(tokenCallback: OAuth2TokenCallback): RefreshToken {
        val refreshToken = UUID.randomUUID().toString()
        cache[refreshToken] = tokenCallback
        return refreshToken
    }
}
