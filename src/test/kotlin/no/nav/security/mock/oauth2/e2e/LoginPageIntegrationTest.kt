package no.nav.security.mock.oauth2.e2e

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.jarOf
import no.nav.security.mock.oauth2.testutils.withClasspath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path

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
    fun `built-in login page escapes authorization query parameters`() {
        val server = MockOAuth2Server(OAuth2Config(interactiveLogin = true)).apply { start() }

        try {
            val body =
                client
                    .get(
                        server
                            .authorizationEndpointUrl("default")
                            .authenticationRequest(clientId = "<script>untrusted</script>"),
                    ).use { it.body.string() }

            body shouldContain "&lt;script&gt;untrusted&lt;/script&gt;"
            body shouldNotContain "<script>untrusted</script>"
        } finally {
            server.shutdown()
        }
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

    @Test
    fun `authorization with interactive login and classpath login page path set should return login page from classpath`() {
        val server =
            MockOAuth2Server(
                OAuth2Config(
                    interactiveLogin = true,
                    loginPagePath = "classpath:login.example.html",
                ),
            ).apply { start() }

        try {
            val body = client.get(server.authorizationEndpointUrl("default").authenticationRequest()).use { it.body.string() }

            body shouldContain "Mock OAuth2 Server Example"
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `authorization with interactive login and classpath login page path set should return login page from a jar`(
        @TempDir temporaryDirectory: Path,
    ) {
        val jar = jarOf(temporaryDirectory.resolve("login.jar"), mapOf("pages/login.html" to "<html>Login from jar</html>"))

        withClasspath(jar) {
            val server = MockOAuth2Server(OAuth2Config(interactiveLogin = true, loginPagePath = "classpath:pages/login.html")).apply { start() }
            try {
                client.get(server.authorizationEndpointUrl("default").authenticationRequest()).use {
                    it.code shouldBe 200
                    it.body.string() shouldContain "Login from jar"
                }
            } finally {
                server.shutdown()
            }
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "./src/test/resources/does-not-exists.html",
            "./src/test/resources/",
            "",
            "classpath:does-not-exists.html",
            "classpath:static",
            "classpath:",
        ],
    )
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
