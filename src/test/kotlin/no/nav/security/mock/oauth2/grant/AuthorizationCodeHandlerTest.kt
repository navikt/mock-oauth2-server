package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.ResponseMode
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse
import no.nav.security.mock.oauth2.callback.DefaultTokenCallback
import no.nav.security.mock.oauth2.login.Login
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI

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
            tokenRequest(response.authorizationCode,"http://redirect", "openid"),
            "http://myissuer".toHttpUrl(),
            DefaultTokenCallback()
        )
        val idToken: SignedJWT = SignedJWT.parse(tokenResponse.idToken)
        assertThat(idToken.jwtClaimsSet.audience.first()).isEqualTo("client1")
        assertThat(idToken.jwtClaimsSet.subject).isEqualTo("foo")
    }

    private fun assertThatAuthResponseContainsRequiredParams(response: AuthenticationSuccessResponse){
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
    ): TokenRequest {
        return TokenRequest(
            URI.create("http://localhost/token"),
            ClientSecretBasic(ClientID("client1"), Secret("clientSecret")),
            AuthorizationCodeGrant(code,
            URI.create(redirectUri)),
            Scope(scope)
        )
    }
}
