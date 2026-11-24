package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.PlainJWT
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.ResponseMode
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.OAuth2Exception
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
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

internal class AuthorizationCodeHandlerTest {
    private val handler = AuthorizationCodeHandler(OAuth2TokenProvider(), RefreshTokenManager())

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
            handler.authorizationCodeResponse("default", AuthenticationRequest.parse(this.toUri())).asClue {
                it.impliedResponseType().impliesCodeFlow() shouldBe true
                it.impliedResponseMode() shouldBe ResponseMode.QUERY
                it.state.toString() shouldBe "mystate"
                it.redirectionURI.toString() shouldBe "http://redirect"
            }
        }
    }

    @Test
    fun `token response with login should return id_token and access_token containing username from login as sub`() {
        val code: String = handler.retrieveAuthorizationCode(Login("foo"))

        handler.tokenResponse(tokenRequest(code = code), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback()).asClue {
            SignedJWT.parse(it.idToken).subject shouldBe "foo"
        }
    }

    @ParameterizedTest
    @MethodSource("jsonClaimsProvider")
    fun `token response with login including claims should return access_token containing claims from login`(
        claims: String,
        expectedClaimKey: String,
        expectedClaimValue: String,
    ) {
        val code: String = handler.retrieveAuthorizationCode(Login("foo", claims))

        handler.tokenResponse(tokenRequest(code = code), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback()).asClue {
            val claim = SignedJWT.parse(it.idToken).claims[expectedClaimKey]
            claim shouldNotBe null
            jacksonObjectMapper().writeValueAsString(claim) shouldBe expectedClaimValue
        }
    }

    companion object {
        @JvmStatic
        fun jsonClaimsProvider(): Stream<Arguments> =
            Stream.of(
                Arguments.of("{ \"acr\": \"value\" }", "acr", "\"value\""),
                Arguments.of("{ \"acr\": { \"reference\": { \"id\": \"value\" } } }", "acr", "{\"reference\":{\"id\":\"value\"}}"),
            )
    }

    @Test
    fun `token response with login including multiple claims should return access_token containing all claims from login`() {
        val code: String = handler.retrieveAuthorizationCode(Login("foo", "{ \"acr\": \"value1\", \"abc\": \"value2\" }"))

        handler.tokenResponse(tokenRequest(code = code), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback()).asClue {
            val claims = SignedJWT.parse(it.idToken).claims
            claims["acr"] shouldBe "value1"
            claims["abc"] shouldBe "value2"
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["{", "[]", "[\"claim\"]", "{}"])
    fun `token response with login including invalid JSON for claims parsing should return access_token containing no additional claims`(claimsValue: String) {
        val code: String = handler.retrieveAuthorizationCode(Login("foo", claimsValue))

        handler.tokenResponse(tokenRequest(code = code), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback()).asClue {
            SignedJWT.parse(it.idToken).claims.count() shouldBe 10
        }
    }

    @Test
    fun `auth request with nonce should result in a token response with refresh token as a JWT containing the nonce`() {
        val code: String = handler.retrieveAuthorizationCode(Login("foo"))

        handler.tokenResponse(tokenRequest(code = code), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback()).asClue {
            val claims = PlainJWT.parse(it.refreshToken).jwtClaimsSet.claims
            claims["nonce"] shouldBe "5678"
        }
    }

    @Test
    fun `expired authorization codes should be removed when a new one is issued`() {
        var now = Instant.parse("2026-01-01T00:00:00Z")
        val codes = LinkedHashMap<AuthorizationCode, IssuedAuthorizationCode>()
        val handler = AuthorizationCodeHandler(OAuth2TokenProvider(timeProvider = { now }), RefreshTokenManager(), codes = codes)
        repeat(3) { handler.retrieveAuthorizationCode(Login("foo")) }

        now = now.plus(Duration.ofMinutes(10))
        val fresh = handler.retrieveAuthorizationCode(Login("foo"))

        codes.keys.map { it.value } shouldBe listOf(fresh)
    }

    @Test
    fun `oldest authorization code should be evicted beyond the limit, also with a frozen clock`() {
        val codes = LinkedHashMap<AuthorizationCode, IssuedAuthorizationCode>()
        val frozen = OAuth2TokenProvider(systemTime = Instant.parse("2026-01-01T00:00:00Z"))
        val handler = AuthorizationCodeHandler(frozen, RefreshTokenManager(), codes = codes, codeLimit = 2)
        val oldest = handler.retrieveAuthorizationCode(Login("foo"))
        val kept = (1..2).map { handler.retrieveAuthorizationCode(Login("foo")) }

        codes.keys.map { it.value } shouldBe kept
        shouldThrow<OAuth2Exception> {
            handler.tokenResponse(tokenRequest(code = oldest), "http://myissuer".toHttpUrl(), DefaultOAuth2TokenCallback())
        }.errorObject?.code shouldBe OAuth2Error.INVALID_GRANT.code
    }

    @Test
    fun `authorization codes should not be evicted when the limit is disabled`() {
        val codes = LinkedHashMap<AuthorizationCode, IssuedAuthorizationCode>()
        val frozen = OAuth2TokenProvider(systemTime = Instant.parse("2026-01-01T00:00:00Z"))
        val handler = AuthorizationCodeHandler(frozen, RefreshTokenManager(), codes = codes, codeLimit = null)
        repeat(1000) { handler.retrieveAuthorizationCode(Login("foo")) }

        codes.size shouldBe 1000
    }

    private fun AuthorizationCodeHandler.retrieveAuthorizationCode(login: Login): String =
        authorizationCodeResponse(
            issuerId = "default",
            authenticationRequest = "http://authorizationendpoint".toHttpUrl().authenticationRequest().asNimbusAuthRequest(),
            login = login,
        ).authorizationCode.value

    private fun HttpUrl.asNimbusAuthRequest(): AuthenticationRequest = AuthenticationRequest.parse(this.toUri())

    private fun tokenRequest(
        code: String,
        redirectUri: String = "http://mycallback",
    ): OAuth2HttpRequest =
        OAuth2HttpRequest(
            headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
            method = "POST",
            originalUrl = "http://localhost/default/token".toHttpUrl(),
            body =
                "grant_type=authorization_code&" +
                    "client_id=client1&" +
                    "client_secret=secret&" +
                    "code=$code&" +
                    "redirect_uri=$redirectUri&",
        )
}
