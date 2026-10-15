package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.Response
import org.junit.jupiter.api.Test

class TokenResponseCacheHeadersIntegrationTest {
    private val client = client()

    @Test
    fun `client_credentials token response should not be cached`() {
        withMockOAuth2Server {
            client
                .tokenRequest(
                    tokenEndpointUrl("default"),
                    "client1" to "secret",
                    mapOf("grant_type" to GrantType.CLIENT_CREDENTIALS.value, "scope" to "scope1"),
                ).asClue {
                    it.code shouldBe 200
                    it.shouldHaveNoCacheHeaders()
                }
        }
    }

    @Test
    fun `password token response should not be cached`() {
        withMockOAuth2Server {
            client
                .tokenRequest(
                    tokenEndpointUrl("default"),
                    "client1" to "secret",
                    mapOf("grant_type" to GrantType.PASSWORD.value, "scope" to "scope1", "username" to "foo", "password" to "bar"),
                ).asClue {
                    it.code shouldBe 200
                    it.shouldHaveNoCacheHeaders()
                }
        }
    }

    @Test
    fun `token error response should not be cached`() {
        withMockOAuth2Server {
            client
                .tokenRequest(
                    tokenEndpointUrl("default"),
                    mapOf("grant_type" to "unknown", "client_id" to "client1"),
                ).asClue {
                    it.code shouldBe 400
                    it.shouldHaveNoCacheHeaders()
                }
        }
    }

    @Test
    fun `well-known response should keep its current headers`() {
        withMockOAuth2Server {
            client.get(wellKnownUrl("default")).asClue {
                it.code shouldBe 200
                it.header("Cache-Control") shouldBe null
                it.header("Pragma") shouldBe null
            }
        }
    }

    private fun Response.shouldHaveNoCacheHeaders() {
        header("Cache-Control") shouldBe "no-store"
        header("Pragma") shouldBe "no-cache"
    }
}
