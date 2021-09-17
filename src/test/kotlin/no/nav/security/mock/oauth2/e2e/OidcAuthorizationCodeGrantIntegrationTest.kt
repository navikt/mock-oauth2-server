package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import org.junit.jupiter.api.Test

class OidcAuthorizationCodeGrantIntegrationTest {

    private val server = MockOAuth2Server().apply { start() }
    private val client = client()

    @Test
    fun `authentication request should return 302 with redirectUri as location and query params state and code`() {

        client.get(
            server.authorizationEndpointUrl("default").authenticationRequest(redirectUri = "http://mycallback", state = "mystate")
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
        val code = client.get(server.authorizationEndpointUrl("default").authenticationRequest()).let { authResponse ->
            authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
        }

        code.shouldNotBeNull()

        client.tokenRequest(
            server.tokenEndpointUrl("default"),
            mapOf(
                "client_id" to "client1",
                "client_secret" to "secret",
                "grant_type" to "authorization_code",
                "scope" to "openid scope1",
                "redirect_uri" to "http://mycallback",
                "code" to code
            )
        ).toTokenResponse().asClue {
            it.accessToken shouldNotBe null
            it.idToken shouldNotBe null
            it.expiresIn shouldBeGreaterThan 0
            it.scope shouldBe "openid scope1"
            it.idToken?.audience shouldContainExactly listOf("client1")
            it.accessToken?.audience shouldContainExactly listOf("scope1")
        }
    }

    @Test
    fun `complete authorization code flow with interactivelogin enable should return tokens with sub=username posted to login`() {
        val server = MockOAuth2Server(OAuth2Config(interactiveLogin = true)).apply { start() }
        // simulate user interaction by doing the auth request as a post (instead of get with user punching username/pwd and submitting form)
        val code = client.post(
            server.authorizationEndpointUrl("default").authenticationRequest(),
            mapOf("username" to "foo")
        ).let { authResponse ->
            authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
        }

        code.shouldNotBeNull()

        client.tokenRequest(
            server.tokenEndpointUrl("default"),
            mapOf(
                "client_id" to "client1",
                "client_secret" to "secret",
                "grant_type" to "authorization_code",
                "scope" to "openid scope1",
                "redirect_uri" to "http://mycallback",
                "code" to code
            )
        ).toTokenResponse().asClue {
            it.accessToken shouldNotBe null
            it.idToken shouldNotBe null
            it.expiresIn shouldBeGreaterThan 0
            it.scope shouldBe "openid scope1"
            it.idToken?.audience shouldContainExactly listOf("client1")
            it.accessToken?.audience shouldContainExactly listOf("scope1")
            it.idToken?.subject shouldBe "foo"
        }
        server.shutdown()
    }

    @Test
    fun `something pkce`() {

        // @ Todo: if code challenge is received in authn req it should be enforced as required in token req, i.e. fail if code_verifier is not present

        val pkce = Pkce()
        client.get(
            server.authorizationEndpointUrl("default").authenticationRequest(pkce = pkce)
        ).asClue { response ->
            response.code shouldBe 302
            response.headers["location"]?.toHttpUrl().asClue {
                it?.queryParameterNames shouldContainExactly setOf("code", "state")
                it?.queryParameter("state") shouldBe "1234"
                println("yolo: $it")
            }
        }
    }
}
