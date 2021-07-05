package examples.kotlin.ktor.resourceserver

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.withMockOAuth2Server
import org.junit.jupiter.api.Test

class OAuth2ResourceServerAppTest {

    @Test
    fun `http get to secured endpoint without token should return 401`() {
        withMockOAuth2Server {
            val authConfig = authConfig()
            withTestApplication({
                module(authConfig)
            }) {
                with(
                    handleRequest(HttpMethod.Get, "/hello1")
                ) {
                    response.status() shouldBe HttpStatusCode.Unauthorized
                }
            }
        }
    }

    @Test
    fun `http get to hello1 endpoint should only accept tokens from provider1 with correct claims`() {
        withMockOAuth2Server {
            val mockOAuth2Server = this
            val authConfig = authConfig()
            withTestApplication({
                module(authConfig)
            }) {
                with(
                    handleRequest(HttpMethod.Get, "/hello1") {
                        addHeader("Authorization", "Bearer ${mockOAuth2Server.tokenFromProvider1()}")
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.OK
                    response.content shouldBe "hello1 foo from issuer ${mockOAuth2Server.issuerUrl("provider1")}"
                }
            }
        }
    }

    @Test
    fun `http get to hello2 endpoint should only accept tokens from provider2 with correct claims`() {
        withMockOAuth2Server {
            val mockOAuth2Server = this
            val authConfig = authConfig()
            withTestApplication({
                module(authConfig)
            }) {
                with(
                    handleRequest(HttpMethod.Get, "/hello2") {
                        addHeader("Authorization", "Bearer ${mockOAuth2Server.tokenFromProvider2()}")
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.OK
                    response.content shouldBe "hello2 foo from issuer ${mockOAuth2Server.issuerUrl("provider2")}"
                }
            }
        }
    }

    private fun MockOAuth2Server.tokenFromProvider1() =
        issueToken(
            "provider1",
            "foo",
            "scopeFromProvider1",
            mapOf("groups" to listOf("group1", "group2"))
        ).serialize()

    private fun MockOAuth2Server.tokenFromProvider2() =
        issueToken(
            "provider2",
            "foo",
            "scopeFromProvider2",
            mapOf("stringClaim" to "1")
        ).serialize()

    private fun MockOAuth2Server.authConfig() =
        AuthConfig(
            mapOf(
                "provider1" to AuthConfig.TokenProvider(
                    wellKnownUrl = wellKnownUrl("provider1").toString(),
                    acceptedAudience = "scopeFromProvider1",
                    requiredClaims = mapOf("groups" to listOf("group2"))
                ),
                "provider2" to AuthConfig.TokenProvider(
                    wellKnownUrl = wellKnownUrl("provider2").toString(),
                    acceptedAudience = "scopeFromProvider2",
                    requiredClaims = mapOf("stringClaim" to "1")
                )
            )
        )
}
