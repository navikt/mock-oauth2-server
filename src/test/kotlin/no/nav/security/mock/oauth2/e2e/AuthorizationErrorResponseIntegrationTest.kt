package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.http.WellKnown
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.parse
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class AuthorizationErrorResponseIntegrationTest {
    private val client = client()

    @Test
    fun `authorize request without response_type should redirect invalid_request to the client`() {
        withMockOAuth2Server {
            client.get(authorizeUrl("response_type" to null)).use {
                it.code shouldBe 302
                val location = checkNotNull(it.headers["location"])
                location shouldStartWith "http://mycallback"
                location.toHttpUrl().asClue { url ->
                    url.queryParameter("error") shouldBe "invalid_request"
                    url.queryParameter("state") shouldBe "s1"
                }
            }
        }
    }

    @Test
    fun `authorize request with unsupported response_type should redirect unsupported_response_type to the client`() {
        withMockOAuth2Server {
            client.get(authorizeUrl("response_type" to "token")).use {
                it.code shouldBe 302
                val location = checkNotNull(it.headers["location"])
                location shouldStartWith "http://mycallback"
                location shouldContain "error=unsupported_response_type"
                location shouldContain "state=s1"
            }
        }
    }

    @Test
    fun `authorize error with response_mode form_post should be posted back to the client`() {
        withMockOAuth2Server {
            client.get(authorizeUrl("response_type" to "token", "response_mode" to "form_post")).use {
                it.code shouldBe 200
                val body = it.body.string()
                body shouldContain "action=\"http://mycallback\""
                body shouldContain "name=\"error\" value=\"unsupported_response_type\""
                body shouldContain "name=\"state\" value=\"s1\""
                body shouldNotContain "name=\"code\""
            }
        }
    }

    @Test
    fun `authorize error with response_mode form_post should escape HTML in state`() {
        withMockOAuth2Server {
            client.get(authorizeUrl("response_type" to "token", "response_mode" to "form_post", "state" to "\"><script>alert(1)</script>")).use {
                it.code shouldBe 200
                val body = it.body.string()
                body shouldNotContain "<script>"
                body shouldContain "name=\"state\" value=\"&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;\""
            }
        }
    }

    @Test
    fun `authorize error without redirect_uri should return a JSON error`() {
        withMockOAuth2Server {
            client.get(authorizeUrl("response_type" to null, "redirect_uri" to null)).use {
                it.code shouldBe 400
                it.headers["location"] shouldBe null
                it.body.string() shouldContain "invalid_request"
            }
        }
    }

    @Test
    fun `authorize error without client_id should return a JSON error`() {
        withMockOAuth2Server {
            client.get(authorizeUrl("client_id" to null)).use {
                it.code shouldBe 400
                it.headers["location"] shouldBe null
                it.body.string() shouldContain "invalid_request"
            }
        }
    }

    @Test
    fun `authorize error with form_post to a non-HTTP callback should return a JSON error`() {
        withMockOAuth2Server {
            client.get(authorizeUrl("response_type" to "token", "response_mode" to "form_post", "redirect_uri" to "myapp://callback")).use {
                it.code shouldBe 400
                it.body.string() shouldNotContain "<form"
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
