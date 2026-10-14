package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.Pkce
import no.nav.security.mock.oauth2.testutils.audience
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.post
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

class OidcAuthorizationCodeGrantIntegrationTest {
    private val server = MockOAuth2Server().apply { start() }
    private val strictServer = MockOAuth2Server(OAuth2Config(strictAuthorizationCodeBinding = true)).apply { start() }
    private val client = client()

    @Test
    fun `authentication request should return 302 with redirectUri as location and query params state and code`() {
        client
            .get(
                server.authorizationEndpointUrl("default").authenticationRequest(redirectUri = "http://mycallback", state = "mystate"),
            ).asClue { response ->
                response.code shouldBe 302
                response.headers["location"]?.toHttpUrl().asClue {
                    it.toString() shouldStartWith "http://mycallback"
                    it?.queryParameterNames shouldContainExactly setOf("code", "state")
                    it?.queryParameter("state") shouldBe "mystate"
                }
            }
    }

    @Test
    fun `complete authorization code flow should return tokens according to spec`() {
        val code =
            client.get(server.authorizationEndpointUrl("default").authenticationRequest()).let { authResponse ->
                authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
            }

        code.shouldNotBeNull()

        client
            .tokenRequest(
                server.tokenEndpointUrl("default"),
                mapOf(
                    "client_id" to "client1",
                    "client_secret" to "secret",
                    "grant_type" to "authorization_code",
                    "redirect_uri" to "http://mycallback",
                    "code" to code,
                ),
            ).toTokenResponse()
            .asClue {
                it.accessToken shouldNotBe null
                it.idToken shouldNotBe null
                it.expiresIn shouldBeGreaterThan 0
                it.idToken?.audience shouldContainExactly listOf("client1")
                it.accessToken?.audience shouldContainExactly listOf("default")
            }
    }

    @Test
    fun `complete authorization code flow with interactivelogin enabled should return tokens with sub=username posted to login`() {
        val server = MockOAuth2Server(OAuth2Config(interactiveLogin = true)).apply { start() }
        // simulate user interaction by doing the auth request as a post (instead of get with user punching username/pwd and submitting form)
        val code =
            client
                .post(
                    server.authorizationEndpointUrl("default").authenticationRequest(),
                    mapOf("username" to "foo"),
                ).let { authResponse ->
                    authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
                }

        code.shouldNotBeNull()

        client
            .tokenRequest(
                server.tokenEndpointUrl("default"),
                mapOf(
                    "client_id" to "client1",
                    "client_secret" to "secret",
                    "grant_type" to "authorization_code",
                    "redirect_uri" to "http://mycallback",
                    "code" to code,
                ),
            ).toTokenResponse()
            .asClue {
                it.accessToken shouldNotBe null
                it.idToken shouldNotBe null
                it.expiresIn shouldBeGreaterThan 0
                it.idToken?.audience shouldContainExactly listOf("client1")
                it.accessToken?.audience shouldContainExactly listOf("default")
                it.idToken?.subject shouldBe "foo"
            }
        server.shutdown()
    }

    @Test
    fun `authorization code flow should return tokens on token request when valid PKCE code_verifier is used`() {
        val pkce = Pkce()
        val code =
            client
                .get(
                    server.authorizationEndpointUrl("default").authenticationRequest(pkce = pkce),
                ).let { authResponse ->
                    authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
                }

        code.shouldNotBeNull()

        client.tokenRequest(code, pkce).asClue {
            it.code shouldBe 200
            it.body.string() shouldContain "id_token"
        }
    }

    @Test
    fun `authorization code flow should reject token request when PKCE code_verifier is omitted`() {
        val pkce = Pkce()
        val code =
            client
                .get(
                    server.authorizationEndpointUrl("default").authenticationRequest(pkce = pkce),
                ).let { authResponse ->
                    authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
                }

        code.shouldNotBeNull()

        client.tokenRequest(code).asClue {
            it.code shouldBe 400
            it.body.string() shouldContain "invalid_grant"
        }
    }

    @Test
    fun `authorization code flow should return 400 bad request on token request when invalid PKCE code_verifier is used`() {
        val pkce = Pkce()
        val code =
            client
                .get(
                    server.authorizationEndpointUrl("default").authenticationRequest(pkce = pkce),
                ).let { authResponse ->
                    authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
                }

        code.shouldNotBeNull()

        val invalidPkce = Pkce()
        client.tokenRequest(code, invalidPkce).asClue {
            it.code shouldBe 400
            it.body.string() shouldContain "code_verifier does not compute to code_challenge from request"
        }
    }

    @Test
    fun `authorization code flow should not accept the same code after a failed PKCE verification`() {
        val pkce = Pkce()
        val code =
            client
                .get(
                    server.authorizationEndpointUrl("default").authenticationRequest(
                        clientId = "client1",
                        redirectUri = "http://mycallback",
                        pkce = pkce,
                    ),
                ).let { authResponse ->
                    authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
                }

        code.shouldNotBeNull()

        val invalidPkce = Pkce()
        client.tokenRequest(code, invalidPkce).asClue {
            it.code shouldBe 400
            it.body.string() shouldContain "code_verifier does not compute to code_challenge from request"
        }

        client.tokenRequest(code, pkce).asClue {
            it.code shouldBe 400
            it.body.string() shouldContain "invalid_grant"
        }
    }

    @Test
    fun `authorization code flow should default to plain PKCE when code_challenge_method is omitted`() {
        val verifier = Pkce().verifier
        val code =
            client
                .get(
                    server
                        .authorizationEndpointUrl("default")
                        .authenticationRequest()
                        .newBuilder()
                        .addQueryParameter("code_challenge", verifier.value)
                        .build(),
                ).let { authResponse ->
                    authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
                }

        code.shouldNotBeNull()

        client.tokenRequest(code, Pkce(verifier = verifier)).asClue {
            it.code shouldBe 200
            it.toTokenResponse().accessToken shouldNotBe null
        }
    }

    @Test
    fun `authorization code flow should reject code_verifier when authorization request had no code_challenge`() {
        val code =
            client.get(server.authorizationEndpointUrl("default").authenticationRequest()).let { authResponse ->
                authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
            }

        code.shouldNotBeNull()

        client.tokenRequest(code, Pkce()).asClue {
            it.code shouldBe 400
            it.body.string() shouldContain "invalid_grant"
        }
    }

    @Test
    fun `form_post authentication response without state should return form without state`() {
        client
            .get(
                server
                    .authorizationEndpointUrl("default")
                    .newBuilder()
                    .addQueryParameter("client_id", "client1")
                    .addQueryParameter("response_type", "code")
                    .addQueryParameter("redirect_uri", "http://mycallback")
                    .addQueryParameter("response_mode", "form_post")
                    .addQueryParameter("scope", "openid")
                    .build(),
            ).asClue { response ->
                response.code shouldBe 200
                val body = response.body.string()
                body shouldContain "name=\"code\""
                body shouldNotContain "name=\"state\""
            }
    }

    @Test
    fun `authorization code flow should return invalid_grant for unknown or bogus authorization code`() {
        client.tokenRequest("bogus-code-that-was-never-issued").asClue {
            it.code shouldBe 400
            it.body.string() shouldContain "invalid_grant"
        }
    }

    @Test
    fun `authorization code flow should reject token request from a different client than the one the code was issued to`() {
        val code = issueCode(clientId = "client1", redirectUri = "http://mycallback")

        client.strictTokenRequest(code, clientId = "client2").asClue {
            it.code shouldBe 400
            it.body.string() shouldContain "invalid_grant"
        }
    }

    @Test
    fun `authorization code flow should reject token request with a redirect_uri different from the authorization request`() {
        val code = issueCode(clientId = "client1", redirectUri = "http://mycallback")

        client.strictTokenRequest(code, redirectUri = "http://evil").asClue {
            it.code shouldBe 400
            it.body.string() shouldContain "invalid_grant"
        }
    }

    @Test
    fun `authorization code flow should reject token request without redirect_uri when authorization request had one`() {
        val code = issueCode(clientId = "client1", redirectUri = "http://mycallback")

        client.strictTokenRequest(code, redirectUri = null).asClue {
            it.code shouldBe 400
            it.body.string() shouldContain "invalid_grant"
        }
    }

    @Test
    fun `authorization code flow should not accept the same code after a failed client or redirect_uri check`() {
        val code = issueCode(clientId = "client1", redirectUri = "http://mycallback")

        client.strictTokenRequest(code, clientId = "client2").code shouldBe 400
        client.strictTokenRequest(code).code shouldBe 400
    }

    @Test
    fun `authorization code flow should accept mismatching client and redirect_uri when strict binding is disabled`() {
        val code =
            client
                .get(server.authorizationEndpointUrl("default").authenticationRequest(clientId = "otherClient", redirectUri = "http://otherCallback"))
                .headers["location"]
                ?.toHttpUrl()
                ?.queryParameter("code")
                .shouldNotBeNull()

        client.tokenRequest(code).code shouldBe 200
    }

    private fun issueCode(
        clientId: String,
        redirectUri: String,
    ): String =
        client
            .get(strictServer.authorizationEndpointUrl("default").authenticationRequest(clientId = clientId, redirectUri = redirectUri))
            .headers["location"]
            ?.toHttpUrl()
            ?.queryParameter("code")
            .shouldNotBeNull()

    private fun OkHttpClient.tokenRequest(
        code: String,
        pkce: Pkce? = null,
        clientId: String = "client1",
        redirectUri: String? = "http://mycallback",
    ) = tokenRequest(
        server.tokenEndpointUrl("default"),
        mutableMapOf(
            "client_id" to clientId,
            "client_secret" to "secret",
            "grant_type" to "authorization_code",
            "code" to code,
        ).apply {
            if (redirectUri != null) {
                put("redirect_uri", redirectUri)
            }
            if (pkce != null) {
                put("code_verifier", pkce.verifier.value)
            }
        },
    )

    private fun OkHttpClient.strictTokenRequest(
        code: String,
        clientId: String = "client1",
        redirectUri: String? = "http://mycallback",
    ) = tokenRequest(
        strictServer.tokenEndpointUrl("default"),
        mutableMapOf(
            "client_id" to clientId,
            "client_secret" to "secret",
            "grant_type" to "authorization_code",
            "code" to code,
        ).apply {
            if (redirectUri != null) {
                put("redirect_uri", redirectUri)
            }
        },
    )
}
