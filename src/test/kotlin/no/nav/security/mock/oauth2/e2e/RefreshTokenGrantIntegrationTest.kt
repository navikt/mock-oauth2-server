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
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.post
import no.nav.security.mock.oauth2.testutils.shouldBeValidFor
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.testutils.verifyWith
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.RequestMapping
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

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
    fun `rotated refresh token should persist enqueued callback when override is applied`() {
        withMockOAuth2Server(OAuth2Config(rotateRefreshToken = true)) {
            val issuerId = "idprovider"

            val initialResponse = this.runAuthCodeFlow(issuerId, "initial-subject")
            val firstRefreshToken = checkNotNull(initialResponse.refreshToken)

            val enqueuedSubject = "queued-subject"
            this.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = issuerId, subject = enqueuedSubject))

            val firstRefreshResponse =
                client
                    .tokenRequest(
                        this.tokenEndpointUrl(issuerId),
                        mapOf(
                            "grant_type" to GrantType.REFRESH_TOKEN.value,
                            "refresh_token" to firstRefreshToken,
                            "client_id" to "id",
                            "client_secret" to "secret",
                        ),
                    ).toTokenResponse()

            firstRefreshResponse.idToken!!.subject shouldBe enqueuedSubject

            val secondRefreshToken = checkNotNull(firstRefreshResponse.refreshToken)
            val secondRefreshResponse =
                client
                    .tokenRequest(
                        this.tokenEndpointUrl(issuerId),
                        mapOf(
                            "grant_type" to GrantType.REFRESH_TOKEN.value,
                            "refresh_token" to secondRefreshToken,
                            "client_id" to "id",
                            "client_secret" to "secret",
                        ),
                    ).toTokenResponse()

            secondRefreshResponse.idToken!!.subject shouldBe enqueuedSubject
        }
    }

    @Test
    fun `refresh_token grant should reuse auth request params captured in authorization code flow`() {
        val issuerId = "refresh-propagation-issuer"
        val server =
            MockOAuth2Server(
                OAuth2Config(
                    tokenCallbacks =
                        setOf(
                            RequestMappingTokenCallback(
                                issuerId = issuerId,
                                requestMappings =
                                    listOf(
                                        RequestMapping(
                                            requestParam = "login_hint",
                                            match = "anna@example.com",
                                            claims =
                                                mapOf(
                                                    "sub" to "from-login-hint",
                                                    "email" to "${'$'}{login_hint}",
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                ),
            ).apply { start() }

        val authorizationCode =
            client
                .get(
                    server.authorizationEndpointUrl(issuerId).authenticationRequest(
                        clientId = "my-app",
                        extraQueryParams = mapOf("login_hint" to "anna@example.com"),
                    ),
                ).use {
                    it.headers["location"]?.toHttpUrl()?.queryParameter("code")
                }

        authorizationCode.shouldNotBeNull()

        val tokenResponseBeforeRefresh =
            client
                .tokenRequest(
                    server.tokenEndpointUrl(issuerId),
                    mapOf(
                        "grant_type" to GrantType.AUTHORIZATION_CODE.value,
                        "code" to authorizationCode,
                        "client_id" to "my-app",
                        "redirect_uri" to "http://defaultRedirectUri",
                    ),
                ).toTokenResponse()

        val refreshToken = checkNotNull(tokenResponseBeforeRefresh.refreshToken)

        val refreshTokenResponse =
            client
                .tokenRequest(
                    server.tokenEndpointUrl(issuerId),
                    mapOf(
                        "grant_type" to GrantType.REFRESH_TOKEN.value,
                        "refresh_token" to refreshToken,
                        "client_id" to "my-app",
                        "client_secret" to "secret",
                        // conflicting token-body param should not override stored auth request params
                        "login_hint" to "max@example.com",
                    ),
                ).toTokenResponse()

        val idToken = refreshTokenResponse.idToken
        idToken.shouldNotBeNull()
        idToken.subject shouldBe "from-login-hint"
        idToken.claims["email"] shouldBe "anna@example.com"

        server.shutdown()
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
    fun `token request with bogus refresh_token should return 400 invalid_grant even when callback is enqueued`() {
        withMockOAuth2Server {
            val issuerId = "idprovider"
            this.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = issuerId, subject = "queued-subject"))

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

    private fun MockOAuth2Server.runAuthCodeFlow(
        issuerId: String,
        initialSubject: String,
    ): ParsedTokenResponse {
        // Authenticate using Authorization Code Flow
        // simulate user interaction by doing the auth request as a post (instead of get with user punching username/pwd and submitting form)
        val authorizationCode =
            client
                .post(
                    this.authorizationEndpointUrl("default").authenticationRequest(),
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
