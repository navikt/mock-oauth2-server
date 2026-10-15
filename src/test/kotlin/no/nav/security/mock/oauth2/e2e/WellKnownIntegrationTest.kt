package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.http.WellKnown
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.parse
import no.nav.security.mock.oauth2.token.KeyProvider
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.Headers
import okhttp3.HttpUrl
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
    fun `RFC 8414 metadata url should return the same document as the issuer suffix url`() {
        withMockOAuth2Server {
            val rfc8414 = metadata(this.url(".well-known/oauth-authorization-server/default"))
            rfc8414 shouldBe metadata(this.oauth2AuthorizationServerMetadataUrl("default"))
            rfc8414["issuer"] shouldBe this.issuerUrl("default").toString()
        }
    }

    @Test
    fun `RFC 8414 metadata url should support issuer paths with several segments`() {
        withMockOAuth2Server {
            val rfc8414 = metadata(this.url(".well-known/oauth-authorization-server/a/b/c"))
            rfc8414 shouldBe metadata(this.url("a/b/c/.well-known/oauth-authorization-server"))
            rfc8414["issuer"] shouldBe this.url("a/b/c").toString()
            (rfc8414["token_endpoint"] as String) shouldStartWith this.url("a/b/c").toString() + "/"
        }
    }

    @Test
    fun `well-known should only advertise the default signing algorithm`() {
        withMockOAuth2Server {
            client.get(this.wellKnownUrl("default")).parse<WellKnown>().asClue {
                it.idTokenSigningAlgValuesSupported shouldContainExactly listOf("RS256")
            }
        }
    }

    @Test
    fun `well-known should advertise the configured signing algorithm used for issued tokens`() {
        val config =
            OAuth2Config(
                tokenProvider = OAuth2TokenProvider(KeyProvider(initialKeys = emptyList(), algorithm = "ES256")),
            )
        withMockOAuth2Server(config) {
            client.get(this.wellKnownUrl("default")).parse<WellKnown>().asClue {
                it.idTokenSigningAlgValuesSupported shouldContainExactly listOf("ES256")
            }
            this
                .issueToken("default")
                .header.algorithm.name shouldBe "ES256"
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

    private fun metadata(url: HttpUrl): Map<String, Any> =
        client.get(url).use {
            it.code shouldBe 200
            jacksonObjectMapper().readValue<Map<String, Any>>(it.body.string())
        }
}
