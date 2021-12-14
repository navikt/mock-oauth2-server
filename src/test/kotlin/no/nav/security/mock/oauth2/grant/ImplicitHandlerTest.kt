package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.ResponseMode
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.extensions.toIssuerUrl
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.issuer
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

internal class ImplicitHandlerTest {
    private val handler = ImplicitGrantHandler(OAuth2TokenProvider())

    @Test
    fun `authorization implicit response should contain required parameters`() {
        oauth2HttpRequest().also { request ->

            val accessToken = handler.tokenResponse(
                request, request.url.toIssuerUrl(),
                DefaultOAuth2TokenCallback(
                    claims = mapOf("acr" to "value1", "abc" to "value2")
                )
            ).apply {
                val signedJwt = SignedJWT.parse(this.accessToken)
                val claims = signedJwt.claims
                claims["acr"] shouldBe "value1"
                claims["abc"] shouldBe "value2"
                signedJwt.issuer shouldBe "http://myissuer/issuer1"
            }
            handler.implicitResponse(request.asAuthorizationRequest(), accessToken).asClue {
                it.accessToken shouldNotBe null
                it.impliedResponseType().impliesImplicitFlow() shouldBe true
                it.impliedResponseMode() shouldBe ResponseMode.FRAGMENT
                it.state.toString() shouldBe "mystate"
                it.redirectionURI.toString() shouldBe "http://redirect"
                it.toHTTPResponse().statusCode shouldBe 302
            }
        }
    }

    private fun oauth2HttpRequest(
        redirectUri: String = "http://redirect",
        scope: String = "some-scope"
    ): OAuth2HttpRequest {
        return OAuth2HttpRequest(
            headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
            method = "GET",
            originalUrl = (
                "http://myissuer/issuer1/authorize?" +
                    "response_type=token&" +
                    "client_id=client1&" +
                    "state=mystate&" +
                    "redirect_uri=$redirectUri&" +
                    "scope=$scope"
                ).toHttpUrl()

        )
    }
}
