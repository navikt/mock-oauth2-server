package no.nav.security.mock.oauth2.e2e

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.ParsedTokenResponse
import no.nav.security.mock.oauth2.audience
import no.nav.security.mock.oauth2.claims
import no.nav.security.mock.oauth2.grant.TOKEN_EXCHANGE
import no.nav.security.mock.oauth2.issuer
import no.nav.security.mock.oauth2.toTokenResponse
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.tokenRequest
import no.nav.security.mock.oauth2.verify
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

class TokenExchangeGrantIntegrationTest {

    private val client: OkHttpClient = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .build()

    // TODO: use client_assertion (private_key_jwt) instead of basic auth as tokenx in NAV only supports this
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
            val issuerId = "tokenx"
            val response: ParsedTokenResponse = client.tokenRequest(
                url = this.tokenEndpointUrl(issuerId),
                basicAuth = Pair("client1", "secret"),
                parameters = mapOf(
                    "grant_type" to TOKEN_EXCHANGE.value,
                    "subject_token_type" to "urn:ietf:params:oauth:token-type:jwt",
                    "subject_token" to initialToken.serialize(),
                    "audience" to "targetAudience",
                )
            ).toTokenResponse()

            response.status shouldBe 200
            response.expiresIn shouldBeGreaterThan 0
            response.scope shouldBe null
            response.tokenType shouldBe "Bearer"
            response.accessToken shouldNotBe null
            response.idToken shouldBe null
            response.refreshToken shouldBe null
            response.issuedTokenType shouldBe "urn:ietf:params:oauth:token-type:access_token"

            response.accessToken?.verify(this.issuerUrl(issuerId), this.jwksUrl(issuerId))

            response.accessToken?.audience shouldContainExactly listOf("targetAudience")
            response.accessToken?.issuer shouldBe this.issuerUrl(issuerId).toString()
            response.accessToken?.claims?.get("claim1") shouldBe "value1"
            response.accessToken?.claims?.get("claim2") shouldBe "value2"
        }
    }
}
