package no.nav.security.mock.oauth2

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.post
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class MockOAuth2ServerTest {
    private val client: OkHttpClient = client()

    @Test
    fun `server takeRequest() should return sent request`() {
        withMockOAuth2Server {
            client.post(this.baseUrl(), mapOf("param1" to "value1")).body?.close()

            this.takeRequest().asClue {
                it.requestUrl shouldBe this.baseUrl()
                it.body.readUtf8() shouldBe "param1=value1"
            }

            client.post(
                this.tokenEndpointUrl("test"),
                mapOf(
                    "client_id" to "client",
                    "client_secret" to "sec",
                    "grant_type" to "client_credentials",
                    "scope" to "scope1"
                )
            ).body?.close()

            this.takeRequest().asClue {
                it.requestUrl shouldBe this.tokenEndpointUrl("test")
                it.body.readUtf8() shouldBe "client_id=client&client_secret=sec&grant_type=client_credentials&scope=scope1"
            }
        }
    }

    @Test
    fun `takeRequest should time out if no request is received`() {
        withMockOAuth2Server {
            shouldThrow<java.lang.RuntimeException> {
                this.takeRequest(5, TimeUnit.MILLISECONDS)
            }
            val url = this.wellKnownUrl("1")
            client.get(url)
            this.takeRequest().requestUrl shouldBe url
        }
    }
}
