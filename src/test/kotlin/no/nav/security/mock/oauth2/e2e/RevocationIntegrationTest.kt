package no.nav.security.mock.oauth2.e2e

import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.grant.RefreshToken
import no.nav.security.mock.oauth2.testutils.ParsedTokenResponse
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.post
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

class RevocationIntegrationTest {
    private val client: OkHttpClient = client()
    private val initialSubject = "yolo"
    private val issuerId = "idprovider"

    @Test
    fun `revocation request with refresh_token should should remove refresh token`() {
        withMockOAuth2Server {
            val tokenResponseBeforeRefresh = login()
            tokenResponseBeforeRefresh.idToken?.subject shouldBe initialSubject
            tokenResponseBeforeRefresh.accessToken?.subject shouldBe initialSubject

            var refreshTokenResponse = refresh(tokenResponseBeforeRefresh.refreshToken)
            refreshTokenResponse.accessToken?.subject shouldBe initialSubject
            val refreshToken = checkNotNull(refreshTokenResponse.refreshToken)
            val revocationResponse =
                client.post(
                    this.url("/default/revoke"),
                    mapOf(
                        "client_id" to "id",
                        "client_secret" to "secret",
                        "token" to refreshToken,
                        "token_type_hint" to "refresh_token",
                    ),
                )
            revocationResponse.code shouldBe 200

            refreshTokenResponse = refresh(tokenResponseBeforeRefresh.refreshToken)
            refreshTokenResponse.accessToken?.subject shouldNotBe initialSubject
        }
    }

    private fun MockOAuth2Server.login(): ParsedTokenResponse {
        // Authenticate using Authorization Code Flow
        // simulate user interaction by doing the auth request as a post (instead of get with user punching username/pwd and submitting form)
        val authorizationCode =
            client.post(
                this.authorizationEndpointUrl("default").authenticationRequest(),
                mapOf("username" to initialSubject),
            ).let { authResponse ->
                authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
            }

        authorizationCode.shouldNotBeNull()

        // Token Request based on authorization code
        return client.tokenRequest(
            this.tokenEndpointUrl(issuerId),
            mapOf(
                "grant_type" to GrantType.AUTHORIZATION_CODE.value,
                "code" to authorizationCode,
                "client_id" to "id",
                "client_secret" to "secret",
                "scope" to "openid",
                "redirect_uri" to "http://something",
            ),
        ).toTokenResponse()
    }

    private fun MockOAuth2Server.refresh(token: RefreshToken?): ParsedTokenResponse {
        // make token request with the refresh_token grant
        val refreshToken = checkNotNull(token)
        return client.tokenRequest(
            this.tokenEndpointUrl(issuerId),
            mapOf(
                "grant_type" to GrantType.REFRESH_TOKEN.value,
                "refresh_token" to refreshToken,
                "client_id" to "id",
                "client_secret" to "secret",
            ),
        ).toTokenResponse()
    }
}
