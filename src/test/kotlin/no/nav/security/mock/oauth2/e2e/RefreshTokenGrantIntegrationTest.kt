package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.assertions.asClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.ParsedTokenResponse
import no.nav.security.mock.oauth2.testutils.audience
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.post
import no.nav.security.mock.oauth2.testutils.shouldBeValidFor
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.testutils.verifyWith
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RefreshTokenGrantIntegrationTest {
    private val client: OkHttpClient = client()

    @Test
    fun `refresh_token grant should return id_token and access_token with same subject as authorization code grant`() {
        withMockOAuth2Server {
            val initialSubject = "yolo"
            val issuerId = "idprovider"

            val tokenResponseBeforeRefresh = this.runAuthCodeFlow(issuerId, initialSubject)

            // make token request with the refresh_token grant
            val refreshToken = checkNotNull(tokenResponseBeforeRefresh.refreshToken)
            val refreshTokenResponse =
                client
                    .tokenRequest(
                        this.tokenEndpointUrl(issuerId),
                        mapOf(
                            "grant_type" to GrantType.REFRESH_TOKEN.value,
                            "refresh_token" to refreshToken,
                            "client_id" to "id",
                            "client_secret" to "secret",
                        ),
                    ).toTokenResponse()

            refreshTokenResponse.asClue {
                it shouldBeValidFor GrantType.REFRESH_TOKEN
                it.refreshToken shouldBe tokenResponseBeforeRefresh.refreshToken
                it.idToken!! shouldNotBe tokenResponseBeforeRefresh.idToken!!
                it.accessToken!! shouldNotBe tokenResponseBeforeRefresh.accessToken!!
                it.accessToken should verifyWith(issuerId, this)
                it.idToken should verifyWith(issuerId, this)

                it.idToken.subject shouldBe initialSubject
                it.idToken.audience shouldBe tokenResponseBeforeRefresh.idToken.audience
                it.accessToken.subject shouldBe initialSubject
            }
        }
    }

    @Test
    fun `refresh_token grant should return tokens with same subject as authorization code grant, even when refreshtoken is rotated`() {
        withMockOAuth2Server(OAuth2Config(rotateRefreshToken = true)) {
            val initialSubject = "yolo"
            val issuerId = "idprovider"

            val tokenResponseBeforeRefresh = this.runAuthCodeFlow(issuerId, initialSubject)

            // make token request with the refresh_token grant
            val refreshToken = checkNotNull(tokenResponseBeforeRefresh.refreshToken)
            val refreshTokenResponse =
                client
                    .tokenRequest(
                        this.tokenEndpointUrl(issuerId),
                        mapOf(
                            "grant_type" to GrantType.REFRESH_TOKEN.value,
                            "refresh_token" to refreshToken,
                            "client_id" to "id",
                            "client_secret" to "secret",
                        ),
                    ).toTokenResponse()

            refreshTokenResponse.asClue {
                it shouldBeValidFor GrantType.REFRESH_TOKEN
                it.refreshToken shouldNotBe tokenResponseBeforeRefresh.refreshToken
                it.idToken?.subject shouldBe initialSubject
                it.idToken?.audience shouldBe tokenResponseBeforeRefresh.idToken?.audience
                it.accessToken?.subject shouldBe initialSubject
            }
        }
    }

    @Test
    fun `token request with refresh_token grant and enqueued tokencallback should return token with subject from tokencallback`() {
        withMockOAuth2Server {
            val expectedSubject = "expectedSub"
            val issuerId = "idprovider"

            val initialResponse = this.runAuthCodeFlow(issuerId, "anysubject")
            val refreshToken = checkNotNull(initialResponse.refreshToken)

            // enqueue callback after auth code flow so it is consumed by the refresh token request
            this.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = issuerId, subject = expectedSubject))

            val refreshTokenResponse =
                client
                    .tokenRequest(
                        this.tokenEndpointUrl(issuerId),
                        mapOf(
                            "grant_type" to GrantType.REFRESH_TOKEN.value,
                            "refresh_token" to refreshToken,
                            "client_id" to "id",
                            "client_secret" to "secret",
                        ),
                    ).toTokenResponse()

            refreshTokenResponse shouldBeValidFor GrantType.REFRESH_TOKEN
            refreshTokenResponse.idToken!!.subject shouldBe expectedSubject
        }
    }

    @Test
    fun `token request with bogus refresh_token should return 400 invalid_grant`() {
        withMockOAuth2Server {
            val issuerId = "idprovider"
            client
                .tokenRequest(
                    this.tokenEndpointUrl(issuerId),
                    mapOf(
                        "grant_type" to GrantType.REFRESH_TOKEN.value,
                        "refresh_token" to "bogus-random-uuid",
                        "client_id" to "id",
                        "client_secret" to "secret",
                    ),
                ).use {
                    it.code shouldBe 400
                    it.body.string() shouldContain "invalid_grant"
                }
        }
    }

    @Test
    fun `token request with bogus refresh_token and enqueued tokencallback should return 400 invalid_grant and keep the callback`() {
        withMockOAuth2Server {
            val expectedSubject = "expectedSub"
            val issuerId = "idprovider"
            val refreshToken = checkNotNull(this.runAuthCodeFlow(issuerId, "anysubject").refreshToken)
            this.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = issuerId, subject = expectedSubject))

            client.refreshTokenRequest(this, issuerId, "bogus-random-uuid").use {
                it.code shouldBe 400
                it.body.string() shouldContain "invalid_grant"
            }

            client.refreshTokenRequest(this, issuerId, refreshToken).toTokenResponse().asClue {
                it shouldBeValidFor GrantType.REFRESH_TOKEN
                it.idToken!!.subject shouldBe expectedSubject
            }
        }
    }

    @Test
    fun `token request with revoked refresh_token and enqueued tokencallback should return 400 invalid_grant`() {
        withMockOAuth2Server {
            val issuerId = "idprovider"
            val refreshToken = checkNotNull(this.runAuthCodeFlow(issuerId, "anysubject").refreshToken)
            client
                .tokenRequest(
                    this.revocationEndpointUrl(issuerId),
                    mapOf(
                        "token" to refreshToken,
                        "token_type_hint" to "refresh_token",
                        "client_id" to "id",
                        "client_secret" to "secret",
                    ),
                ).use { it.code shouldBe 200 }
            this.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = issuerId, subject = "expectedSub"))

            client.refreshTokenRequest(this, issuerId, refreshToken).use {
                it.code shouldBe 400
                it.body.string() shouldContain "invalid_grant"
            }
        }
    }

    @Test
    fun `refresh_token issued by one issuer should not be accepted by a different issuer`() {
        withMockOAuth2Server {
            val issuerA = "issuer-a"
            val issuerB = "issuer-b"

            val initialResponse = this.runAuthCodeFlow(issuerA, "subject")
            val refreshToken = checkNotNull(initialResponse.refreshToken)

            client
                .tokenRequest(
                    this.tokenEndpointUrl(issuerB),
                    mapOf(
                        "grant_type" to GrantType.REFRESH_TOKEN.value,
                        "refresh_token" to refreshToken,
                        "client_id" to "id",
                        "client_secret" to "secret",
                    ),
                ).use {
                    it.code shouldBe 400
                    it.body.string() shouldContain "invalid_grant"
                }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `refresh_token grant should reject scope that was not granted`(rotateRefreshToken: Boolean) {
        withMockOAuth2Server(OAuth2Config(rotateRefreshToken = rotateRefreshToken)) {
            val refreshToken = this.authCodeFlowRefreshToken("idprovider", scope = listOf("openid", "profile"))

            client.refreshWithScope(this, "idprovider", refreshToken, "openid profile admin").use {
                it.code shouldBe 400
                it.body.string() shouldContain "invalid_scope"
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `refresh_token grant should accept the granted scope or a subset, and use the granted scope when scope is omitted`(rotateRefreshToken: Boolean) {
        withMockOAuth2Server(OAuth2Config(rotateRefreshToken = rotateRefreshToken)) {
            var refreshToken = this.authCodeFlowRefreshToken("idprovider", scope = listOf("openid", "profile"))

            listOf("openid profile" to "openid profile", "openid" to "openid", null to "openid profile").forEach { (requested, expected) ->
                val response = client.refreshWithScope(this, "idprovider", refreshToken, requested).toTokenResponse()
                response.scope shouldBe expected
                refreshToken = checkNotNull(response.refreshToken)
            }
        }
    }

    private fun OkHttpClient.refreshTokenRequest(
        server: MockOAuth2Server,
        issuerId: String,
        refreshToken: String,
    ) = refreshWithScope(server, issuerId, refreshToken, scope = null)

    private fun OkHttpClient.refreshWithScope(
        server: MockOAuth2Server,
        issuerId: String,
        refreshToken: String,
        scope: String?,
    ) = tokenRequest(
        server.tokenEndpointUrl(issuerId),
        listOfNotNull(
            "grant_type" to GrantType.REFRESH_TOKEN.value,
            "refresh_token" to refreshToken,
            "client_id" to "id",
            "client_secret" to "secret",
            scope?.let { "scope" to it },
        ).toMap(),
    )

    private fun MockOAuth2Server.authCodeFlowRefreshToken(
        issuerId: String,
        scope: List<String>,
    ): String {
        val code =
            client
                .post(
                    this.authorizationEndpointUrl(issuerId).authenticationRequest(redirectUri = "http://something", scope = scope),
                    mapOf("username" to "foo"),
                ).let { it.headers["location"]?.toHttpUrl()?.queryParameter("code") }
                .shouldNotBeNull()
        return client
            .tokenRequest(
                this.tokenEndpointUrl(issuerId),
                mapOf(
                    "grant_type" to GrantType.AUTHORIZATION_CODE.value,
                    "code" to code,
                    "client_id" to "id",
                    "client_secret" to "secret",
                    "redirect_uri" to "http://something",
                ),
            ).toTokenResponse()
            .refreshToken
            .shouldNotBeNull()
    }

    private fun MockOAuth2Server.runAuthCodeFlow(
        issuerId: String,
        initialSubject: String,
    ): ParsedTokenResponse {
        // Authenticate using Authorization Code Flow
        // simulate user interaction by doing the auth request as a post (instead of get with user punching username/pwd and submitting form)
        val authorizationCode =
            client
                .post(
                    this.authorizationEndpointUrl(issuerId).authenticationRequest(),
                    mapOf("username" to initialSubject),
                ).let { authResponse ->
                    authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
                }

        authorizationCode.shouldNotBeNull()

        // Token Request based on authorization code
        val tokenResponseBeforeRefresh =
            client
                .tokenRequest(
                    this.tokenEndpointUrl(issuerId),
                    mapOf(
                        "grant_type" to GrantType.AUTHORIZATION_CODE.value,
                        "code" to authorizationCode,
                        "client_id" to "id",
                        "client_secret" to "secret",
                        "redirect_uri" to "http://something",
                    ),
                ).toTokenResponse()

        tokenResponseBeforeRefresh.idToken?.subject shouldBe initialSubject
        tokenResponseBeforeRefresh.accessToken?.subject shouldBe initialSubject
        return tokenResponseBeforeRefresh
    }
}
