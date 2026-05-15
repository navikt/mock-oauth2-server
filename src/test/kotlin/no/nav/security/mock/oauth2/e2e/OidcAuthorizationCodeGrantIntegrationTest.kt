package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldStartWith
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.Pkce
import no.nav.security.mock.oauth2.testutils.audience
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.post
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.token.RequestMapping
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

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
    fun `authorization code flow should return invalid_grant for unknown or bogus authorization code`() {
        client.tokenRequest("bogus-code-that-was-never-issued").asClue {
            it.code shouldBe 400
            it.body.string() shouldContain "invalid_grant"
        }
    }

    @Test
    fun `authorization code flow with uuid template variable should produce a unique sub per request`() {
        val uuidRegex = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        val server = MockOAuth2Server(
            OAuth2Config(
                tokenCallbacks = setOf(
                    RequestMappingTokenCallback(
                        issuerId = "default",
                        requestMappings = listOf(
                            RequestMapping(
                                requestParam = "grant_type",
                                match = "*",
                                claims = mapOf("sub" to "\${uuid}"),
                            ),
                        ),
                    ),
                ),
            ),
        ).apply { start() }

        fun doFlow(): String {
            val code = client.get(
                server.authorizationEndpointUrl("default").authenticationRequest(),
            ).headers["location"]?.toHttpUrl()?.queryParameter("code")
            code.shouldNotBeNull()
            return client.tokenRequest(
                server.tokenEndpointUrl("default"),
                mutableMapOf(
                    "client_id" to "client1",
                    "client_secret" to "secret",
                    "grant_type" to "authorization_code",
                    "redirect_uri" to "http://defaultRedirectUri",
                    "code" to code,
                ),
            ).toTokenResponse().accessToken?.subject!!
        }

        val sub1 = doFlow()
        val sub2 = doFlow()

        sub1 shouldMatch uuidRegex
        sub2 shouldMatch uuidRegex
        sub1 shouldNotBe sub2

        server.shutdown()
    }

    @Test
    fun `authorization code flow should forward custom authorize query params as template variables in token claims`() {
        val server = MockOAuth2Server(
            OAuth2Config(
                tokenCallbacks = setOf(
                    RequestMappingTokenCallback(
                        issuerId = "default",
                        requestMappings = listOf(
                            RequestMapping(
                                requestParam = "grant_type",
                                match = "*",
                                claims = mapOf("sub" to "\${userId}", "email" to "\${userEmail}"),
                            ),
                        ),
                    ),
                ),
            ),
        ).apply { start() }

        val code = client.get(
            server.authorizationEndpointUrl("default").authenticationRequest(
                extraParams = mapOf("userId" to "alice123", "userEmail" to "alice@example.com"),
            ),
        ).headers["location"]?.toHttpUrl()?.queryParameter("code")
        code.shouldNotBeNull()

        client.tokenRequest(
            server.tokenEndpointUrl("default"),
            mutableMapOf(
                "client_id" to "client1",
                "client_secret" to "secret",
                "grant_type" to "authorization_code",
                "redirect_uri" to "http://defaultRedirectUri",
                "code" to code,
            ),
        ).toTokenResponse().accessToken.asClue {
            it.shouldNotBeNull()
            it.subject shouldBe "alice123"
            it.claims["email"] shouldBe "alice@example.com"
        }

        server.shutdown()
    }

    @Test
    fun `${subject} template variable should resolve to interactive login username in requestMappings claims`() {
        val server = MockOAuth2Server(
            OAuth2Config(
                interactiveLogin = true,
                tokenCallbacks = setOf(
                    RequestMappingTokenCallback(
                        issuerId = "default",
                        requestMappings = listOf(
                            RequestMapping(
                                requestParam = "grant_type",
                                match = "authorization_code",
                                claims = mapOf(
                                    "custom_sub" to "\${subject}",
                                    "also_sub" to "\${sub}",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ).apply { start() }

        val code = client.post(
            server.authorizationEndpointUrl("default").authenticationRequest(),
            mapOf("username" to "testuser"),
        ).headers["location"]?.toHttpUrl()?.queryParameter("code")
        code.shouldNotBeNull()

        client.tokenRequest(
            server.tokenEndpointUrl("default"),
            mutableMapOf(
                "client_id" to "client1",
                "client_secret" to "secret",
                "grant_type" to "authorization_code",
                "redirect_uri" to "http://mycallback",
                "code" to code,
            ),
        ).toTokenResponse().accessToken.asClue {
            it.shouldNotBeNull()
            it.subject shouldBe "testuser"
            it.claims["custom_sub"] shouldBe "testuser"
            it.claims["also_sub"] shouldBe "testuser"
        }

        server.shutdown()
    }

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
}
