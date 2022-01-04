package no.nav.security.mock.oauth2.http

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.AUTHORIZATION
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.DEBUGGER
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.END_SESSION
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.JWKS
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.OAUTH2_WELL_KNOWN
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.OIDC_WELL_KNOWN
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.TESTUTILS_JWKS
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.TESTUTILS_SIGN
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.TOKEN
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.USER_INFO
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class OAuth2HttpRequestHandlerTest {

    @ParameterizedTest
    @MethodSource("testRequests")
    fun `supported routes in authorization server should return expected response`(request: OAuth2HttpRequest, expectedResponse: OAuth2HttpResponse) {
        authServer.invoke(request).asClue {
            it.status shouldBe expectedResponse.status
        }
    }

    companion object {
        private val tokenProvider = OAuth2TokenProvider()
        private val authServer = OAuth2HttpRequestHandler(OAuth2Config(tokenProvider = tokenProvider)).authorizationServer
        private const val AUTHORIZATION_WITH_PARAMS = "$AUTHORIZATION?client_id=client&response_type=code&redirect_uri=foo&scope=openid"

        @JvmStatic
        fun testRequests(): Stream<Arguments> = Stream.of(
            request(path = "/issuer1$OIDC_WELL_KNOWN", method = "GET", expectedResponse = OAuth2HttpResponse(status = 200)),
            request(path = "/issuer1$OAUTH2_WELL_KNOWN", method = "GET", expectedResponse = OAuth2HttpResponse(status = 200)),
            request(path = "/issuer1$JWKS", method = "GET", expectedResponse = OAuth2HttpResponse(status = 200)),
            request(path = "/issuer1$AUTHORIZATION_WITH_PARAMS", method = "GET", expectedResponse = OAuth2HttpResponse(status = 302)),
            request(
                path = "/issuer1$AUTHORIZATION_WITH_PARAMS",
                method = "POST",
                body = "username=foo",
                expectedResponse = OAuth2HttpResponse(status = 302)
            ),
            request(
                path = "/issuer1$TOKEN",
                method = "POST",
                headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
                body = "grant_type=client_credentials&client_id=client&client_secret=secret",
                expectedResponse = OAuth2HttpResponse(status = 200)
            ),
            request(path = "/issuer1$END_SESSION", method = "GET", expectedResponse = OAuth2HttpResponse(status = 200)),
            request(path = "/issuer1$USER_INFO", method = "GET", headers = bearerTokenHeader("issuer1"), expectedResponse = OAuth2HttpResponse(status = 200)),
            request(path = "/issuer1$DEBUGGER", method = "GET", expectedResponse = OAuth2HttpResponse(status = 200)),
            request(
                path = "/issuer1$DEBUGGER",
                method = "POST",
                headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
                body = "authorize_url=http://url",
                expectedResponse = OAuth2HttpResponse(status = 302)
            ),
            request(path = "/favicon.ico", method = "GET", expectedResponse = OAuth2HttpResponse(status = 200)),
            request(path= "/issuer1$TESTUTILS_JWKS", method="GET", expectedResponse = OAuth2HttpResponse(status = 200)),
            request(
                path= "/issuer1$TESTUTILS_SIGN",
                method="POST",
                body= (jacksonObjectMapper().createObjectNode().apply {
                    this.put("expiry","PT1H")
                    this.set<ObjectNode>("claims", jacksonObjectMapper().createObjectNode().apply {
                        this.set<ArrayNode>("groups", jacksonObjectMapper().createArrayNode().apply{
                            this.add("grp1")
                            this.add("grp2")
                        })
                    })
                }).toString(),
                expectedResponse = OAuth2HttpResponse(status = 200)
            )
        )

        private fun request(path: String, method: String, headers: Headers = Headers.headersOf(), body: String? = null, expectedResponse: OAuth2HttpResponse) =
            Arguments.of(
                OAuth2HttpRequest(
                    headers,
                    method,
                    "http://localhost$path".toHttpUrl(),
                    body
                ),
                expectedResponse
            )

        private fun bearerTokenHeader(issuerId: String): Headers {
            val claims = mapOf(
                "iss" to "http://localhost/$issuerId",
                "sub" to "foo",
                "extra" to "bar",
            )
            val bearerToken = tokenProvider.jwt(claims = claims, issuerId = issuerId).serialize()
            return Headers.headersOf("Authorization", "Bearer $bearerToken")
        }
    }
}
