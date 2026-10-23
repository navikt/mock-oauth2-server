package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.PlainJWT
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.Scope
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.OAuth2Exception
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

    @Test
    fun `rotated refresh token should keep the granted scope`() {
        val mgr = RefreshTokenManager()
        val grantedScope = Scope("openid", "profile")
        val refreshToken = mgr.refreshToken(DefaultOAuth2TokenCallback(), grantedScope = grantedScope)

        val rotated = mgr.rotate(refreshToken)

        mgr.state(refreshToken) shouldBe null
        mgr.state(rotated)?.grantedScope shouldBe grantedScope
    }

    @Test
    fun `rotating a refresh token that was already rotated should fail with invalid_grant`() {
        val mgr = RefreshTokenManager()
        val refreshToken = mgr.refreshToken(DefaultOAuth2TokenCallback(), grantedScope = Scope("openid"))
        mgr.rotate(refreshToken)

        shouldThrow<OAuth2Exception> { mgr.rotate(refreshToken) }.errorObject?.code shouldBe OAuth2Error.INVALID_GRANT.code
    }
}
