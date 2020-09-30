package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.ResponseMode
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse
import java.net.URI
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.login.Login
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class AuthorizationCodeHandlerTest {
    private val handler = AuthorizationCodeHandler()

    @Test
    fun authorizationCodeResponse() {
        val response: AuthenticationSuccessResponse =
            handler.authorizationCodeResponse(
                authRequest(
                    "client1",
                    "openid",
                    "code",
                    "http://redirect",
                    "someState",
                    "someNonce",
                    "query"
                )
            )
        assertThatAuthResponseContainsRequiredParams(response)
    }

    @Test
    fun tokenResponse() {
    }

    @Test
    fun tokenResponseWithLogin() {
        val response: AuthenticationSuccessResponse =
            handler.authorizationCodeResponse(
                authRequest(
                    "client1",
                    "openid",
                    "code",
                    "http://redirect",
                    "someState",
                    "someNonce",
                    "query"
                ),
                Login("foo")
            )
        assertThatAuthResponseContainsRequiredParams(response)

        val tokenResponse = handler.tokenResponse(
            tokenRequest(response.authorizationCode, "http://redirect", "openid"),
            "http://myissuer".toHttpUrl(),
            DefaultOAuth2TokenCallback()
        )
        val idToken: SignedJWT = SignedJWT.parse(tokenResponse.idToken)
        assertThat(idToken.jwtClaimsSet.audience.first()).isEqualTo("client1")
        assertThat(idToken.jwtClaimsSet.subject).isEqualTo("foo")
    }

    private fun assertThatAuthResponseContainsRequiredParams(response: AuthenticationSuccessResponse) {
        assertThat(response.impliedResponseType().impliesCodeFlow()).isTrue()
        assertThat(response.impliedResponseMode()).isEqualTo(ResponseMode.QUERY)
        assertThat(response.state).isEqualTo(State("someState"))
        assertThat(response.redirectionURI).isEqualTo(URI.create("http://redirect"))
    }

    private fun authRequest(
        clientId: String,
        scope: String,
        responseType: String,
        redirectUri: String,
        state: String,
        nonce: String,
        responseMode: String
    ): AuthenticationRequest {
        val url: HttpUrl = "http://localhost".toHttpUrl().newBuilder()
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("scope", scope)
            .addQueryParameter("response_type", responseType)
            .addQueryParameter("redirect_uri", redirectUri)
            .addQueryParameter("state", state)
            .addQueryParameter("nonce", nonce)
            .addQueryParameter("response_mode", responseMode)
            .build()
        return AuthenticationRequest.parse(url.toUri())
    }

    private fun tokenRequest(
        code: AuthorizationCode,
        redirectUri: String,
        scope: String
    ): OAuth2HttpRequest {
        return OAuth2HttpRequest(
            headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
            method = "POST",
            url = "http://localhost/token".toHttpUrl(),
            body = "grant_type=authorization_code&" +
                "client_id=client1&" +
                "client_secret=secret&" +
                "code=${code.value}&" +
                "redirect_uri=$redirectUri&" +
                "scope=$scope"

        ).also {
            println("code is $code")
        }
    }
}
