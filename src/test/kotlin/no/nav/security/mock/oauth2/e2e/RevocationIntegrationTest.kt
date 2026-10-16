package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.grant.RefreshToken
import no.nav.security.mock.oauth2.testutils.ParsedTokenResponse
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.post
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

class RevocationIntegrationTest {
    private val client: OkHttpClient = client()
    private val initialSubject = "yolo"
    private val issuerId = "idprovider"

    @Test
    fun `revocation request with refresh_token should should remove refresh token`() {
        withMockOAuth2Server {
            val tokenResponseBeforeRefresh = login()
            tokenResponseBeforeRefresh.idToken?.subject shouldBe initialSubject
            tokenResponseBeforeRefresh.accessToken?.subject shouldBe initialSubject

            val refreshTokenResponse = refresh(tokenResponseBeforeRefresh.refreshToken)
            refreshTokenResponse.accessToken?.subject shouldBe initialSubject
            val refreshToken = checkNotNull(refreshTokenResponse.refreshToken)
            val revocationResponse =
                client.post(
                    this.url("/default/revoke"),
                    mapOf(
                        "client_id" to "id",
                        "client_secret" to "secret",
                        "token" to refreshToken,
                        "token_type_hint" to "refresh_token",
                    ),
                )
            revocationResponse.use {
                it.code shouldBe 200
                it.body.string() shouldBe ""
            }

            // after revocation, using the revoked refresh token must return 400 invalid_grant
            client
                .tokenRequest(
                    this.tokenEndpointUrl(issuerId),
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

    @Test
    fun `revocation request without token_type_hint should remove refresh token`() {
        withMockOAuth2Server {
            val refreshToken = checkNotNull(login().refreshToken)
            revoke(refreshToken, tokenTypeHint = null).use {
                it.code shouldBe 200
            }
            refreshShouldFail(refreshToken)
        }
    }

    @Test
    fun `revocation request with token_type_hint access_token should return 200`() {
        withMockOAuth2Server {
            val accessToken = checkNotNull(login().accessToken).serialize()
            revoke(accessToken, tokenTypeHint = "access_token").use {
                it.code shouldBe 200
            }
        }
    }

    @Test
    fun `revocation request with wrong token_type_hint should still remove refresh token`() {
        withMockOAuth2Server {
            val refreshToken = checkNotNull(login().refreshToken)
            revoke(refreshToken, tokenTypeHint = "access_token").use {
                it.code shouldBe 200
            }
            refreshShouldFail(refreshToken)
        }
    }

    @Test
    fun `revocation request with unknown token should return 200`() {
        withMockOAuth2Server {
            revoke("unknown-token", tokenTypeHint = null).use {
                it.code shouldBe 200
            }
        }
    }

    @Test
    fun `revocation request with a line break in token_type_hint should return unsupported_token_type`() {
        withMockOAuth2Server {
            revoke("some-token", tokenTypeHint = "bad\nsecond-line").use {
                it.code shouldBe 400
                val body = it.body.string()
                body shouldContain "unsupported_token_type"
                body shouldContain "unsupported token type: badsecond-line"
            }
        }
    }

    @Test
    fun `revocation request without token should return invalid_request`() {
        withMockOAuth2Server {
            client
                .post(
                    this.url("/default/revoke"),
                    mapOf(
                        "client_id" to "id",
                        "client_secret" to "secret",
                        "token_type_hint" to "refresh_token",
                    ),
                ).use {
                    it.code shouldBe 400
                    it.body.string() shouldContain "invalid_request"
                }
        }
    }

    @Test
    fun `revocation request without token and token_type_hint should return invalid_request`() {
        withMockOAuth2Server {
            client
                .post(
                    this.url("/default/revoke"),
                    mapOf(
                        "client_id" to "id",
                        "client_secret" to "secret",
                    ),
                ).use {
                    it.code shouldBe 400
                    it.body.string() shouldContain "invalid_request"
                }
        }
    }

    private fun MockOAuth2Server.revoke(
        token: String,
        tokenTypeHint: String?,
    ) = client.post(
        this.url("/default/revoke"),
        mutableMapOf(
            "client_id" to "id",
            "client_secret" to "secret",
            "token" to token,
        ).apply {
            if (tokenTypeHint != null) {
                put("token_type_hint", tokenTypeHint)
            }
        },
    )

    private fun MockOAuth2Server.refreshShouldFail(refreshToken: RefreshToken) {
        client
            .tokenRequest(
                this.tokenEndpointUrl(issuerId),
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

    private fun MockOAuth2Server.login(): ParsedTokenResponse {
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
        return client
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
    }

    private fun MockOAuth2Server.refresh(token: RefreshToken?): ParsedTokenResponse {
        // make token request with the refresh_token grant
        val refreshToken = checkNotNull(token)
        return client
            .tokenRequest(
                this.tokenEndpointUrl(issuerId),
                mapOf(
                    "grant_type" to GrantType.REFRESH_TOKEN.value,
                    "refresh_token" to refreshToken,
                    "client_id" to "id",
                    "client_secret" to "secret",
                ),
            ).toTokenResponse()
    }
}
