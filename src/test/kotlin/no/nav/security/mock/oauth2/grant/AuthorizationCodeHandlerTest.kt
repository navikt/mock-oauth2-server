package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.ResponseMode
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.uuidRegex
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class AuthorizationCodeHandlerTest {
    private val handler = AuthorizationCodeHandler(OAuth2TokenProvider(), RefreshTokenManager())

    private fun HttpUrl.toOAuth2Request(
        headers: Headers = Headers.headersOf(),
        method: String = "GET",
        body: String? = null,
    ) = OAuth2HttpRequest(headers, method, this, body)

    @Test
    fun `authorization code response should contain required parameters`() {
        with(
            "http://authorizationendpoint"
                .toHttpUrl()
                .authenticationRequest(
                    state = "mystate",
                    redirectUri = "http://redirect",
                ),
        ) {
            handler.authorizationCodeResponse(this.toOAuth2Request()).asClue {
                it.impliedResponseType().impliesCodeFlow() shouldBe true
                it.impliedResponseMode() shouldBe ResponseMode.QUERY
                it.state.toString() shouldBe "mystate"
                it.redirectionURI.toString() shouldBe "http://redirect"
            }
        }
    }

    @Test
    fun `token response with login should return id_token and access_token containing username from login as sub`() {
        val code: String = handler.retrieveAuthorizationCode()

        handler.tokenResponse(tokenRequest(code = code), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback()).asClue {
            SignedJWT.parse(it.idToken).subject shouldMatch uuidRegex
        }
    }

    @Test
    fun `token response with login including multiple claims should return access_token containing all claims from login`() {
        val code: String = handler.retrieveAuthorizationCode()

        handler.tokenResponse(tokenRequest(code = code), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback()).asClue {
            val claims = SignedJWT.parse(it.idToken).claims
            claims["aud"] shouldBe arrayListOf("defaultClient")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["{", "[]", "[\"claim\"]", "{}"])
    fun `token response with login including invalid JSON for claims parsing should return access_token containing no additional claims`() {
        val code: String = handler.retrieveAuthorizationCode()

        handler.tokenResponse(tokenRequest(code = code), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback()).asClue {
            SignedJWT.parse(it.idToken).claims.count() shouldBe 10
        }
    }

    private fun AuthorizationCodeHandler.retrieveAuthorizationCode(): String =
        authorizationCodeResponse(
            "http://authorizationendpoint".toHttpUrl().authenticationRequest().toOAuth2Request(),
        ).authorizationCode.value

    private fun HttpUrl.asNimbusAuthRequest(): AuthenticationRequest = AuthenticationRequest.parse(this.toUri())

    private fun tokenRequest(
        code: String,
        redirectUri: String = "http://redirect",
    ): OAuth2HttpRequest =
        OAuth2HttpRequest(
            headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
            method = "POST",
            originalUrl = "http://localhost/token".toHttpUrl(),
            body =
                "grant_type=authorization_code&" +
                    "client_id=client1&" +
                    "client_secret=secret&" +
                    "code=$code&" +
                    "redirect_uri=$redirectUri&",
        )
}
