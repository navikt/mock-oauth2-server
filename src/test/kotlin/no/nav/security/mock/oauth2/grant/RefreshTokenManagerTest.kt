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
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal class RefreshTokenManagerTest {
    @Test
    fun `refresh token should store the client it was issued to`() {
        val mgr = RefreshTokenManager()
        val token = mgr.refreshToken(state(clientId = "owner"))

        mgr.state(token)?.clientId shouldBe "owner"
    }

    @Test
    fun `only the owning client can revoke a refresh token`() {
        val mgr = RefreshTokenManager()
        val token = mgr.refreshToken(state(clientId = "owner"))
        val state = mgr.state(token)

        mgr.revoke(token, "other")
        mgr.state(token) shouldBe state

        mgr.revoke(token, "owner")
        mgr.state(token) shouldBe null
    }

    @Test
    fun `revoking an unknown refresh token should do nothing`() {
        val mgr = RefreshTokenManager()

        mgr.revoke("unknown", "client")

        mgr.state("unknown") shouldBe null
    }

    @Test
    fun `refresh token should be a jwt with nonce included if nonce is not null (for keycloak compatibility)`() {
        val mgr = RefreshTokenManager()

        mgr.refreshToken(state(), "nonce123").asClue {
            val claims = PlainJWT.parse(it).jwtClaimsSet.claims

            claims["nonce"] shouldBe "nonce123"
            claims["jti"] shouldNotBe null
        }
    }

    @Test
    fun `tokencallback should be available in cache for specific refresh token`() {
        val mgr = RefreshTokenManager()
        val tokenCallback = DefaultOAuth2TokenCallback()

        val refreshToken = mgr.refreshToken(state(tokenCallback))
        mgr[refreshToken] shouldBe tokenCallback
        val refreshToken2 = mgr.refreshToken(state(tokenCallback), "nonce123")
        mgr[refreshToken2] shouldBe tokenCallback
    }

    @Test
    fun `rotated refresh token should keep the client and granted scope`() {
        val mgr = RefreshTokenManager()
        val grantedScope = Scope("openid", "profile")
        val refreshToken = mgr.refreshToken(state(clientId = "owner", grantedScope = grantedScope))

        val rotated = mgr.rotate(refreshToken)

        mgr.state(refreshToken) shouldBe null
        mgr.state(rotated)?.clientId shouldBe "owner"
        mgr.state(rotated)?.grantedScope shouldBe grantedScope
    }

    @Test
    fun `rotating a refresh token that was already rotated should fail with invalid_grant`() {
        val mgr = RefreshTokenManager()
        val refreshToken = mgr.refreshToken(state(grantedScope = Scope("openid")))
        mgr.rotate(refreshToken)

        shouldThrow<OAuth2Exception> { mgr.rotate(refreshToken) }.errorObject?.code shouldBe OAuth2Error.INVALID_GRANT.code
    }

    @Test
    fun `refresh token should be rejected with invalid_grant once it has expired`() {
        var now = Instant.parse("2026-01-01T00:00:00Z")
        val mgr = RefreshTokenManager(expiry = Duration.ofHours(24), now = { now })
        val token = mgr.refreshToken(state())

        now = now.plus(Duration.ofHours(24)).minusSeconds(1)
        mgr.state(token) shouldNotBe null

        now = now.plusSeconds(1)
        shouldThrow<OAuth2Exception> { mgr.state(token) }.errorObject?.code shouldBe OAuth2Error.INVALID_GRANT.code
        shouldThrow<OAuth2Exception> { mgr.rotate(token) }.errorObject?.code shouldBe OAuth2Error.INVALID_GRANT.code
    }

    @Test
    fun `rotated refresh token should get a full lifetime of its own`() {
        var now = Instant.parse("2026-01-01T00:00:00Z")
        val mgr = RefreshTokenManager(expiry = Duration.ofHours(24), now = { now })
        val token = mgr.refreshToken(state())

        now = now.plus(Duration.ofHours(23))
        val rotated = mgr.rotate(token)
        now = now.plus(Duration.ofHours(23))

        mgr.state(rotated) shouldNotBe null
    }

    @Test
    fun `a very long expiry should not overflow`() {
        var now = Instant.parse("2026-01-01T00:00:00Z")
        val mgr = RefreshTokenManager(expiry = Duration.ofSeconds(Long.MAX_VALUE), now = { now })
        val token = mgr.refreshToken(state())

        now = now.plus(Duration.ofDays(1000))

        mgr.state(token) shouldNotBe null
        mgr.refreshToken(state()) shouldNotBe null
    }

    @Test
    fun `refresh token should not expire when expiry is disabled`() {
        var now = Instant.parse("2026-01-01T00:00:00Z")
        val mgr = RefreshTokenManager(expiry = null, now = { now })
        val token = mgr.refreshToken(state())

        now = now.plus(Duration.ofDays(1000))

        mgr.state(token) shouldNotBe null
    }

    @Test
    fun `expired refresh tokens should be removed when a new one is issued`() {
        var now = Instant.parse("2026-01-01T00:00:00Z")
        val cache = LinkedHashMap<RefreshToken, RefreshTokenState>()
        val mgr = RefreshTokenManager(cache, expiry = Duration.ofHours(24), now = { now })
        repeat(3) { mgr.refreshToken(state()) }

        now = now.plus(Duration.ofHours(24))
        val fresh = mgr.refreshToken(state())

        cache.keys shouldBe setOf(fresh)
    }

    @Test
    fun `oldest refresh token should be evicted when the limit is exceeded`() {
        val mgr = RefreshTokenManager(limit = 2)
        val first = mgr.refreshToken(state())
        val second = mgr.refreshToken(state())
        val third = mgr.refreshToken(state())

        mgr.state(first) shouldBe null
        mgr.state(second) shouldNotBe null
        mgr.state(third) shouldNotBe null
    }

    @Test
    fun `refresh tokens should not be evicted when the limit is disabled`() {
        val mgr = RefreshTokenManager(limit = null)
        val tokens = (1..1000).map { mgr.refreshToken(state()) }

        tokens.forEach { mgr.state(it) shouldNotBe null }
    }

    private fun state(
        tokenCallback: OAuth2TokenCallback = DefaultOAuth2TokenCallback(),
        clientId: String = "client",
        grantedScope: Scope? = null,
    ) = RefreshTokenState(tokenCallback, clientId, grantedScope = grantedScope)
}
