package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.http.WellKnown
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.parse
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.HttpUrl
import org.junit.jupiter.api.Test

class AuthorizeResponseTypeIntegrationTest {
    private val client = client()

    @Test
    fun `authorize request with hybrid response_type should return unsupported_response_type`() {
        withMockOAuth2Server {
            client.get(authorizeUrl("response_type" to "code id_token")).use {
                it.code shouldBe 400
                it.body.string() shouldContain "unsupported_response_type"
            }
        }
    }

    @Test
    fun `successful authorization response with response_mode form_post should not be cached`() {
        withMockOAuth2Server {
            client.get(authorizeUrl("response_mode" to "form_post")).use {
                it.code shouldBe 200
                it.body.string() shouldContain "name=\"code\""
                it.headers["Cache-Control"] shouldBe "no-store"
                it.headers["Pragma"] shouldBe "no-cache"
            }
        }
    }

    @Test
    fun `well-known should only advertise the code response type`() {
        withMockOAuth2Server {
            client.get(wellKnownUrl("default")).parse<WellKnown>().asClue {
                it.responseTypesSupported shouldContainExactly listOf("code")
            }
        }
    }

    private fun MockOAuth2Server.authorizeUrl(vararg overrides: Pair<String, String?>): HttpUrl {
        val params =
            linkedMapOf<String, String?>(
                "client_id" to "client1",
                "response_type" to "code",
                "redirect_uri" to "http://mycallback",
                "scope" to "openid",
                "state" to "s1",
                "nonce" to "n1",
            ).apply { putAll(overrides) }
        return authorizationEndpointUrl("default")
            .newBuilder()
            .apply { params.forEach { (key, value) -> if (value != null) addQueryParameter(key, value) } }
            .build()
    }
}
