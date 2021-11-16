package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.netty.handler.codec.http.HttpHeaderNames
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.options
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import org.junit.jupiter.api.Test

class CorsHeadersIntegrationTest {
    private val client = client()

    @Test
    fun `preflight response should allow all origin, all methods and all headers`() {
        withMockOAuth2Server {
            client.options(this.baseUrl()).asClue {
                it.code shouldBe 200
                it.headers[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString()] shouldBe "*"
                it.headers[HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString()] shouldBe "*"
                it.headers[HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS.toString()] shouldBe "*"
            }
        }
    }

    @Test
    fun `wellknown response should allow all origins`() {
        withMockOAuth2Server {
            client.get(this.wellKnownUrl("issuer")).asClue {
                it.code shouldBe 200
                it.headers[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString()] shouldBe "*"
            }
        }
    }

    @Test
    fun `jwks response should allow all origins`() {
        withMockOAuth2Server {
            client.get(this.jwksUrl("issuer")).asClue {
                it.code shouldBe 200
                it.headers[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString()] shouldBe "*"
            }
        }
    }

    @Test
    fun `token response should allow all origins`() {
        withMockOAuth2Server {
            val expectedSubject = "expectedSub"
            val issuerId = "idprovider"
            this.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = issuerId, subject = expectedSubject))

            val response = client.tokenRequest(
                this.tokenEndpointUrl(issuerId),
                mapOf(
                    "grant_type" to GrantType.REFRESH_TOKEN.value,
                    "refresh_token" to "canbewhatever",
                    "client_id" to "id",
                    "client_secret" to "secret"
                )
            )

            response.code shouldBe 200
            response.headers[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString()] shouldBe "*"
        }
    }
}
