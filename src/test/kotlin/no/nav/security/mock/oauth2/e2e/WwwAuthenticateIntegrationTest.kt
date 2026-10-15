package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.parse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.Headers
import org.junit.jupiter.api.Test

class WwwAuthenticateIntegrationTest {
    private val client = client()

    @Test
    fun `userinfo without bearer token should return Bearer challenge without error`() {
        withMockOAuth2Server {
            client.get(userInfoUrl("default")).asClue {
                it.code shouldBe 401
                it.header("WWW-Authenticate") shouldBe "Bearer"
                it.parse<Map<String, Any>>()["error"] shouldBe "invalid_token"
            }
        }
    }

    @Test
    fun `userinfo with invalid bearer token should return Bearer challenge`() {
        withMockOAuth2Server {
            client
                .get(
                    url = userInfoUrl("default"),
                    headers = Headers.headersOf("Authorization", "Bearer not-a-jwt"),
                ).asClue {
                    it.code shouldBe 401
                    it.header("WWW-Authenticate") shouldBe "Bearer error=\"invalid_token\""
                    it.parse<Map<String, Any>>()["error"] shouldBe "invalid_token"
                }
        }
    }

    @Test
    fun `introspection with invalid Basic authorization header should return Basic challenge`() {
        withMockOAuth2Server {
            client
                .tokenRequest(
                    url = url("default/introspect"),
                    headers = Headers.headersOf("Authorization", "Basic "),
                    parameters = mapOf("token" to "token"),
                ).asClue {
                    it.code shouldBe 401
                    it.header("WWW-Authenticate") shouldBe "Basic realm=\"${issuerUrl("default")}\""
                    it.parse<Map<String, Any>>()["error"] shouldBe "invalid_client"
                }
        }
    }

    @Test
    fun `introspection without authorization header should not return a challenge`() {
        withMockOAuth2Server {
            client
                .tokenRequest(
                    url = url("default/introspect"),
                    parameters = mapOf("token" to "token"),
                ).asClue {
                    it.code shouldBe 401
                    it.header("WWW-Authenticate") shouldBe null
                }
        }
    }
}
