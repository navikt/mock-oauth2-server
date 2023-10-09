package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.PlainJWT
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.jupiter.api.Test

internal class RefreshTokenManagerTest {
    @Test
    fun `refresh token should be a jwt with nonce included if nonce is not null (for keycloak compatibility)`() {
        val mgr = RefreshTokenManager()
        val tokenCallback = DefaultOAuth2TokenCallback()

        mgr.refreshToken(tokenCallback, "nonce123").asClue {
            val claims = PlainJWT.parse(it).jwtClaimsSet.claims

            claims["nonce"] shouldBe "nonce123"
            claims["jti"] shouldNotBe null
        }
    }

    @Test
    fun `tokencallback should be available in cache for specific refresh token`() {
        val mgr = RefreshTokenManager()
        val tokenCallback = DefaultOAuth2TokenCallback()

        val refreshToken = mgr.refreshToken(tokenCallback, null)
        mgr[refreshToken] shouldBe tokenCallback
        val refreshToken2 = mgr.refreshToken(tokenCallback, "nonce123")
        mgr[refreshToken2] shouldBe tokenCallback
    }
}
