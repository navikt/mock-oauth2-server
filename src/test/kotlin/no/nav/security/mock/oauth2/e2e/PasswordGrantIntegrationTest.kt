package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.mock.oauth2.testutils.ParsedTokenResponse
import no.nav.security.mock.oauth2.testutils.audience
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.shouldBeValidFor
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.testutils.verifyWith
import no.nav.security.mock.oauth2.withMockOAuth2Server
import org.junit.jupiter.api.Test

class PasswordGrantIntegrationTest {
    private val client = client()

    @Test
    fun `token request with password grant should return accesstoken with username as subject`() {
        withMockOAuth2Server {
            val issuerId = "default"
            val response: ParsedTokenResponse = client.tokenRequest(
                url = this.tokenEndpointUrl(issuerId),
                basicAuth = Pair("client", "secret"),
                parameters = mapOf(
                    "grant_type" to GrantType.PASSWORD.value,
                    "scope" to "scope1",
                    "username" to "foo",
                    "password" to "bar"
                )
            ).toTokenResponse()

            response shouldBeValidFor GrantType.PASSWORD
            response.scope shouldContain "scope1"
            response.accessToken.shouldNotBeNull()
            response.accessToken should verifyWith(issuerId, this)
            response.accessToken.subject shouldBe "foo"
            response.accessToken.audience shouldContainExactly listOf("scope1")
        }
    }
}
