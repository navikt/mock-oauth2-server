package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.post
import no.nav.security.mock.oauth2.testutils.queryParameters
import no.nav.security.mock.oauth2.testutils.withoutQuery
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LoginPageIntegrationTest {
    private val client = client()

    @Test
    fun `authorization with interactive login should return built-in login page`() {
        val server = MockOAuth2Server(OAuth2Config(interactiveLogin = true)).apply { start() }
        val body = client.get(server.authorizationEndpointUrl("default").authenticationRequest()).body.string()

        body shouldNotBe null
        body shouldContain "<h2 class=\"title\">Mock OAuth2 Server Sign-in</h2>"
    }

    @Test
    fun `authorization sent as POST with form parameters and interactive login should lead to the login page`() {
        val server = MockOAuth2Server(OAuth2Config(interactiveLogin = true)).apply { start() }
        val authorizationUrl = server.authorizationEndpointUrl("default").authenticationRequest(redirectUri = "http://mycallback", state = "mystate")

        val loginPageUrl =
            client.post(authorizationUrl.withoutQuery(), authorizationUrl.queryParameters()).let { response ->
                response.code shouldBe 303
                checkNotNull(response.headers["location"]).toHttpUrl()
            }

        loginPageUrl.withoutQuery() shouldBe authorizationUrl.withoutQuery()
        loginPageUrl.queryParameters() shouldBe authorizationUrl.queryParameters()
        client.get(loginPageUrl).body.string() shouldContain "<h2 class=\"title\">Mock OAuth2 Server Sign-in</h2>"
        client.post(loginPageUrl, mapOf("username" to "foo")).asClue { response ->
            response.code shouldBe 302
            val location = checkNotNull(response.headers["location"]).toHttpUrl()
            location.queryParameter("state") shouldBe "mystate"
            location.queryParameter("code") shouldNotBe null
        }
        server.shutdown()
    }

    @Test
    fun `authorization with interactive login and login page path set should return external login page`() {
        val server =
            MockOAuth2Server(
                OAuth2Config(
                    interactiveLogin = true,
                    loginPagePath = "./src/test/resources/login.example.html",
                ),
            ).apply { start() }
        val body = client.get(server.authorizationEndpointUrl("default").authenticationRequest()).body.string()

        body shouldNotBe null
        body shouldContain "Mock OAuth2 Server Example"
    }

    @ParameterizedTest
    @ValueSource(strings = ["./src/test/resources/does-not-exists.html", "./src/test/resources/", ""])
    fun `authorization with interactive login and login page path set to invalid path should return 404`(path: String) {
        val server =
            MockOAuth2Server(
                OAuth2Config(
                    interactiveLogin = true,
                    loginPagePath = path,
                ),
            ).apply { start() }
        val code = client.get(server.authorizationEndpointUrl("default").authenticationRequest()).code

        code shouldBe 404
    }
}
