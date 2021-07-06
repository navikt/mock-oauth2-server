package examples.kotlin.ktor.login

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.client.request.get
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.IOException
import java.net.ServerSocket

internal class OAuth2LoginAppTest {
    private val mockOAuth2Server = MockOAuth2Server()

    @BeforeEach
    fun setup() = mockOAuth2Server.start()

    @AfterEach
    fun after() = mockOAuth2Server.shutdown()

    @Test
    fun `login with google or github should return appropriate subject`() {
        mockOAuth2Server.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = "google", subject = "googleSubject"))
        mockOAuth2Server.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = "github", subject = "githubSubject"))

        val port = randomPort()

        withEmbeddedServer(
            { module(authConfig()) },
            port
        ) {
            get<String>("http://localhost:$port/login/google").asClue {
                it shouldBe "welcome googleSubject"
            }
            get<String>("http://localhost:$port/login/github").asClue {
                it shouldBe "welcome githubSubject"
            }
        }
    }

    private inline fun <reified R> get(url: String): R = runBlocking { httpClient.get(url) }

    private fun <R> withEmbeddedServer(
        moduleFunction: Application.() -> Unit,
        port: Int,
        test: ApplicationEngine.() -> R
    ): R {
        val engine = embeddedServer(Netty, port = port) {
            moduleFunction(this)
        }
        engine.start()
        try {
            return engine.test()
        } finally {
            engine.stop(0L, 0L)
        }
    }

    private fun randomPort() = try {
        ServerSocket(0).use { serverSocket -> serverSocket.localPort }
    } catch (e: IOException) {
        fail("Port is not available")
    }

    private fun authConfig() = AuthConfig(
        listOf(
            AuthConfig.IdProvider(
                name = "google",
                authorizationEndpoint = mockOAuth2Server.authorizationEndpointUrl("google").toString(),
                tokenEndpoint = mockOAuth2Server.tokenEndpointUrl("google").toString(),
            ),
            AuthConfig.IdProvider(
                name = "github",
                authorizationEndpoint = mockOAuth2Server.authorizationEndpointUrl("github").toString(),
                tokenEndpoint = mockOAuth2Server.tokenEndpointUrl("github").toString(),
            )
        )
    )
}
