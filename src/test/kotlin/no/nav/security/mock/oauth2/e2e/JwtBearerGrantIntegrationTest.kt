package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.mock.oauth2.ParsedTokenResponse
import no.nav.security.mock.oauth2.audience
import no.nav.security.mock.oauth2.claims
import no.nav.security.mock.oauth2.issuer
import no.nav.security.mock.oauth2.toTokenResponse
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.tokenRequest
import no.nav.security.mock.oauth2.verify
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

class JwtBearerGrantIntegrationTest {

    private val client: OkHttpClient = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .build()

    @Test
    fun `token request with JwtBearerGrant should exchange assertion with a new token containing many of the same claims`() {
        withMockOAuth2Server {
            val initialToken = this.issueToken(
                issuerId = "idprovider",
                clientId = "client1",
                tokenCallback = DefaultOAuth2TokenCallback(
                    issuerId = "idprovider",
                    subject = "mysub",
                    claims = mapOf(
                        "claim1" to "value1",
                        "claim2" to "value2",
                    )
                )
            )

            val response: ParsedTokenResponse = client.tokenRequest(
                url = this.tokenEndpointUrl("aad"),
                basicAuth = Pair("client1", "secret"),
                parameters = mapOf(
                    "grant_type" to GrantType.JWT_BEARER.value,
                    "scope" to "scope1",
                    "assertion" to initialToken.serialize()
                )
            ).toTokenResponse()

            response.status shouldBe 200
            response.expiresIn shouldBeGreaterThan 0
            response.scope shouldContain "scope1"
            response.tokenType shouldBe "Bearer"
            response.accessToken shouldNotBe null
            response.idToken shouldBe null
            response.refreshToken shouldBe null
            response.issuedTokenType shouldBe null

            response.accessToken?.verify(this.issuerUrl("aad"), this.jwksUrl("aad"))

            response.accessToken?.audience shouldContainExactly listOf("scope1")
            response.accessToken?.issuer shouldBe this.issuerUrl("aad").toString()
            response.accessToken?.claims?.get("claim1") shouldBe "value1"
            response.accessToken?.claims?.get("claim2") shouldBe "value2"
        }
    }
}
