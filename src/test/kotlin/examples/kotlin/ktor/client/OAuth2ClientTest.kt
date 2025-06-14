package examples.kotlin.ktor.client

import com.auth0.jwt.JWT
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator

internal class OAuth2ClientTest {
    private val server = MockOAuth2Server()

    @BeforeEach
    fun setup() = server.start()

    @AfterEach
    fun after() = server.shutdown()

    @Test
    fun `client credentials grant`() {
        runBlocking {
            server.enqueueCallback(DefaultOAuth2TokenCallback(subject = "client1", audience = listOf("targetScope")))

            val tokenResponse =
                httpClient.clientCredentialsGrant(
                    url = server.tokenEndpointUrl("default").toString(),
                    auth = Auth.clientSecretBasic("client1", "secret"),
                    scope = "targetScope",
                )

            tokenResponse.asClue {
                it
                    .body<TokenResponse>()
                    .accessToken
                    .asDecodedJWT()
                    .subject shouldBe "client1"
                it
                    .body<TokenResponse>()
                    .accessToken
                    .asDecodedJWT()
                    .audience
                    .shouldContainExactly("targetScope")
            }
        }
    }

    @Test
    fun `onbehalfof grant`() {
        runBlocking {
            val initialToken = server.issueToken(subject = "enduser")
            val tokenEndpointUrl = server.tokenEndpointUrl("default").toString()
            val issuerUrl = server.issuerUrl("default").toString()
            val tokenResponse =
                httpClient.onBehalfOfGrant(
                    url = tokenEndpointUrl,
                    auth =
                        Auth.privateKeyJwt(
                            keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair(),
                            clientId = "client1",
                            audience = issuerUrl,
                        ),
                    token = initialToken.serialize(),
                    scope = "targetScope",
                )

            tokenResponse.asClue {
                it
                    .body<TokenResponse>()
                    .accessToken
                    .asDecodedJWT()
                    .subject shouldBe "enduser"
                it
                    .body<TokenResponse>()
                    .accessToken
                    .asDecodedJWT()
                    .audience
                    .shouldContainExactly("targetScope")
            }
        }
    }

    private fun String.asDecodedJWT() = JWT.decode(this)
}
