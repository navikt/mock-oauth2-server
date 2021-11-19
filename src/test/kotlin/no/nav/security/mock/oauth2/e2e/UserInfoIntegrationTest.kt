package no.nav.security.mock.oauth2.e2e

import com.nimbusds.jwt.SignedJWT
import io.kotest.assertions.asClue
import io.kotest.matchers.maps.shouldContainAll
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.parse
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.Headers
import org.junit.jupiter.api.Test

class UserInfoIntegrationTest {

    private val client = client()

    @Test
    fun `userinfo should return claims from token when valid bearer token is present`() {
        withMockOAuth2Server {
            val issuerId = "default"
            val token = this.issueToken(issuerId = issuerId, subject = "foo", claims = mapOf("extra" to "bar"))
            client.get(
                url = this.userInfoUrl(issuerId),
                headers = token.asBearerTokenHeader()
            ).asClue {
                it.parse<Map<String, Any>>() shouldContainAll mapOf(
                    "sub" to token.claims["sub"],
                    "iss" to token.claims["iss"],
                    "extra" to token.claims["extra"],
                )
            }
        }
    }

    private fun SignedJWT.asBearerTokenHeader(): Headers = this.serialize().let {
        Headers.headersOf("Authorization", "Bearer $it")
    }
}
