package examples.kotlin.ktor.resourceserver

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.withMockOAuth2Server
import org.junit.jupiter.api.Test

class OAuth2ResourceServerAppTest {

    @Test
    fun `http get to secured endpoint without token should return 401`() {
        withMockOAuth2Server {
            val authConfig = authConfig()

            testApplication {
                this.application {
                    module(authConfig)
                }
                val response = client.get("/hello1")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    @Test
    fun `http get to hello1 endpoint should only accept tokens from provider1 with correct claims`() {
        withMockOAuth2Server {
            val mockOAuth2Server = this
            val authConfig = authConfig()
            testApplication {
                this.application {
                    module(authConfig)
                }
                val response = client.get("/hello1"){
                header("Authorization", "Bearer ${mockOAuth2Server.tokenFromProvider1()}")
                }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "hello1 foo from issuer ${mockOAuth2Server.issuerUrl("provider1")}"
            }
        }
    }

    @Test
    fun `http get to hello2 endpoint should only accept tokens from provider2 with correct claims`() {
        withMockOAuth2Server {
            val mockOAuth2Server = this
            val authConfig = authConfig()
            testApplication {
                this.application {
                    module(authConfig)
                }

                val response = client.get("/hello2"){
                    header("Authorization", "Bearer ${mockOAuth2Server.tokenFromProvider2()}")
                }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "hello2 foo from issuer ${mockOAuth2Server.issuerUrl("provider2")}"
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
