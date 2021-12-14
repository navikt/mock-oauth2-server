package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class ImplicitGrantIntegrationTest {

    private val server = MockOAuth2Server().apply { start() }
    private val client = client()

    @Test
    fun `authorized request should return 302 with redirectUri as location and query params access_token, state and token_type`() {
        client.get(
            server.authorizationEndpointUrl("default").authenticationRequest(
                redirectUri = "http://mycallback",
                state = "mystate",
                responseType = "token",
                scope = listOf("some-scope")
            )
        ).asClue { response ->
            response.code shouldBe 302

            response.headers["location"]?.toHttpUrl()?.queryParameter("access_token").shouldNotBeNull()

            response.headers["location"]?.toHttpUrl().asClue {
                it.toString() shouldStartWith "http://mycallback"
                it?.queryParameterNames shouldContainExactly setOf("access_token", "state", "token_type")
                it?.queryParameter("state") shouldBe "mystate"
            }
        }
    }

    @Test
    fun `implicit grant should return 400 bad request on request when none supported response_type is used`() {
        client.get(
            server.authorizationEndpointUrl("default").authenticationRequest(
                // hybrid grant
                responseType = "code id_token",
            )
        ).asClue { response ->
            response.code shouldBe 400
        }
    }
}
