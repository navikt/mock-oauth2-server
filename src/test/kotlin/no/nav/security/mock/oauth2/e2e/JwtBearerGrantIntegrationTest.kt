package no.nav.security.mock.oauth2.e2e

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.GrantType
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.tokenRequest
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.OkHttpClient
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JwtBearerGrantIntegrationTest {

    private val client: OkHttpClient = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .build()

    @Test
    fun `token request with JwtBearerGrant should exchange assertion with a new token containing many of the same claims`() {
        withMockOAuth2Server {
            val signedJWT = this.issueToken(
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

            val response: Response = client.tokenRequest(
                url = this.tokenEndpointUrl("aad"),
                userPwd = Pair("client1", "secret"),
                parameters = mapOf(
                    "grant_type" to GrantType.JWT_BEARER.value,
                    "scope" to "scope1",
                    "assertion" to signedJWT.serialize()
                )
            )

            assertThat(response.code).isEqualTo(200)
            val tokenResponse: OAuth2TokenResponse = jacksonObjectMapper().readValue(checkNotNull(response.body?.string()))
            assertThat(tokenResponse.accessToken).isNotNull
            assertThat(tokenResponse.idToken).isNull()
            assertThat(tokenResponse.expiresIn).isGreaterThan(0)
            assertThat(tokenResponse.scope).contains("scope1")
            assertThat(tokenResponse.tokenType).isEqualTo("Bearer")
            val accessToken: SignedJWT = SignedJWT.parse(tokenResponse.accessToken)
            assertThat(accessToken.jwtClaimsSet.audience).containsExactly("scope1")
            assertThat(accessToken.jwtClaimsSet.issuer).endsWith("aad")

            assertThat(accessToken.jwtClaimsSet.getClaim("claim1")).isEqualTo("value1")
            assertThat(accessToken.jwtClaimsSet.getClaim("claim2")).isEqualTo("value2")
        }
    }
}
