package no.nav.security.mock.oauth2.e2e

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LoginPageIntegrationTest {

    private val client = client()

    @Test
    fun `authorization with interactive login should return built-in login page`() {
        val server = MockOAuth2Server(OAuth2Config(interactiveLogin = true)).apply { start() }
        val body = client.get(server.authorizationEndpointUrl("default").authenticationRequest()).body?.string()

        body shouldNotBe null
        body shouldContain "<h2 class=\"title\">Mock OAuth2 Server Sign-in</h2>"
    }

    @Test
    fun `authorization with interactive login and login page path set should return external login page`() {
        val server = MockOAuth2Server(
            OAuth2Config(
                interactiveLogin = true,
                loginPagePath = "./src/test/resources/login.example.html"
            )
        ).apply { start() }
        val body = client.get(server.authorizationEndpointUrl("default").authenticationRequest()).body?.string()

        body shouldNotBe null
        body shouldContain "<h4>Mock OAuth2 Server Example Sign-in</h4>"
    }

    @ParameterizedTest
    @ValueSource(strings = ["./src/test/resources/does-not-exists.html", "./src/test/resources/", ""])
    fun `authorization with interactive login and login page path set to invalid path should return 404`(path: String) {
        val server = MockOAuth2Server(
            OAuth2Config(
                interactiveLogin = true,
                loginPagePath = path
            )
        ).apply { start() }
        val code = client.get(server.authorizationEndpointUrl("default").authenticationRequest()).code

        code shouldBe 404
    }
}
