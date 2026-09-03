package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.Headers
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class WellKnownIntegrationTest {
    private val client = client()

    @Test
    fun `get to well-known url should return oauth2 server metadata`() {
        withMockOAuth2Server {
            val response = client.get(this.wellKnownUrl("default"))
            val body = response.body.string()
            response.code shouldBe 200
            body shouldNotBe null
            jacksonObjectMapper().readValue<Map<String, Any>>(body).keys.asClue {
                it shouldContainExactlyInAnyOrder
                    listOf(
                        "issuer",
                        "authorization_endpoint",
                        "end_session_endpoint",
                        "revocation_endpoint",
                        "token_endpoint",
                        "userinfo_endpoint",
                        "jwks_uri",
                        "introspection_endpoint",
                        "response_modes_supported",
                        "response_types_supported",
                        "subject_types_supported",
                        "id_token_signing_alg_values_supported",
                        "code_challenge_methods_supported",
                    )
            }
        }
    }

    @Test
    fun `get to well-known url with IPv6 host header should not crash and reflect the proxy-aware issuer`() {
        withMockOAuth2Server {
            val port = this.wellKnownUrl("default").port
            val response =
                client.get(
                    this.wellKnownUrl("default"),
                    headers = Headers.headersOf("host", "[::1]:$port"),
                )
            val body = response.body.string()
            response.code shouldBe 200
            jacksonObjectMapper().readValue<Map<String, Any>>(body).asClue { metadata ->
                metadata["issuer"] shouldBe "http://[::1]:$port/default"
                listOf(
                    "authorization_endpoint",
                    "end_session_endpoint",
                    "revocation_endpoint",
                    "token_endpoint",
                    "userinfo_endpoint",
                    "jwks_uri",
                    "introspection_endpoint",
                ).forEach { (metadata[it] as String) shouldStartWith "http://[::1]:$port/default/" }
            }
        }
    }
}
