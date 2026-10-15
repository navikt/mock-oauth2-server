package no.nav.security.mock.oauth2.e2e

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.SignedJWT
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.http.WellKnown
import no.nav.security.mock.oauth2.testutils.ClientAssertionType
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.clientAssertion
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.parse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.token.KeyProvider
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.Headers
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class WellKnownIntegrationTest {
    private val client = client()

    @Test
    fun `get to well-known url should return oauth2 server metadata`() {
        withMockOAuth2Server {
            val response = client.get(this.wellKnownUrl("default"))
            val body = response.body.string()
            response.code shouldBe 200
            body shouldNotBe null
            jacksonObjectMapper().readValue<Map<String, Any>>(body).keys.asClue {
                it shouldContainExactlyInAnyOrder
                    listOf(
                        "issuer",
                        "authorization_endpoint",
                        "end_session_endpoint",
                        "revocation_endpoint",
                        "token_endpoint",
                        "userinfo_endpoint",
                        "jwks_uri",
                        "introspection_endpoint",
                        "response_modes_supported",
                        "response_types_supported",
                        "subject_types_supported",
                        "id_token_signing_alg_values_supported",
                        "code_challenge_methods_supported",
                        "grant_types_supported",
                        "token_endpoint_auth_methods_supported",
                        "scopes_supported",
                        "token_endpoint_auth_signing_alg_values_supported",
                    )
            }
        }
    }

    @Test
    fun `well-known should advertise the grant types accepted by the token endpoint`() {
        withMockOAuth2Server {
            val grantTypes = metadata()["grant_types_supported"] as List<*>
            grantTypes shouldContainExactlyInAnyOrder
                listOf(
                    "authorization_code",
                    "refresh_token",
                    "client_credentials",
                    "password",
                    "urn:ietf:params:oauth:grant-type:jwt-bearer",
                    "urn:ietf:params:oauth:grant-type:token-exchange",
                )
            grantTypes.forEach { grantType ->
                tokenRequestWithGrantType(grantType.toString()).asClue {
                    it shouldNotContain "unsupported_grant_type"
                }
            }
            tokenRequestWithGrantType("implicit") shouldContain "unsupported_grant_type"
        }
    }

    @Test
    fun `well-known should advertise the client authentication methods accepted by the token endpoint`() {
        withMockOAuth2Server {
            val authMethods = metadata()["token_endpoint_auth_methods_supported"] as List<*>
            authMethods shouldContainExactlyInAnyOrder listOf("client_secret_basic", "client_secret_post", "private_key_jwt")
            authMethods.forEach { authMethod ->
                clientCredentialsRequest(authMethod.toString()).asClue {
                    it.code shouldBe 200
                }
            }
        }
    }

    @Test
    fun `well-known should advertise the signing algorithms accepted for private_key_jwt client assertions`() {
        withMockOAuth2Server {
            val algorithms = metadata()["token_endpoint_auth_signing_alg_values_supported"] as List<*>
            algorithms shouldContainExactlyInAnyOrder clientAssertionAlgorithms.keys.map { it.name }
            clientAssertionAlgorithms.forEach { (algorithm, signer) ->
                val clientAssertion =
                    SignedJWT(
                        JWSHeader.Builder(algorithm).type(JOSEObjectType.JWT).build(),
                        clientAssertion("client1", this.tokenEndpointUrl("default").toString()).jwtClaimsSet,
                    ).apply { sign(signer) }
                client
                    .tokenRequest(
                        this.tokenEndpointUrl("default"),
                        mapOf(
                            "grant_type" to "client_credentials",
                            "scope" to "scope1",
                            "client_assertion_type" to ClientAssertionType.JWT_BEARER,
                            "client_assertion" to clientAssertion.serialize(),
                        ),
                    ).use {
                        "$algorithm: ${it.body.string()}".asClue { _ -> it.code shouldBe 200 }
                    }
            }
        }
    }

    @Test
    fun `well-known should advertise the openid scope`() {
        withMockOAuth2Server {
            metadata()["scopes_supported"] as List<*> shouldContain "openid"
        }
    }

    @Test
    fun `well-known should only advertise the default signing algorithm`() {
        withMockOAuth2Server {
            client.get(this.wellKnownUrl("default")).parse<WellKnown>().asClue {
                it.idTokenSigningAlgValuesSupported shouldContainExactly listOf("RS256")
            }
        }
    }

    @Test
    fun `well-known should advertise the configured signing algorithm used for issued tokens`() {
        val config =
            OAuth2Config(
                tokenProvider = OAuth2TokenProvider(KeyProvider(initialKeys = emptyList(), algorithm = "ES256")),
            )
        withMockOAuth2Server(config) {
            client.get(this.wellKnownUrl("default")).parse<WellKnown>().asClue {
                it.idTokenSigningAlgValuesSupported shouldContainExactly listOf("ES256")
            }
            this
                .issueToken("default")
                .header.algorithm.name shouldBe "ES256"
        }
    }

    @Test
    fun `get to well-known url with IPv6 host header should not crash and reflect the proxy-aware issuer`() {
        withMockOAuth2Server {
            val port = this.wellKnownUrl("default").port
            val response =
                client.get(
                    this.wellKnownUrl("default"),
                    headers = Headers.headersOf("host", "[::1]:$port"),
                )
            val body = response.body.string()
            response.code shouldBe 200
            jacksonObjectMapper().readValue<Map<String, Any>>(body).asClue { metadata ->
                metadata["issuer"] shouldBe "http://[::1]:$port/default"
                listOf(
                    "authorization_endpoint",
                    "end_session_endpoint",
                    "revocation_endpoint",
                    "token_endpoint",
                    "userinfo_endpoint",
                    "jwks_uri",
                    "introspection_endpoint",
                ).forEach { (metadata[it] as String) shouldStartWith "http://[::1]:$port/default/" }
            }
        }
    }

    private fun MockOAuth2Server.metadata(): Map<String, Any> =
        client.get(this.wellKnownUrl("default")).use {
            it.code shouldBe 200
            jacksonObjectMapper().readValue<Map<String, Any>>(it.body.string())
        }

    private fun MockOAuth2Server.tokenRequestWithGrantType(grantType: String): String =
        client
            .tokenRequest(
                this.tokenEndpointUrl("default"),
                mapOf("grant_type" to grantType, "client_id" to "client1", "client_secret" to "secret"),
            ).use { it.body.string() }

    private fun MockOAuth2Server.clientCredentialsRequest(authMethod: String) =
        when (authMethod) {
            "client_secret_basic" -> {
                client.tokenRequest(
                    this.tokenEndpointUrl("default"),
                    basicAuth = Pair("client1", "secret"),
                    parameters = mapOf("grant_type" to "client_credentials", "scope" to "scope1"),
                )
            }

            "client_secret_post" -> {
                client.tokenRequest(
                    this.tokenEndpointUrl("default"),
                    mapOf("grant_type" to "client_credentials", "scope" to "scope1", "client_id" to "client1", "client_secret" to "secret"),
                )
            }

            "private_key_jwt" -> {
                client.tokenRequest(
                    this.tokenEndpointUrl("default"),
                    mapOf(
                        "grant_type" to "client_credentials",
                        "scope" to "scope1",
                        "client_assertion_type" to ClientAssertionType.JWT_BEARER,
                        "client_assertion" to clientAssertion("client1", this.tokenEndpointUrl("default").toString()).serialize(),
                    ),
                )
            }

            else -> {
                error("no test for client authentication method $authMethod")
            }
        }.also { it.close() }

    private val clientAssertionAlgorithms: Map<JWSAlgorithm, JWSSigner> =
        RSASSASigner(RSAKeyGenerator(2048).generate()).let { rsaSigner ->
            listOf(JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512, JWSAlgorithm.PS256, JWSAlgorithm.PS384, JWSAlgorithm.PS512)
                .associateWith { rsaSigner }
        } +
            mapOf(
                JWSAlgorithm.ES256 to ECDSASigner(ECKeyGenerator(Curve.P_256).generate()),
                JWSAlgorithm.ES384 to ECDSASigner(ECKeyGenerator(Curve.P_384).generate()),
                JWSAlgorithm.ES512 to ECDSASigner(ECKeyGenerator(Curve.P_521).generate()),
            )
}
