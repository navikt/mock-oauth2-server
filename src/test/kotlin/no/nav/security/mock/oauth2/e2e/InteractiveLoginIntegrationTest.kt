package no.nav.security.mock.oauth2.e2e

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.asClue
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.authenticationRequest
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.post
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class InteractiveLoginIntegrationTest {
    private val issuerId = "default"
    private val server = MockOAuth2Server(OAuth2Config(interactiveLogin = true)).apply { start() }
    private val client = client()

    @ParameterizedTest
    @MethodSource("testUsers")
    internal fun `interactive login with a supplied username should result in id_token containing sub and claims from input`(user: User) {
        val code = loginForCode(user)

        val response = tokenRequest(code)

        response.idToken.shouldNotBeNull()
        response.idToken.subject shouldBe user.username
        response.idToken.claims shouldContainAll user.claims
    }

    companion object {
        @JvmStatic
        fun testUsers(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    User(
                        username = "user1",
                        claims =
                            mapOf(
                                "claim1" to "claim1value",
                            ),
                    ),
                ),
                Arguments.of(
                    User(
                        username = "user2",
                        claims =
                            mapOf(
                                "claim2" to "claim2value",
                            ),
                    ),
                ),
            )
    }

    private fun loginForCode(user: User): String {
        val loginUrl = server.authorizationEndpointUrl(issuerId).authenticationRequest()
        client.get(loginUrl).asClue {
            it.code shouldBe 200
            it.body.string() shouldContain "<html"
        }

        return client.post(
            loginUrl,
            mapOf(
                "username" to user.username,
                "claims" to user.claimsAsJson(),
            ),
        ).let { authResponse ->
            val code = authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
            code.shouldNotBeNull()
        }
    }

    private fun tokenRequest(authCode: String) =
        client.tokenRequest(
            server.tokenEndpointUrl(issuerId),
            mapOf(
                "client_id" to "client1",
                "client_secret" to "secret",
                "grant_type" to "authorization_code",
                "redirect_uri" to "http://mycallback",
                "code" to authCode,
            ),
        ).toTokenResponse()

    internal data class User(
        val username: String,
        val claims: Map<String, Any> = emptyMap(),
    ) {
        fun claimsAsJson(): String = jacksonObjectMapper().writeValueAsString(claims)
    }
}
