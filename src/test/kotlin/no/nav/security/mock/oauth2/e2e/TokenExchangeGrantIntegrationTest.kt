package no.nav.security.mock.oauth2.e2e

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jwt.SignedJWT
import no.nav.security.mock.oauth2.grant.TOKEN_EXCHANGE
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.tokenRequest
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.OkHttpClient
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TokenExchangeGrantIntegrationTest {

    private val client: OkHttpClient = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .build()

    // TODO: use client_assertion (private_key_jwt) instead of basic auth as tokenx in NAV only supports this
    @Test
    fun `token request with TokenExchange grant should exchange subject_token with a new token containing many of the same claims`() {
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
                url = this.tokenEndpointUrl("tokenx"),
                userPwd = Pair("client1", "secret"),
                parameters = mapOf(
                    "grant_type" to TOKEN_EXCHANGE.value,
                    "subject_token_type" to "urn:ietf:params:oauth:token-type:jwt",
                    "subject_token" to signedJWT.serialize(),
                    "audience" to "desiredaudience",
                )
            )

            assertThat(response.code).isEqualTo(200)
            val tokenResponse: OAuth2TokenResponse = jacksonObjectMapper().readValue(checkNotNull(response.body?.string()))
            assertThat(tokenResponse.accessToken).isNotNull
            assertThat(tokenResponse.idToken).isNull()
            assertThat(tokenResponse.expiresIn).isGreaterThan(0)
            assertThat(tokenResponse.scope).isNull()
            assertThat(tokenResponse.tokenType).isEqualTo("Bearer")
            assertThat(tokenResponse.issuedTokenType).isEqualTo("urn:ietf:params:oauth:token-type:access_token")
            val accessToken: SignedJWT = SignedJWT.parse(tokenResponse.accessToken)
            assertThat(accessToken.jwtClaimsSet.audience).containsExactly("desiredaudience")
            assertThat(accessToken.jwtClaimsSet.issuer).endsWith("tokenx")

            assertThat(accessToken.jwtClaimsSet.getClaim("claim1")).isEqualTo("value1")
            assertThat(accessToken.jwtClaimsSet.getClaim("claim2")).isEqualTo("value2")
        }
    }
}
