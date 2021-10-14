package no.nav.security.mock.oauth2.grant

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.ResponseMode
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.login.Login
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

internal class AuthorizationCodeHandlerTest {
    private val handler = AuthorizationCodeHandler(OAuth2TokenProvider(), RefreshTokenManager())

    @Test
    fun `authorization code response should contain required parameters`() {
        with(
            "http://authorizationendpoint".toHttpUrl()
                .authenticationRequest(
                    state = "mystate",
                    redirectUri = "http://redirect"
                )
        ) {
            handler.authorizationCodeResponse(AuthenticationRequest.parse(this.toUri())).asClue {
                it.impliedResponseType().impliesCodeFlow() shouldBe true
                it.impliedResponseMode() shouldBe ResponseMode.QUERY
                it.state.toString() shouldBe "mystate"
                it.redirectionURI.toString() shouldBe "http://redirect"
            }
        }
    }

    @Test
    fun `token response with login should return id_token and access_token containing username from login as sub`() {
        val code: String = handler.authorizationCodeResponse(
            authenticationRequest = "http://authorizationendpoint".toHttpUrl().authenticationRequest().asNimbusAuthRequest(),
            login = Login("foo")
        ).authorizationCode.value

        handler.tokenResponse(tokenRequest(code = code), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback()).asClue {
            SignedJWT.parse(it.idToken).subject shouldBe "foo"
        }
    }

    @ParameterizedTest
    @MethodSource("jsonClaimsProvider")
    fun `token response with login including claims should return access_token containing claims from login`(claims: String, expectedClaimKey: String, expectedClaimValue: String) {
        val code: String = handler.authorizationCodeResponse(
            authenticationRequest = "http://authorizationendpoint".toHttpUrl().authenticationRequest().asNimbusAuthRequest(),
            login = Login("foo", claims)
        ).authorizationCode.value

        handler.tokenResponse(tokenRequest(code = code), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback()).asClue {
            val claim = SignedJWT.parse(it.idToken).claims[expectedClaimKey]
            claim shouldNotBe null
            jacksonObjectMapper().writeValueAsString(claim) shouldBe expectedClaimValue
        }
    }

    companion object {
        @JvmStatic
        fun jsonClaimsProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("{ \"acr\": \"value\" }", "acr", "\"value\""),
            Arguments.of("{ \"acr\": { \"reference\": { \"id\": \"value\" } } }", "acr", "{\"reference\":{\"id\":\"value\"}}")
        )
    }

    @Test
    fun `token response with login including multiple claims should return access_token containing all claims from login`() {
        val code: String = handler.authorizationCodeResponse(
            authenticationRequest = "http://authorizationendpoint".toHttpUrl().authenticationRequest().asNimbusAuthRequest(),
            login = Login("foo", "{ \"acr\": \"value1\", \"abc\": \"value2\" }")
        ).authorizationCode.value

        handler.tokenResponse(tokenRequest(code = code), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback()).asClue {
            val claims = SignedJWT.parse(it.idToken).claims
            claims["acr"] shouldBe "value1"
            claims["abc"] shouldBe "value2"
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["{", "[]", "[\"claim\"]", "{}"])
    fun `token response with login including invalid JSON for claims parsing should return access_token containing no additional claims`(claimsValue: String) {
        val code: String = handler.authorizationCodeResponse(
            authenticationRequest = "http://authorizationendpoint".toHttpUrl().authenticationRequest().asNimbusAuthRequest(),
            login = Login("foo", claimsValue)
        ).authorizationCode.value

        handler.tokenResponse(tokenRequest(code = code), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback()).asClue {
            SignedJWT.parse(it.idToken).claims.count() shouldBe 10
        }
    }

    private fun HttpUrl.asNimbusAuthRequest(): AuthenticationRequest = AuthenticationRequest.parse(this.toUri())

    private fun tokenRequest(
        code: String,
        redirectUri: String = "http://redirect",
        scope: String = "openid"
    ): OAuth2HttpRequest {
        return OAuth2HttpRequest(
            headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
            method = "POST",
            originalUrl = "http://localhost/token".toHttpUrl(),
            body = "grant_type=authorization_code&" +
                "client_id=client1&" +
                "client_secret=secret&" +
                "code=$code&" +
                "redirect_uri=$redirectUri&" +
                "scope=$scope"

        )
    }
}
