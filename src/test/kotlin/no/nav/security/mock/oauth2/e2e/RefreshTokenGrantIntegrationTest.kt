package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.testutils.audience
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.authorizationCode
import no.nav.security.mock.oauth2.testutils.shouldBeValidFor
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.testutils.verifyWith
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

class RefreshTokenGrantIntegrationTest {
    private val client: OkHttpClient = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .build()

    @Test
    fun `token request with refresh_token grant should return id_token and access_token with same subject as authorization code grant`() {
        withMockOAuth2Server {
            val initialSubject = "yolo"
            val issuerId = "idprovider"

            // Authenticate using Authorization Code Flow
            val codeResponse = client.authenticationRequest(this.authorizationEndpointUrl(issuerId), initialSubject)
            val authorizationCode = checkNotNull(codeResponse.authorizationCode)

            // Token Request based on authorization code
            val tokenResponseBeforeRefresh = client.tokenRequest(
                this.tokenEndpointUrl(issuerId),
                mapOf(
                    "grant_type" to GrantType.AUTHORIZATION_CODE.value,
                    "code" to authorizationCode,
                    "client_id" to "id",
                    "client_secret" to "secret",
                    "scope" to "openid",
                    "redirect_uri" to "http://something"
                )
            ).toTokenResponse()

            tokenResponseBeforeRefresh.idToken?.subject shouldBe initialSubject
            tokenResponseBeforeRefresh.accessToken?.subject shouldBe initialSubject

            // make token request with the refresh_token grant
            val refreshToken = checkNotNull(tokenResponseBeforeRefresh.refreshToken)
            val refreshTokenResponse = client.tokenRequest(
                this.tokenEndpointUrl(issuerId),
                mapOf(
                    "grant_type" to GrantType.REFRESH_TOKEN.value,
                    "refresh_token" to refreshToken,
                    "client_id" to "id",
                    "client_secret" to "secret"
                )
            ).toTokenResponse()

            refreshTokenResponse shouldBeValidFor GrantType.REFRESH_TOKEN
            refreshTokenResponse.refreshToken shouldBe tokenResponseBeforeRefresh.refreshToken
            refreshTokenResponse.idToken!! shouldNotBe tokenResponseBeforeRefresh.idToken!!
            refreshTokenResponse.accessToken!! shouldNotBe tokenResponseBeforeRefresh.accessToken!!
            refreshTokenResponse.accessToken should verifyWith(issuerId, this)
            refreshTokenResponse.idToken should verifyWith(issuerId, this)

            refreshTokenResponse.idToken.subject shouldBe initialSubject
            refreshTokenResponse.idToken.audience shouldBe tokenResponseBeforeRefresh.idToken.audience
            refreshTokenResponse.accessToken.subject shouldBe initialSubject
        }
    }

    @Test
    fun `token request with refresh_token grant and enqueued tokencallback should return token with subject from tokencallback`() {
        withMockOAuth2Server {
            val expectedSubject = "expectedSub"
            val issuerId = "idprovider"
            this.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = issuerId, subject = expectedSubject))

            val refreshTokenResponse = client.tokenRequest(
                this.tokenEndpointUrl(issuerId),
                mapOf(
                    "grant_type" to GrantType.REFRESH_TOKEN.value,
                    "refresh_token" to "canbewhatever",
                    "client_id" to "id",
                    "client_secret" to "secret"
                )
            ).toTokenResponse()

            refreshTokenResponse shouldBeValidFor GrantType.REFRESH_TOKEN
            refreshTokenResponse.idToken!!.subject shouldBe expectedSubject
        }
    }

    @Test
    fun `token request with refresh_token grant and random refresh token should return random subject in tokens`() {
        withMockOAuth2Server {
            val issuerId = "idprovider"
            val refreshTokenResponse = client.tokenRequest(
                this.tokenEndpointUrl(issuerId),
                mapOf(
                    "grant_type" to GrantType.REFRESH_TOKEN.value,
                    "refresh_token" to "canbewhatever",
                    "client_id" to "id",
                    "client_secret" to "secret"
                )
            ).toTokenResponse()

            refreshTokenResponse shouldBeValidFor GrantType.REFRESH_TOKEN
            refreshTokenResponse.idToken!!.subject shouldNotBe null
            refreshTokenResponse.idToken should verifyWith(issuerId, this)
        }
    }
}
