package no.nav.security.mock.oauth2.e2e

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod
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
import no.nav.security.mock.oauth2.testutils.parse
import no.nav.security.mock.oauth2.testutils.post
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class OidcAuthorizationCodeGrantIntegrationTest {
    private val server = MockOAuth2Server().apply { start() }
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

    @ParameterizedTest
    @ValueSource(
        strings = [
            "javascript:alert(document.domain)",
            "customapp://callback.example/finish",
            "http:///finish",
            "http://:8080/finish",
            "http://user@/finish",
            "http://user@:8080/finish",
        ],
    )
    fun `form post rejects non HTTP redirect URIs`(redirectUri: String) {
        client.get(server.authorizationEndpointUrl("default").authenticationRequest(redirectUri = redirectUri, responseMode = "form_post")).use {
            it.code shouldBe 400
            it.body.string() shouldContain "invalid_request"
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "http://callback.example/finish",
            "https://callback.example/finish",
            "http://callback_service:8080/finish",
            "http://user@callback_service/finish",
        ],
    )
    fun `form post sends authorization code to HTTP callback`(redirectUri: String) {
        client.get(server.authorizationEndpointUrl("default").authenticationRequest(redirectUri = redirectUri, responseMode = "form_post")).use {
            it.code shouldBe 200
            it.body.string() shouldContain "action=\"$redirectUri\""
        }
    }

    @Test
    fun `authentication request should return state with reserved characters unchanged`() {
        client
            .get(
                server.authorizationEndpointUrl("default").authenticationRequest(redirectUri = "http://mycallback", state = "a&b=c"),
            ).asClue { response ->
                response.code shouldBe 302
                val location = checkNotNull(response.headers["location"]).toHttpUrl()
                location.queryParameter("state") shouldBe "a&b=c"
                location.queryParameterNames shouldContainExactly setOf("code", "state")
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
    fun `authorization code flow should return tokens when code_challenge_method is plain`() {
        val pkce = Pkce(method = CodeChallengeMethod.PLAIN)
        val code =
            client.get(server.authorizationEndpointUrl("default").authenticationRequest(pkce = pkce)).let { authResponse ->
                authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
            }

        code.shouldNotBeNull()

        client.tokenRequest(code, pkce).asClue {
            it.code shouldBe 200
            it.toTokenResponse().accessToken shouldNotBe null
        }
    }

    @Test
    fun `authentication request should return invalid_request when code_challenge_method is not supported`() {
        client.get(server.authorizationEndpointUrl("default").withUnsupportedCodeChallengeMethod()).asClue {
            it.code shouldBe 400
            it.headers["location"] shouldBe null
            it.body.string() shouldContain "invalid_request"
        }
    }

    @Test
    fun `authentication request with interactivelogin enabled should return invalid_request when code_challenge_method is not supported`() {
        val server = MockOAuth2Server(OAuth2Config(interactiveLogin = true)).apply { start() }
        val authorizationUrl = server.authorizationEndpointUrl("default").withUnsupportedCodeChallengeMethod()

        client.get(authorizationUrl).asClue {
            it.code shouldBe 400
            it.body.string() shouldContain "invalid_request"
        }
        client.post(authorizationUrl, mapOf("username" to "foo")).asClue {
            it.code shouldBe 400
            it.headers["location"] shouldBe null
            it.body.string() shouldContain "invalid_request"
        }
        server.shutdown()
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
    fun `authorization code flow should use the enqueued token callback`() {
        val code = authorizationCode()
        server.enqueueCallback(DefaultOAuth2TokenCallback(claims = mapOf("source" to "enqueued")))

        client
            .tokenRequest(code)
            .toTokenResponse()
            .accessToken
            .shouldNotBeNull()
            .source() shouldBe "enqueued"
    }

    @Test
    fun `unknown authorization code should not use up the enqueued token callback`() {
        server.enqueueCallback(DefaultOAuth2TokenCallback(claims = mapOf("source" to "enqueued")))

        client.tokenRequest("bogus-code-that-was-never-issued").asClue {
            it.code shouldBe 400
            it.parse<Map<String, Any>>()["error"] shouldBe "invalid_grant"
        }

        clientCredentialsToken().source() shouldBe "enqueued"
    }

    @Test
    fun `failed PKCE verification should not use up the enqueued token callback`() {
        val code = authorizationCode(pkce = Pkce())
        server.enqueueCallback(DefaultOAuth2TokenCallback(claims = mapOf("source" to "enqueued")))

        client.tokenRequest(code, Pkce()).asClue {
            it.code shouldBe 400
            it.parse<Map<String, Any>>()["error"] shouldBe "invalid_grant"
        }

        clientCredentialsToken().source() shouldBe "enqueued"
    }

    @Test
    fun `rejected authorization codes should keep the order of the enqueued token callbacks`() {
        server.enqueueCallback(DefaultOAuth2TokenCallback(claims = mapOf("source" to "first")))
        server.enqueueCallback(DefaultOAuth2TokenCallback(claims = mapOf("source" to "second")))

        repeat(2) {
            client.tokenRequest("bogus-code-that-was-never-issued").asClue {
                it.code shouldBe 400
                it.parse<Map<String, Any>>()["error"] shouldBe "invalid_grant"
            }
        }

        clientCredentialsToken().source() shouldBe "first"
        clientCredentialsToken().source() shouldBe "second"
    }

    private fun authorizationCode(pkce: Pkce? = null): String =
        client
            .get(server.authorizationEndpointUrl("default").authenticationRequest(redirectUri = "http://mycallback", pkce = pkce))
            .headers["location"]
            ?.toHttpUrl()
            ?.queryParameter("code")
            .shouldNotBeNull()

    private fun clientCredentialsToken(): SignedJWT =
        client
            .tokenRequest(
                server.tokenEndpointUrl("default"),
                "client1" to "secret",
                mapOf("grant_type" to "client_credentials", "scope" to "scope1"),
            ).toTokenResponse()
            .accessToken
            .shouldNotBeNull()

    private fun SignedJWT.source(): String? = jwtClaimsSet.getStringClaim("source")

    private fun OkHttpClient.tokenRequest(
        code: String,
        pkce: Pkce? = null,
    ) = tokenRequest(
        server.tokenEndpointUrl("default"),
        mutableMapOf(
            "client_id" to "client1",
            "client_secret" to "secret",
            "grant_type" to "authorization_code",
            "redirect_uri" to "http://mycallback",
            "code" to code,
        ).apply {
            if (pkce != null) {
                put("code_verifier", pkce.verifier.value)
            }
        },
    )

    private fun HttpUrl.withUnsupportedCodeChallengeMethod(): HttpUrl =
        authenticationRequest()
            .newBuilder()
            .addQueryParameter("code_challenge", Pkce().challenge.value)
            .addQueryParameter("code_challenge_method", "S512")
            .build()
}
