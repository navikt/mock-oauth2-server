package no.nav.security.mock.oauth2.e2e

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.ParsedTokenResponse
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.token.RequestMapping
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class IdTokenRequiredClaimsIntegrationTest {
    private val client = client()

    private val configWithoutSub =
        OAuth2Config(
            tokenCallbacks =
                setOf(
                    RequestMappingTokenCallback(
                        issuerId = "default",
                        requestMappings = listOf(RequestMapping(requestParam = "client_id", match = "*", claims = mapOf("foo" to "bar"))),
                    ),
                ),
        )

    @Test
    fun `id_token should contain sub when the request mapping does not set it`() {
        withMockOAuth2Server(configWithoutSub) {
            authorizationCodeFlow()
                .idToken
                .shouldNotBeNull()
                .subject
                .shouldNotBeBlank()
        }
    }

    @Test
    fun `refreshed id_token should keep the sub when the request mapping does not set it`() {
        withMockOAuth2Server(configWithoutSub) {
            val tokens = authorizationCodeFlow()
            val refreshed = refresh(checkNotNull(tokens.refreshToken))

            refreshed.idToken.shouldNotBeNull().subject shouldBe tokens.idToken.shouldNotBeNull().subject
        }
    }

    @Test
    fun `id_token should keep sub from the request mapping`() {
        val config =
            OAuth2Config(
                tokenCallbacks =
                    setOf(
                        RequestMappingTokenCallback(
                            issuerId = "default",
                            requestMappings = listOf(RequestMapping(requestParam = "client_id", match = "*", claims = mapOf("sub" to "mapped"))),
                        ),
                    ),
            )
        withMockOAuth2Server(config) {
            authorizationCodeFlow().idToken.shouldNotBeNull().subject shouldBe "mapped"
        }
    }

    @Test
    fun `id_token should contain auth_time when max_age is requested`() {
        withMockOAuth2Server {
            val before = Instant.now().epochSecond
            val idToken = authorizationCodeFlow(maxAge = 0).idToken.shouldNotBeNull()
            val after = Instant.now().epochSecond

            idToken.authTime() shouldBeInRange before..after
        }
    }

    @Test
    fun `auth_time should use the token provider clock`() {
        val systemTime = Instant.now().minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS)
        withMockOAuth2Server(OAuth2Config(tokenProvider = OAuth2TokenProvider(systemTime = systemTime))) {
            val idToken = authorizationCodeFlow(maxAge = 0).idToken.shouldNotBeNull()

            idToken.authTime() shouldBe systemTime.epochSecond
            idToken.jwtClaimsSet.issueTime.toInstant() shouldBe systemTime
        }
    }

    @Test
    fun `refreshed id_token should keep the original auth_time`() {
        withMockOAuth2Server {
            val tokens = authorizationCodeFlow(maxAge = 0)
            val authTime = tokens.idToken.shouldNotBeNull().authTime()
            Thread.sleep(1100)

            refresh(checkNotNull(tokens.refreshToken)).idToken.shouldNotBeNull().authTime() shouldBe authTime
        }
    }

    @Test
    fun `id_token should not contain auth_time when max_age is not requested`() {
        withMockOAuth2Server {
            authorizationCodeFlow().idToken.shouldNotBeNull().claims["auth_time"] shouldBe null
        }
    }

    private fun MockOAuth2Server.authorizationCodeFlow(maxAge: Int? = null): ParsedTokenResponse {
        val authorizeUrl =
            authorizationEndpointUrl("default")
                .authenticationRequest()
                .newBuilder()
                .apply { if (maxAge != null) addQueryParameter("max_age", maxAge.toString()) }
                .build()
        val code =
            client
                .get(authorizeUrl)
                .headers["location"]
                ?.toHttpUrl()
                ?.queryParameter("code")
                .shouldNotBeNull()
        return client
            .tokenRequest(
                tokenEndpointUrl("default"),
                mapOf(
                    "grant_type" to GrantType.AUTHORIZATION_CODE.value,
                    "code" to code,
                    "client_id" to "client1",
                    "client_secret" to "secret",
                    "redirect_uri" to "http://mycallback",
                ),
            ).toTokenResponse()
    }

    private fun MockOAuth2Server.refresh(refreshToken: String): ParsedTokenResponse =
        client
            .tokenRequest(
                tokenEndpointUrl("default"),
                mapOf(
                    "grant_type" to GrantType.REFRESH_TOKEN.value,
                    "refresh_token" to refreshToken,
                    "client_id" to "client1",
                    "client_secret" to "secret",
                ),
            ).toTokenResponse()

    private fun SignedJWT.authTime(): Long = jwtClaimsSet.getDateClaim("auth_time").toInstant().epochSecond
}
