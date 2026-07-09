package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.ParsedTokenResponse
import no.nav.security.mock.oauth2.testutils.audience
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.shouldBeValidFor
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.testutils.verifyWith
import no.nav.security.mock.oauth2.token.RequestMapping
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import org.junit.jupiter.api.Test

class PasswordGrantIntegrationTest {
    private val client = client()
    private val requiredClaimsWithoutAudience = listOf("sub", "iss", "iat", "exp")

    @Test
    fun `token request with password grant should return accesstoken with username as subject`() {
        withMockOAuth2Server {
            val issuerId = "default"
            val response: ParsedTokenResponse =
                client
                    .tokenRequest(
                        url = this.tokenEndpointUrl(issuerId),
                        basicAuth = Pair("client", "secret"),
                        parameters =
                            mapOf(
                                "grant_type" to GrantType.PASSWORD.value,
                                "scope" to "scope1",
                                "username" to "foo",
                                "password" to "bar",
                            ),
                    ).toTokenResponse()

            response shouldBeValidFor GrantType.PASSWORD
            response.scope shouldContain "scope1"
            response.accessToken.shouldNotBeNull()
            response.accessToken should verifyWith(issuerId, this)
            response.accessToken.subject shouldBe "foo"
            response.accessToken.audience shouldContainExactly listOf("scope1")
            response.idToken.shouldNotBeNull()
            response.idToken should verifyWith(issuerId, this)
            response.idToken.subject shouldBe "foo"
            response.idToken.audience shouldContainExactly listOf("client")
        }
    }

    @Test
    fun `password grant supports requestMapping match on subject and exposes subject template variable`() {
        val issuerId = "default"
        val requestMappingCallback =
            RequestMappingTokenCallback(
                issuerId = issuerId,
                requestMappings =
                    listOf(
                        RequestMapping(
                            requestParam = RequestMappingTokenCallback.SUBJECT_PARAM,
                            match = "admin",
                            claims = mapOf("role" to "admin", "preferred_username" to "\${subject}"),
                        ),
                    ),
            )

        withMockOAuth2Server(
            OAuth2Config(
                tokenCallbacks = setOf(requestMappingCallback),
            ),
        ) {
            val response: ParsedTokenResponse =
                client
                    .tokenRequest(
                        url = this.tokenEndpointUrl(issuerId),
                        basicAuth = Pair("client", "secret"),
                        parameters =
                            mapOf(
                                "grant_type" to GrantType.PASSWORD.value,
                                "scope" to "scope1",
                                "username" to "admin",
                                "password" to "bar",
                            ),
                    ).toTokenResponse()

            response shouldBeValidFor GrantType.PASSWORD
            response.accessToken.shouldNotBeNull()
            response.accessToken should verifyWith(issuerId, this, requiredClaimsWithoutAudience)
            response.accessToken.subject shouldBe "admin"
            response.accessToken.claims shouldContainAll mapOf("role" to "admin", "preferred_username" to "admin")

            response.idToken.shouldNotBeNull()
            response.idToken should verifyWith(issuerId, this, requiredClaimsWithoutAudience)
            response.idToken.subject shouldBe "admin"
            response.idToken.claims shouldContainAll mapOf("role" to "admin", "preferred_username" to "admin")
        }
    }

    @Test
    fun `password grant does not apply subject requestMapping when username does not match`() {
        val issuerId = "default"
        val requestMappingCallback =
            RequestMappingTokenCallback(
                issuerId = issuerId,
                requestMappings =
                    listOf(
                        RequestMapping(
                            requestParam = RequestMappingTokenCallback.SUBJECT_PARAM,
                            match = "admin",
                            claims = mapOf("role" to "admin", "preferred_username" to "\${subject}"),
                        ),
                    ),
            )

        withMockOAuth2Server(
            OAuth2Config(
                tokenCallbacks = setOf(requestMappingCallback),
            ),
        ) {
            val response: ParsedTokenResponse =
                client
                    .tokenRequest(
                        url = this.tokenEndpointUrl(issuerId),
                        basicAuth = Pair("client", "secret"),
                        parameters =
                            mapOf(
                                "grant_type" to GrantType.PASSWORD.value,
                                "scope" to "scope1",
                                "username" to "user",
                                "password" to "bar",
                            ),
                    ).toTokenResponse()

            response shouldBeValidFor GrantType.PASSWORD
            response.accessToken.shouldNotBeNull()
            response.accessToken should verifyWith(issuerId, this, requiredClaimsWithoutAudience)
            response.accessToken.subject shouldBe "user"
            response.accessToken.claims["role"] shouldBe null
            response.accessToken.claims["preferred_username"] shouldBe null

            response.idToken.shouldNotBeNull()
            response.idToken should verifyWith(issuerId, this, requiredClaimsWithoutAudience)
            response.idToken.subject shouldBe "user"
            response.idToken.claims["role"] shouldBe null
            response.idToken.claims["preferred_username"] shouldBe null
        }
    }
}
