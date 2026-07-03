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
import no.nav.security.mock.oauth2.token.RequestMapping
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test
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
        val response = fetchToken(code)
        response.idToken.shouldNotBeNull()
        response.idToken.subject shouldBe user.username
        response.idToken.claims shouldContainAll user.claims
    }

    @Test
    fun `interactive login selects requestMapping based on login username as subject`() {
        val requestMappingCallback =
            RequestMappingTokenCallback(
                issuerId = issuerId,
                requestMappings =
                    listOf(
                        RequestMapping(
                            requestParam = "subject",
                            match = "alice",
                            claims = mapOf("role" to "admin", "sub" to "alice"),
                        ),
                        RequestMapping(
                            requestParam = "subject",
                            match = "bob",
                            claims = mapOf("role" to "user", "sub" to "bob"),
                        ),
                    ),
            )
        MockOAuth2Server(
            OAuth2Config(
                interactiveLogin = true,
                tokenCallbacks = setOf(requestMappingCallback),
            ),
        ).apply { start() }.let { srv ->
            try {
                val aliceCode = loginForCode(User(username = "alice"), srv)
                val bobCode = loginForCode(User(username = "bob"), srv)

                val aliceResponse = fetchToken(aliceCode, srv)
                val bobResponse = fetchToken(bobCode, srv)

                aliceResponse.idToken.shouldNotBeNull()
                aliceResponse.idToken.subject shouldBe "alice"
                aliceResponse.idToken.claims shouldContainAll mapOf("role" to "admin")
                aliceResponse.idToken.claims["sub"] shouldBe "alice"

                bobResponse.idToken.shouldNotBeNull()
                bobResponse.idToken.subject shouldBe "bob"
                bobResponse.idToken.claims shouldContainAll mapOf("role" to "user")
                bobResponse.idToken.claims["sub"] shouldBe "bob"
            } finally {
                srv.shutdown()
            }
        }
    }

    @Test
    fun `mapping sub claim is not overwritten by login claims`() {
        val requestMappingCallback =
            RequestMappingTokenCallback(
                issuerId = issuerId,
                requestMappings =
                    listOf(
                        RequestMapping(
                            requestParam = "subject",
                            match = "alice",
                            claims = mapOf("sub" to "mapped-alice"),
                        ),
                    ),
            )
        MockOAuth2Server(
            OAuth2Config(
                interactiveLogin = true,
                tokenCallbacks = setOf(requestMappingCallback),
            ),
        ).apply { start() }.let { srv ->
            try {
                val code = loginForCode(User(username = "alice", claims = mapOf("sub" to "login-alice")), srv)
                val response = fetchToken(code, srv)
                response.idToken.shouldNotBeNull()
                response.idToken.subject shouldBe "mapped-alice"
                response.idToken.claims["sub"] shouldBe "mapped-alice"
            } finally {
                srv.shutdown()
            }
        }
    }

    @Test
    fun `interactive login subject falls back to login username when matching mapping omits sub`() {
        val requestMappingCallback =
            RequestMappingTokenCallback(
                issuerId = issuerId,
                requestMappings =
                    listOf(
                        RequestMapping(
                            requestParam = "subject",
                            match = "alice",
                            claims = mapOf("role" to "admin"),
                        ),
                    ),
            )
        MockOAuth2Server(
            OAuth2Config(
                interactiveLogin = true,
                tokenCallbacks = setOf(requestMappingCallback),
            ),
        ).apply { start() }.let { srv ->
            try {
                val code = loginForCode(User(username = "alice"), srv)
                val response = fetchToken(code, srv)
                response.idToken.shouldNotBeNull()
                response.idToken.subject shouldBe "alice"
                response.idToken.claims["sub"] shouldBe "alice"
                response.idToken.claims["role"] shouldBe "admin"
            } finally {
                srv.shutdown()
            }
        }
    }

    @Test
    fun `interactive login should prefer submitted username over authorize subject param`() {
        val requestMappingCallback =
            RequestMappingTokenCallback(
                issuerId = issuerId,
                requestMappings =
                    listOf(
                        RequestMapping(
                            requestParam = "subject",
                            match = "bob",
                            claims = mapOf("role" to "user", "sub" to "bob"),
                        ),
                        RequestMapping(
                            requestParam = "subject",
                            match = "alice",
                            claims = mapOf("role" to "admin", "sub" to "alice"),
                        ),
                    ),
            )
        MockOAuth2Server(
            OAuth2Config(
                interactiveLogin = true,
                tokenCallbacks = setOf(requestMappingCallback),
            ),
        ).apply { start() }.let { srv ->
            try {
                val loginUrl = srv.authorizationEndpointUrl(issuerId).authenticationRequest(extraQueryParams = mapOf("subject" to "alice"))
                val code =
                    client
                        .post(
                            loginUrl,
                            mapOf(
                                "username" to "bob",
                                "claims" to User(username = "bob").claimsAsJson(),
                            ),
                        ).let { authResponse ->
                            authResponse.headers["location"]?.toHttpUrl()?.queryParameter("code")
                        }

                code.shouldNotBeNull()

                val response = fetchToken(code, srv)
                response.idToken.shouldNotBeNull()
                response.idToken.subject shouldBe "bob"
                response.idToken.claims["sub"] shouldBe "bob"
                response.idToken.claims["role"] shouldBe "user"
            } finally {
                srv.shutdown()
            }
        }
    }

    companion object {
        @JvmStatic
        fun testUsers(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    User(
                        username = "user1",
                        claims = mapOf("claim1" to "claim1value"),
                    ),
                ),
                Arguments.of(
                    User(
                        username = "user2",
                        claims = mapOf("claim2" to "claim2value"),
                    ),
                ),
            )
    }

    private fun loginForCode(
        user: User,
        srv: MockOAuth2Server = server,
    ): String {
        val loginUrl = srv.authorizationEndpointUrl(issuerId).authenticationRequest()
        client.get(loginUrl).asClue {
            it.code shouldBe 200
            it.body.string() shouldContain "<html"
        }
        return client
            .post(
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

    private fun fetchToken(
        authCode: String,
        srv: MockOAuth2Server = server,
    ) = client
        .tokenRequest(
            srv.tokenEndpointUrl(issuerId),
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
