package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.PlainJWT
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldContainAll
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
        mgr[refreshToken] shouldBe StoredRefreshToken(tokenCallback, emptyMap(), null)
        val refreshToken2 = mgr.refreshToken(tokenCallback, "nonce123")
        mgr[refreshToken2] shouldBe StoredRefreshToken(tokenCallback, emptyMap(), "nonce123")
    }

    @Test
    fun `refreshToken should store provided auth request params`() {
        val mgr = RefreshTokenManager()
        val tokenCallback = DefaultOAuth2TokenCallback()
        val authRequestParams = mapOf("login_hint" to "anna@example.com", "acr_values" to "high")

        val refreshToken = mgr.refreshToken(tokenCallback, authRequestParams = authRequestParams)
        mgr[refreshToken].asClue {
            it shouldNotBe null
            it!!.callback shouldBe tokenCallback
            it.authRequestParams shouldContainAll authRequestParams
            it.nonce shouldBe null
        }
    }

    @Test
    fun `refreshToken should filter and bound stored auth request params`() {
        val mgr = RefreshTokenManager()
        val tokenCallback = DefaultOAuth2TokenCallback()
        val oversized = "x".repeat(600)
        val authRequestParams =
            mapOf(
                "claims" to "{\"sensitive\":true}",
                "request" to "jwt-request-object",
                "client_assertion" to "jwt-assertion",
                "login_hint" to oversized,
            )

        val refreshToken = mgr.refreshToken(tokenCallback, authRequestParams = authRequestParams)
        mgr[refreshToken].asClue {
            it shouldNotBe null
            it!!.authRequestParams.containsKey("claims") shouldBe false
            it.authRequestParams.containsKey("request") shouldBe false
            it.authRequestParams.containsKey("client_assertion") shouldBe false
            it.authRequestParams["login_hint"]!!.length shouldBe 512
        }
    }

    @Test
    fun `rotate should preserve stored auth request params`() {
        val mgr = RefreshTokenManager()
        val tokenCallback = DefaultOAuth2TokenCallback()
        val authRequestParams = mapOf("login_hint" to "anna@example.com")

        val refreshToken = mgr.refreshToken(tokenCallback, authRequestParams = authRequestParams)
        val rotated = mgr.rotate(refreshToken, DefaultOAuth2TokenCallback(), authRequestParams = mapOf("login_hint" to "fallback@example.com"))

        mgr[rotated].asClue {
            it shouldNotBe null
            it!!.callback shouldBe tokenCallback
            it.authRequestParams shouldContainAll authRequestParams
        }
    }

    @Test
    fun `rotate should preserve nonce and keep jwt format`() {
        val mgr = RefreshTokenManager()
        val tokenCallback = DefaultOAuth2TokenCallback()
        val refreshToken = mgr.refreshToken(tokenCallback, nonce = "nonce123")

        val rotated = mgr.rotate(refreshToken, DefaultOAuth2TokenCallback())

        PlainJWT.parse(rotated).jwtClaimsSet.claims.asClue {
            it["nonce"] shouldBe "nonce123"
            it["jti"] shouldNotBe null
        }

        mgr[rotated].asClue {
            it shouldNotBe null
            it!!.nonce shouldBe "nonce123"
        }
    }

    @Test
    fun `refreshToken should respect custom auth request params storage policy`() {
        val policy =
            AuthRequestParamsStoragePolicy(
                maxStoredParams = 1,
                maxValueLength = 3,
                maxTotalLength = 20,
                excludedKeys = setOf("skip"),
            )
        val mgr = RefreshTokenManager(authRequestParamsStoragePolicy = policy)
        val tokenCallback = DefaultOAuth2TokenCallback()
        val authRequestParams =
            linkedMapOf(
                "skip" to "ignored",
                "login_hint" to "abcdef",
                "acr_values" to "high",
            )

        val refreshToken = mgr.refreshToken(tokenCallback, authRequestParams = authRequestParams)

        mgr[refreshToken].asClue {
            it shouldNotBe null
            it!!.authRequestParams shouldBe mapOf("login_hint" to "abc")
        }
    }

    @Test
    fun `auth request params storage policy should reject invalid limits`() {
        shouldThrow<IllegalArgumentException> {
            AuthRequestParamsStoragePolicy(maxStoredParams = -1)
        }.message shouldBe "maxStoredParams must be >= 0"

        shouldThrow<IllegalArgumentException> {
            AuthRequestParamsStoragePolicy(maxValueLength = -1)
        }.message shouldBe "maxValueLength must be >= 0"

        shouldThrow<IllegalArgumentException> {
            AuthRequestParamsStoragePolicy(maxTotalLength = 0)
        }.message shouldBe "maxTotalLength must be > 0"
    }
}
