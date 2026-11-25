package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.pkce.CodeChallenge
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.authorizationCode
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.post
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.Response
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class CodeAndRefreshTokenExpiryIntegrationTest {
    private val client = client()
    private var now: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private fun config(
        refreshTokenExpiry: Long = OAuth2Config.DEFAULT_REFRESH_TOKEN_EXPIRY,
        refreshTokenLimit: Int = OAuth2Config.DEFAULT_REFRESH_TOKEN_LIMIT,
        authorizationCodeLimit: Int = OAuth2Config.DEFAULT_AUTHORIZATION_CODE_LIMIT,
    ) = OAuth2Config(
        tokenProvider = OAuth2TokenProvider(timeProvider = { now }),
        refreshTokenExpiry = refreshTokenExpiry,
        refreshTokenLimit = refreshTokenLimit,
        authorizationCodeLimit = authorizationCodeLimit,
    )

    @Test
    fun `authorization code should be accepted just before it expires`() {
        withMockOAuth2Server(config()) {
            val code = authorizationCode()
            now = now.plus(Duration.ofMinutes(10)).minusSeconds(1)

            redeem(code).status shouldBe 200
        }
    }

    @Test
    fun `authorization code should be rejected with invalid_grant after 10 minutes`() {
        withMockOAuth2Server(config()) {
            val code = authorizationCode()
            now = now.plus(Duration.ofMinutes(10))

            redeem(code).shouldBeInvalidGrant()
        }
    }

    @Test
    fun `expired authorization code should be consumed`() {
        withMockOAuth2Server(config()) {
            val code = authorizationCode()
            now = now.plus(Duration.ofMinutes(10))
            redeem(code).shouldBeInvalidGrant()

            now = now.minus(Duration.ofMinutes(10))
            redeem(code).shouldBeInvalidGrant()
        }
    }

    @Test
    fun `expired authorization code from a PKCE flow should be rejected with invalid_grant`() {
        withMockOAuth2Server(config()) {
            val verifier = CodeVerifier()
            val code =
                client
                    .get(
                        authorizationEndpointUrl("default")
                            .authenticationRequest()
                            .newBuilder()
                            .addQueryParameter("code_challenge", CodeChallenge.compute(CodeChallengeMethod.S256, verifier).value)
                            .addQueryParameter("code_challenge_method", "S256")
                            .build(),
                    ).authorizationCode
                    .shouldNotBeNull()
            now = now.plus(Duration.ofMinutes(10))

            redeem(code, mapOf("code_verifier" to verifier.value)).shouldBeInvalidGrant()
        }
    }

    @Test
    fun `revoking an expired refresh token should return 200`() {
        withMockOAuth2Server(config()) {
            val refreshToken = redeem(authorizationCode()).toTokenResponse().refreshToken.shouldNotBeNull()
            now = now.plus(Duration.ofHours(24))

            client
                .post(
                    url("/default/revoke"),
                    mapOf("client_id" to "client1", "client_secret" to "secret", "token" to refreshToken),
                ).use {
                    it.code shouldBe 200
                    it.body.string() shouldBe ""
                }
        }
    }

    @Test
    fun `oldest authorization code should be rejected with invalid_grant once authorizationCodeLimit is exceeded`() {
        withMockOAuth2Server(config(authorizationCodeLimit = 1)) {
            val oldest = authorizationCode()
            val newest = authorizationCode()

            redeem(oldest).shouldBeInvalidGrant()
            redeem(newest).status shouldBe 200
        }
    }

    @Test
    fun `refresh token should be accepted just before it expires`() {
        withMockOAuth2Server(config()) {
            val refreshToken = redeem(authorizationCode()).toTokenResponse().refreshToken.shouldNotBeNull()
            now = now.plus(Duration.ofHours(24)).minusSeconds(1)

            refresh(refreshToken).status shouldBe 200
        }
    }

    @Test
    fun `refresh token should be rejected with invalid_grant after 24 hours by default`() {
        withMockOAuth2Server(config()) {
            val refreshToken = redeem(authorizationCode()).toTokenResponse().refreshToken.shouldNotBeNull()
            now = now.plus(Duration.ofHours(24))

            refresh(refreshToken).shouldBeInvalidGrant()
        }
    }

    @Test
    fun `refresh token in use should stay valid past its original expiry`() {
        withMockOAuth2Server(config(refreshTokenExpiry = 6)) {
            val refreshToken = redeem(authorizationCode()).toTokenResponse().refreshToken.shouldNotBeNull()
            repeat(5) {
                now = now.plusSeconds(2)
                refresh(refreshToken).status shouldBe 200
            }

            now = now.plusSeconds(6)
            refresh(refreshToken).shouldBeInvalidGrant()
        }
    }

    @Test
    fun `refresh rejected for another client should not restart the expiry`() {
        withMockOAuth2Server(config(refreshTokenExpiry = 6)) {
            val refreshToken = redeem(authorizationCode()).toTokenResponse().refreshToken.shouldNotBeNull()
            now = now.plusSeconds(4)
            refresh(refreshToken, "client2" to "secret").shouldBeInvalidGrant()

            now = now.plusSeconds(2)
            refresh(refreshToken).shouldBeInvalidGrant()
        }
    }

    @Test
    fun `refresh token should not expire when refreshTokenExpiry is 0`() {
        withMockOAuth2Server(config(refreshTokenExpiry = 0)) {
            val refreshToken = redeem(authorizationCode()).toTokenResponse().refreshToken.shouldNotBeNull()
            now = now.plus(Duration.ofDays(1000))

            refresh(refreshToken).status shouldBe 200
        }
    }

    @Test
    fun `oldest refresh token should be rejected with invalid_grant once refreshTokenLimit is exceeded`() {
        withMockOAuth2Server(config(refreshTokenLimit = 1)) {
            val oldest = redeem(authorizationCode()).toTokenResponse().refreshToken.shouldNotBeNull()
            val newest = redeem(authorizationCode()).toTokenResponse().refreshToken.shouldNotBeNull()

            refresh(oldest).shouldBeInvalidGrant()
            refresh(newest).status shouldBe 200
        }
    }

    private fun MockOAuth2Server.authorizationCode(): String =
        client.get(authorizationEndpointUrl("default").authenticationRequest()).authorizationCode.shouldNotBeNull()

    private fun MockOAuth2Server.redeem(
        code: String,
        extra: Map<String, String> = emptyMap(),
    ): Response =
        client
            .tokenRequest(
                tokenEndpointUrl("default"),
                mapOf(
                    "grant_type" to GrantType.AUTHORIZATION_CODE.value,
                    "code" to code,
                    "client_id" to "client1",
                    "client_secret" to "secret",
                    "redirect_uri" to "http://mycallback",
                ) + extra,
            )

    private fun MockOAuth2Server.refresh(
        refreshToken: String,
        clientCredentials: Pair<String, String> = "client1" to "secret",
    ): Response =
        client
            .tokenRequest(
                tokenEndpointUrl("default"),
                clientCredentials,
                mapOf("grant_type" to GrantType.REFRESH_TOKEN.value, "refresh_token" to refreshToken),
            )

    private fun Response.shouldBeInvalidGrant() =
        use {
            it.code shouldBe 400
            it.body.string() shouldContain OAuth2Error.INVALID_GRANT.code
        }

    private val Response.status: Int
        get() = use { it.code }
}
