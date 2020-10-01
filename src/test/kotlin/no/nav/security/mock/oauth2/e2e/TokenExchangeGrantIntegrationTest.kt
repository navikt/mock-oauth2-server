package no.nav.security.mock.oauth2.e2e

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.grant.TOKEN_EXCHANGE
import no.nav.security.mock.oauth2.testutils.ClientAssertionType
import no.nav.security.mock.oauth2.testutils.ParsedTokenResponse
import no.nav.security.mock.oauth2.testutils.SubjectTokenType
import no.nav.security.mock.oauth2.testutils.audience
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.clientAssertion
import no.nav.security.mock.oauth2.testutils.issuer
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.testutils.verify
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

class TokenExchangeGrantIntegrationTest {

    private val client: OkHttpClient = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .build()

    @Test
    fun `token request with token exchange grant should exchange subject_token with a new token containing many of the same claims`() {
        withMockOAuth2Server {
            val initialSubject = "yolo"
            val initialToken = this.issueToken(
                issuerId = "idprovider",
                clientId = "initialClient",
                tokenCallback = DefaultOAuth2TokenCallback(
                    issuerId = "idprovider",
                    subject = initialSubject,
                    claims = mapOf(
                        "claim1" to "value1",
                        "claim2" to "value2",
                    )
                )
            )

            val issuerId = "tokenx"
            val tokenEndpointUrl = this.tokenEndpointUrl(issuerId)
            val clientAssertion = clientAssertion("tokenExchangeClient", tokenEndpointUrl.toUrl()).serialize()
            val targetAudienceForToken = "targetAudience"

            val response: ParsedTokenResponse = client.tokenRequest(
                url = tokenEndpointUrl,
                parameters = mapOf(
                    "grant_type" to TOKEN_EXCHANGE.value,
                    "client_assertion_type" to ClientAssertionType.JWT_BEARER,
                    "client_assertion" to clientAssertion,
                    "subject_token_type" to SubjectTokenType.TOKEN_TYPE_JWT,
                    "subject_token" to initialToken.serialize(),
                    "audience" to targetAudienceForToken
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

            response.accessToken?.subject shouldBe initialSubject
            response.accessToken?.audience shouldContainExactly listOf(targetAudienceForToken)
            response.accessToken?.issuer shouldBe this.issuerUrl(issuerId).toString()
            response.accessToken?.claims?.get("claim1") shouldBe "value1"
            response.accessToken?.claims?.get("claim2") shouldBe "value2"
        }
    }
}
