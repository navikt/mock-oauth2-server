package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.http.CorsInterceptor.HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS
import no.nav.security.mock.oauth2.http.CorsInterceptor.HeaderNames.ACCESS_CONTROL_ALLOW_HEADERS
import no.nav.security.mock.oauth2.http.CorsInterceptor.HeaderNames.ACCESS_CONTROL_ALLOW_METHODS
import no.nav.security.mock.oauth2.http.CorsInterceptor.HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN
import no.nav.security.mock.oauth2.http.CorsInterceptor.HeaderNames.ACCESS_CONTROL_REQUEST_HEADERS
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.options
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.Headers
import org.junit.jupiter.api.Test

class CorsHeadersIntegrationTest {
    private val client = client()

    private val origin = "https://theorigin"

    @Test
    fun `preflight response should allow specific origin, methods and headers`() {
        withMockOAuth2Server {
            client
                .options(
                    this.baseUrl(),
                    Headers.headersOf(
                        "origin",
                        origin,
                        ACCESS_CONTROL_REQUEST_HEADERS,
                        "X-MY-HEADER",
                    ),
                ).asClue {
                    it.code shouldBe 204
                    it.headers[ACCESS_CONTROL_ALLOW_ORIGIN] shouldBe origin
                    it.headers[ACCESS_CONTROL_ALLOW_METHODS] shouldBe "POST, GET, OPTIONS"
                    it.headers[ACCESS_CONTROL_ALLOW_HEADERS] shouldBe "X-MY-HEADER"
                    it.headers[ACCESS_CONTROL_ALLOW_CREDENTIALS] shouldBe "true"
                }
        }
    }

    @Test
    fun `wellknown response should allow origin`() {
        withMockOAuth2Server {
            client
                .get(
                    this.wellKnownUrl("issuer"),
                    Headers.headersOf("origin", origin),
                ).asClue {
                    it.code shouldBe 200
                    it.headers[ACCESS_CONTROL_ALLOW_ORIGIN] shouldBe origin
                    it.headers[ACCESS_CONTROL_ALLOW_CREDENTIALS] shouldBe "true"
                }
        }
    }

    @Test
    fun `jwks response should allow all origins`() {
        withMockOAuth2Server {
            client
                .get(
                    this.jwksUrl("issuer"),
                    Headers.headersOf("origin", origin),
                ).asClue {
                    it.code shouldBe 200
                    it.headers[ACCESS_CONTROL_ALLOW_ORIGIN] shouldBe origin
                    it.headers[ACCESS_CONTROL_ALLOW_CREDENTIALS] shouldBe "true"
                }
        }
    }

    @Test
    fun `token response should allow all origins`() {
        withMockOAuth2Server {
            val expectedSubject = "expectedSub"
            val issuerId = "idprovider"
            this.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = issuerId, subject = expectedSubject))

            val response =
                client.tokenRequest(
                    this.tokenEndpointUrl(issuerId),
                    Headers.headersOf("origin", origin),
                    mapOf(
                        "grant_type" to GrantType.REFRESH_TOKEN.value,
                        "refresh_token" to "canbewhatever",
                        "client_id" to "id",
                        "client_secret" to "secret",
                    ),
                )

            response.code shouldBe 200
            response.headers[ACCESS_CONTROL_ALLOW_ORIGIN] shouldBe origin
            response.headers[ACCESS_CONTROL_ALLOW_CREDENTIALS] shouldBe "true"
        }
    }
}
