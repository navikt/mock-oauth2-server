package no.nav.security.mock.oauth2.e2e

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jwt.SignedJWT
import io.kotest.assertions.asClue
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.parse
import no.nav.security.mock.oauth2.token.KeyProvider
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.Headers
import org.junit.jupiter.api.Test

class UserInfoIntegrationTest {

    private val client = client()
    private val rs384Config = OAuth2Config(
        tokenProvider = OAuth2TokenProvider(keyProvider = KeyProvider(initialKeys = emptyList(), algorithm = JWSAlgorithm.RS384.name))
    )

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
                    "extra" to token.claims["extra"]
                )
            }
        }
    }

    @Test
    fun `userinfo should return claims from token signed with non-default algorithm when valid bearer token is present`() {
        withMockOAuth2Server(config = rs384Config) {
            val issuerId = "default"
            val token = this.issueToken(issuerId = issuerId, subject = "foo", claims = mapOf("extra" to "bar"))
            token.header.algorithm.shouldBe(JWSAlgorithm.RS384)
            client.get(
                url = this.userInfoUrl(issuerId),
                headers = token.asBearerTokenHeader()
            ).asClue {
                it.parse<Map<String, Any>>() shouldContainAll mapOf(
                    "sub" to token.claims["sub"],
                    "iss" to token.claims["iss"],
                    "extra" to token.claims["extra"]
                )
            }
        }
    }

    @Test
    fun `userinfo should return error from token signed with non-default algorithm does not match server config`() {
        val issuerId = "default"
        val rs384Server = MockOAuth2Server(config = rs384Config)
        val token = rs384Server.issueToken(issuerId = issuerId, subject = "foo", claims = mapOf("extra" to "bar"))
        withMockOAuth2Server {
            client.get(
                url = this.userInfoUrl(issuerId),
                headers = token.asBearerTokenHeader()
            ).asClue {
                it.code shouldBe 401
                it.message shouldBe "Client Error"
            }
        }
    }

    private fun SignedJWT.asBearerTokenHeader(): Headers = this.serialize().let {
        Headers.headersOf("Authorization", "Bearer $it")
    }
}
