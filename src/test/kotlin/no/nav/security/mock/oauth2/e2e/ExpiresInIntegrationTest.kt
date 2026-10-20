package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.assertions.asClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.ClientAssertionType
import no.nav.security.mock.oauth2.testutils.SubjectTokenType
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.authorizationCode
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.clientAssertion
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.withMockOAuth2Server
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class ExpiresInIntegrationTest {
    private val client = client()
    private val systemTime = Instant.now().minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS)
    private val config = OAuth2Config(tokenProvider = OAuth2TokenProvider(systemTime = systemTime))

    @Test
    fun `expires_in should use the token provider clock for client_credentials`() {
        withMockOAuth2Server(config) {
            client
                .tokenRequest(
                    tokenEndpointUrl("default"),
                    "client1" to "secret",
                    mapOf("grant_type" to GrantType.CLIENT_CREDENTIALS.value, "scope" to "scope1"),
                ).toTokenResponse()
                .asClue {
                    it.status shouldBe 200
                    it.expiresIn shouldBe 3600
                }
        }
    }

    @Test
    fun `expires_in should be the full lifetime when the clock has sub-second precision`() {
        val untruncated = Instant.parse("2020-01-21T00:00:00.123456789Z")
        withMockOAuth2Server(OAuth2Config(tokenProvider = OAuth2TokenProvider(systemTime = untruncated))) {
            client
                .tokenRequest(
                    tokenEndpointUrl("default"),
                    "client1" to "secret",
                    mapOf("grant_type" to GrantType.CLIENT_CREDENTIALS.value, "scope" to "scope1"),
                ).toTokenResponse()
                .asClue {
                    it.status shouldBe 200
                    it.expiresIn shouldBe 3600
                }
        }
    }

    @Test
    fun `expires_in should use the token provider clock for authorization_code`() {
        withMockOAuth2Server(config) {
            val code = client.get(authorizationEndpointUrl("default").authenticationRequest()).authorizationCode.shouldNotBeNull()
            client
                .tokenRequest(
                    tokenEndpointUrl("default"),
                    mapOf(
                        "grant_type" to GrantType.AUTHORIZATION_CODE.value,
                        "code" to code,
                        "client_id" to "client1",
                        "client_secret" to "secret",
                        "redirect_uri" to "http://defaultRedirectUri",
                    ),
                ).toTokenResponse()
                .asClue {
                    it.status shouldBe 200
                    it.expiresIn shouldBe 3600
                }
        }
    }

    @Test
    fun `expires_in should use the token provider clock for refresh_token`() {
        withMockOAuth2Server(config) {
            val code = client.get(authorizationEndpointUrl("default").authenticationRequest()).authorizationCode.shouldNotBeNull()
            val refreshToken =
                client
                    .tokenRequest(
                        tokenEndpointUrl("default"),
                        mapOf(
                            "grant_type" to GrantType.AUTHORIZATION_CODE.value,
                            "code" to code,
                            "client_id" to "client1",
                            "client_secret" to "secret",
                            "redirect_uri" to "http://defaultRedirectUri",
                        ),
                    ).toTokenResponse()
                    .refreshToken
                    .shouldNotBeNull()
            client
                .tokenRequest(
                    tokenEndpointUrl("default"),
                    "client1" to "secret",
                    mapOf("grant_type" to GrantType.REFRESH_TOKEN.value, "refresh_token" to refreshToken),
                ).toTokenResponse()
                .asClue {
                    it.status shouldBe 200
                    it.expiresIn shouldBe 3600
                }
        }
    }

    @Test
    fun `expires_in should use the token provider clock for password`() {
        withMockOAuth2Server(config) {
            client
                .tokenRequest(
                    tokenEndpointUrl("default"),
                    "client1" to "secret",
                    mapOf(
                        "grant_type" to GrantType.PASSWORD.value,
                        "username" to "user",
                        "password" to "password",
                        "scope" to "openid",
                    ),
                ).toTokenResponse()
                .asClue {
                    it.status shouldBe 200
                    it.expiresIn shouldBe 3600
                }
        }
    }

    @Test
    fun `expires_in should use the token provider clock for jwt-bearer`() {
        withMockOAuth2Server(config) {
            val assertion = issueToken("default", "client1").serialize()
            client
                .tokenRequest(
                    tokenEndpointUrl("default"),
                    "client1" to "secret",
                    mapOf(
                        "grant_type" to GrantType.JWT_BEARER.value,
                        "assertion" to assertion,
                        "scope" to "scope1",
                    ),
                ).toTokenResponse()
                .asClue {
                    it.status shouldBe 200
                    it.expiresIn shouldBe 3600
                }
        }
    }

    @Test
    fun `expires_in should use the token provider clock for token exchange`() {
        withMockOAuth2Server(config) {
            val subjectToken = issueToken("default", "client1").serialize()
            val clientAssertion = clientAssertion(clientId = "client1", audience = issuerUrl("default").toString()).serialize()
            client
                .tokenRequest(
                    tokenEndpointUrl("default"),
                    mapOf(
                        "grant_type" to GrantType.TOKEN_EXCHANGE.value,
                        "client_assertion_type" to ClientAssertionType.JWT_BEARER,
                        "client_assertion" to clientAssertion,
                        "subject_token_type" to SubjectTokenType.TOKEN_TYPE_JWT,
                        "subject_token" to subjectToken,
                        "audience" to "targetAudience",
                    ),
                ).toTokenResponse()
                .asClue {
                    it.status shouldBe 200
                    it.expiresIn shouldBe 3600
                }
        }
    }
}
