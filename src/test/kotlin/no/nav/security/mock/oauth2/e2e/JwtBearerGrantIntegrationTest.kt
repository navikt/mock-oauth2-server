package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.mock.oauth2.testutils.ParsedTokenResponse
import no.nav.security.mock.oauth2.testutils.audience
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.shouldBeValidFor
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.testutils.verifyWith
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import org.junit.jupiter.api.Test

class JwtBearerGrantIntegrationTest {
    private val client = client()

    @Test
    fun `token request with JwtBearerGrant should exchange assertion with a new token containing many of the same claims`() {
        withMockOAuth2Server {
            val initialSubject = "yolo"
            val initialToken =
                this.issueToken(
                    issuerId = "idprovider",
                    clientId = "client1",
                    tokenCallback =
                        DefaultOAuth2TokenCallback(
                            issuerId = "idprovider",
                            subject = initialSubject,
                            claims =
                                mapOf(
                                    "claim1" to "value1",
                                    "claim2" to "value2",
                                ),
                        ),
                )
            val issuerId = "aad"
            val response: ParsedTokenResponse =
                client.tokenRequest(
                    url = this.tokenEndpointUrl(issuerId),
                    basicAuth = Pair("client1", "secret"),
                    parameters =
                        mapOf(
                            "grant_type" to GrantType.JWT_BEARER.value,
                            "scope" to "scope1",
                            "assertion" to initialToken.serialize(),
                        ),
                ).toTokenResponse()

            response shouldBeValidFor GrantType.JWT_BEARER
            response.scope shouldContain "scope1"
            response.issuedTokenType shouldBe null
            response.accessToken.shouldNotBeNull()
            response.accessToken should verifyWith(issuerId, this)
            response.accessToken.subject shouldBe initialSubject
            response.accessToken.audience shouldContainExactly listOf("scope1")
            response.accessToken.claims["claim1"] shouldBe "value1"
            response.accessToken.claims["claim2"] shouldBe "value2"
        }
    }

    @Test
    fun `token request with JwtBearerGrant should exchange assertion with a new token with scope specified in assertion claim or request params`() {
        withMockOAuth2Server {
            val initialSubject = "mysub"
            val initialToken =
                this.issueToken(
                    issuerId = "idprovider",
                    clientId = "client1",
                    tokenCallback =
                        DefaultOAuth2TokenCallback(
                            issuerId = "idprovider",
                            subject = initialSubject,
                            audience = emptyList(),
                            claims =
                                mapOf(
                                    "claim1" to "value1",
                                    "claim2" to "value2",
                                    "scope" to "ascope",
                                    "resource" to "aud1",
                                ),
                        ),
                )

            initialToken.audience.shouldBeEmpty()

            val issuerId = "aad"

            this.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = issuerId, audience = emptyList()))

            val response: ParsedTokenResponse =
                client.tokenRequest(
                    url = this.tokenEndpointUrl(issuerId),
                    basicAuth = Pair("client1", "secret"),
                    parameters =
                        mapOf(
                            "grant_type" to GrantType.JWT_BEARER.value,
                            "assertion" to initialToken.serialize(),
                        ),
                ).toTokenResponse()

            response shouldBeValidFor GrantType.JWT_BEARER
            response.scope shouldContain "ascope"
            response.issuedTokenType shouldBe null
            response.accessToken.shouldNotBeNull()
            response.accessToken should verifyWith(issuerId, this, listOf("sub", "iss", "iat", "exp"))
            response.accessToken.subject shouldBe initialSubject
            response.accessToken.audience.shouldBeEmpty()
            response.accessToken.claims["claim1"] shouldBe "value1"
            response.accessToken.claims["claim2"] shouldBe "value2"
        }
    }
}
