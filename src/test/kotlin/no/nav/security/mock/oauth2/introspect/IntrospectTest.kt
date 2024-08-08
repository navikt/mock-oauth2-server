package no.nav.security.mock.oauth2.introspect

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.JWSAlgorithm
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.INTROSPECT
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.http.routes
import no.nav.security.mock.oauth2.token.KeyProvider
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

internal class IntrospectTest {
    private val rs384TokenProvider = OAuth2TokenProvider(keyProvider = KeyProvider(initialKeys = emptyList(), algorithm = JWSAlgorithm.RS384.name))

    @Test
    fun `introspect should return active and claims from bearer token`() {
        val issuerUrl = "http://localhost/default"
        val tokenProvider = OAuth2TokenProvider()
        val claims =
            mapOf(
                "iss" to issuerUrl,
                "client_id" to "yolo",
                "token_type" to "token",
                "sub" to "foo",
            )
        val token = tokenProvider.jwt(claims)
        val request = request("$issuerUrl$INTROSPECT", token.serialize())

        routes { introspect(tokenProvider) }.invoke(request).asClue {
            it.status shouldBe 200
            val response = it.parse<Map<String, Any>>()
            response shouldContainAll claims
            response shouldContain ("active" to true)
        }
    }

    @Test
    fun `introspect should return active and claims for non-default algorithm from bearer token`() {
        val issuerUrl = "http://localhost/default"
        val claims =
            mapOf(
                "iss" to issuerUrl,
                "client_id" to "yolo",
                "token_type" to "token",
                "sub" to "foo",
            )
        val token = rs384TokenProvider.jwt(claims)
        val request = request("$issuerUrl$INTROSPECT", token.serialize())

        routes { introspect(rs384TokenProvider) }.invoke(request).asClue {
            it.status shouldBe 200
            val response = it.parse<Map<String, Any>>()
            response shouldContainAll claims
            response shouldContain ("active" to true)
        }
    }

    @Test
    fun `introspect should return active false when token is missing`() {
        val url = "http://localhost/default$INTROSPECT"

        routes {
            introspect(OAuth2TokenProvider())
        }.invoke(request(url, null)).asClue {
            it.status shouldBe 200
            it.parse<Map<String, Any>>() shouldContainExactly mapOf("active" to false)
        }
    }

    @Test
    fun `introspect should return active false when token is invalid`() {
        val url = "http://localhost/default$INTROSPECT"

        routes {
            introspect(OAuth2TokenProvider())
        }.invoke(request(url, "invalid")).asClue {
            it.status shouldBe 200
            it.parse<Map<String, Any>>() shouldContainExactly mapOf("active" to false)
        }
    }

    @Test
    fun `introspect should return active false when token was signed with a different algorithm than token provider`() {
        val issuerUrl = "http://localhost/default"
        val claims =
            mapOf(
                "iss" to issuerUrl,
                "client_id" to "yolo",
                "token_type" to "token",
                "sub" to "foo",
            )
        val token = rs384TokenProvider.jwt(claims)
        val request = request("$issuerUrl$INTROSPECT", token.serialize())

        routes {
            introspect(OAuth2TokenProvider())
        }.invoke(request).asClue {
            it.status shouldBe 200
            it.parse<Map<String, Any>>() shouldContainExactly mapOf("active" to false)
        }
    }

    @Test
    fun `introspect should return 401 when no Authorization header is provided`() {
        val url = "http://localhost/default$INTROSPECT"

        shouldThrow<OAuth2Exception> {
            routes {
                introspect(OAuth2TokenProvider())
            }.invoke(request(url, "invalid", "no auth"))
        }.asClue {
            it.errorObject?.code shouldBe "invalid_client"
            it.errorObject?.httpStatusCode shouldBe 401
            it.errorObject?.description shouldBe "The client authentication was invalid"
        }
    }

    private inline fun <reified T> OAuth2HttpResponse.parse(): T = jacksonObjectMapper().readValue(checkNotNull(body))

    private fun request(
        url: String,
        token: String?,
        auth: String = "Basic user=password",
    ): OAuth2HttpRequest =
        OAuth2HttpRequest(
            Headers.headersOf(
                "Authorization",
                auth,
                "Accept",
                "application/json",
                "Content-Type",
                "application/x-www-form-urlencoded",
            ),
            method = "POST",
            url.toHttpUrl(),
            body = token?.let { "token=$it&token_type_hint=access_token" },
        )
}
