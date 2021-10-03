package no.nav.security.mock.oauth2.userinfo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.USER_INFO
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.http.routes
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

internal class UserInfoTest {

    @Test
    fun `userinfo should return claims from bearer token`() {
        val issuerUrl = "http://localhost/default"
        val tokenProvider = OAuth2TokenProvider()
        val claims = mapOf(
            "iss" to issuerUrl,
            "sub" to "foo",
            "extra" to "bar",
        )
        val bearerToken = tokenProvider.jwt(claims)
        val request = request("$issuerUrl$USER_INFO", bearerToken.serialize())

        routes { userInfo(tokenProvider) }.invoke(request).asClue {
            it.status shouldBe 200
            it.parse<Map<String, Any>>() shouldContainAll claims
        }
    }

    @Test
    fun `userinfo should throw OAuth2Exception when bearer token is missing`() {
        val url = "http://localhost/default$USER_INFO"

        shouldThrow<OAuth2Exception> {

            routes {
                userInfo(OAuth2TokenProvider())
            }.invoke(request(url, null))
        }.asClue {
            it.errorObject?.code shouldBe "invalid_token"
            it.errorObject?.description shouldBe "missing bearer token"
            it.errorObject?.httpStatusCode shouldBe 401
        }
    }

    @Test
    fun `userinfo should throw OAuth2Exception when bearer token is invalid`() {
        val url = "http://localhost/default$USER_INFO"

        shouldThrow<OAuth2Exception> {
            routes {
                userInfo(OAuth2TokenProvider())
            }.invoke(request(url, "invalid"))
        }.asClue {
            it.errorObject?.code shouldBe "invalid_token"
            it.errorObject?.httpStatusCode shouldBe 401
        }
    }

    private inline fun <reified T> OAuth2HttpResponse.parse(): T = jacksonObjectMapper().readValue(checkNotNull(body))

    private fun request(url: String, bearerToken: String?): OAuth2HttpRequest {
        return OAuth2HttpRequest(
            bearerToken?.let { Headers.headersOf("Authorization", "Bearer $it") } ?: Headers.headersOf(),
            "GET",
            url.toHttpUrl(),
            null
        )
    }
}
