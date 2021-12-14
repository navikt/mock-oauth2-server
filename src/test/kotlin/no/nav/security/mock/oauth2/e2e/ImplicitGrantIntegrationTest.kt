package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.testutils.authorizationRequest
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class ImplicitGrantIntegrationTest {

    private val server = MockOAuth2Server().apply { start() }
    private val client = client()

    @Test
    fun `authorized request should return 302 with redirectUri as location and fragment params access_token, state, token_type, expires_in, scope`() {
        client.get(
            server.authorizationEndpointUrl("default").authorizationRequest()
        ).asClue { response ->
            response.code shouldBe 302
            response.headers["location"]?.toHttpUrl().asClue {
                it?.toString() shouldStartWith "http://defaultredirecturi/#"
                val fragments = it?.encodedFragment
                fragments.shouldContain("access_token")
                fragments.shouldContain("state")
                fragments.shouldContain("token_type")
                fragments?.shouldContain("expires_in")
                fragments?.shouldContain("scope")
            }
        }
    }

    @Test
    fun `implicit grant should return 400 bad request on request when none supported response_type is used`() {
        client.get(
            server.authorizationEndpointUrl("default").authorizationRequest(
                // hybrid grant
                responseType = "code id_token",
            )
        ).asClue { response ->
            response.code shouldBe 400
            response.body?.string() shouldContain "hybrid grant not supported (yet)"
        }
    }
}
