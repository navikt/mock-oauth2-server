package examples.kotlin.ktor.login

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OAuth2LoginAppTest {
    private val server = MockOAuth2Server()

    @BeforeEach
    fun setup() = server.start()

    @AfterEach
    fun after() = server.shutdown()

    @Test
    fun `dss`() {
        server.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = "google", subject = "googleSubject"))
        server.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = "github", subject = "githubSubject"))

        withTestApplication({
            module(authConfig())
        }) {
            with(
                handleRequest(HttpMethod.Get, "/login/google")
            ) {
                response.status() shouldBe HttpStatusCode.OK
                //response.content shouldBe "hello1 foo from issuer ${mockOAuth2Server.issuerUrl("provider1")}"
            }
        }
    }

    private fun authConfig() = AuthConfig(
        listOf(
            AuthConfig.IdProvider(
                name = "google",
                authorizationEndpoint = server.authorizationEndpointUrl("google").toString(),
                tokenEndpoint = server.tokenEndpointUrl("google").toString(),
            ),
            AuthConfig.IdProvider(
                name = "github",
                authorizationEndpoint = server.authorizationEndpointUrl("github").toString(),
                tokenEndpoint = server.tokenEndpointUrl("github").toString(),
            )
        )
    )
}
