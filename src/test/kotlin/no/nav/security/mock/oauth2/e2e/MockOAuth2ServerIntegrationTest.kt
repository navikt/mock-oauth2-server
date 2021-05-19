package no.nav.security.mock.oauth2.e2e

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.id.Issuer
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.extensions.verifySignatureAndIssuer
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.http.WellKnown
import no.nav.security.mock.oauth2.http.route
import no.nav.security.mock.oauth2.testutils.audience
import no.nav.security.mock.oauth2.testutils.claims
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.issuer
import no.nav.security.mock.oauth2.testutils.parse
import no.nav.security.mock.oauth2.testutils.post
import no.nav.security.mock.oauth2.testutils.subject
import no.nav.security.mock.oauth2.testutils.toTokenResponse
import no.nav.security.mock.oauth2.testutils.tokenRequest
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.Duration

class MockOAuth2ServerIntegrationTest {
    private val client = client()

    @Test
    fun `server with addtional routes should serve wellknown and additional route`() {
        val s = MockOAuth2Server(
            route("/custom") {
                OAuth2HttpResponse(status = 200, body = "custom route")
            }
        ).apply {
            start()
        }
        client.get(s.wellKnownUrl("someissuer")).body?.string() shouldContain s.issuerUrl("someissuer").toString()
        client.get(s.url("/custom")).body?.string() shouldBe "custom route"
        client.get(s.url("/someissuer/custom")).body?.string() shouldBe "custom route"
    }

    @Test
    fun `server on fixed port should include fixed port in wellknown and issued token`() {
        val port = 1234
        val server = MockOAuth2Server()
        server.start(port = port)

        val issuerUrl = "http://localhost:$port/issuer1"
        client.get("$issuerUrl/.well-known/openid-configuration".toHttpUrl()).asClue {
            it.parse<WellKnown>().issuer shouldBe issuerUrl
            server.issueToken("issuer1").issuer shouldBe issuerUrl
        }

        server.shutdown()
    }

    @Test
    fun `wellknown should include issuer id in urls`() {
        withMockOAuth2Server {
            val baseUrl = this.baseUrl().toString().removeSuffix("/")
            client.get(this.wellKnownUrl("default")).let {
                it.parse<WellKnown>() urlsShouldStartWith "$baseUrl/default"
            }
            client.get(this.wellKnownUrl("foo")).let {
                it.parse<WellKnown>() urlsShouldStartWith "$baseUrl/foo"
            }
            client.get(this.wellKnownUrl("path1/path2/path3")).let {
                it.parse<WellKnown>() urlsShouldStartWith "$baseUrl/path1/path2/path3"
            }
        }
    }

    @Test
    fun `token request with enqueued token callback should return claims from tokencallback (with exception of id_token and oidc rules)`() {
        val server = MockOAuth2Server().apply { start() }
        server.enqueueCallback(
            DefaultOAuth2TokenCallback(
                issuerId = "custom",
                subject = "yolo",
                audience = listOf("myaud")
            )
        )

        client.post(
            server.tokenEndpointUrl("custom"),
            mapOf(
                "client_id" to "client1",
                "client_secret" to "secret",
                "grant_type" to "authorization_code",
                "scope" to "openid scope1",
                "redirect_uri" to "http://mycallback",
                "code" to "1234"
            )
        ).toTokenResponse().asClue {
            it.idToken.shouldNotBeNull()
            it.idToken.subject shouldBe "yolo"
            it.idToken.audience shouldBe listOf("client1")
            it.accessToken.shouldNotBeNull()
            it.accessToken.subject shouldBe "yolo"
            it.accessToken.audience shouldBe listOf("myaud")
        }
        server.shutdown()
    }

    @Test
    fun `issue token directly from server should contain claims from callback and be verifiable with server jwks`() {
        withMockOAuth2Server {
            val signedJWT: SignedJWT = this.issueToken(
                "default",
                "client1",
                DefaultOAuth2TokenCallback(
                    issuerId = "default",
                    subject = "mysub",
                    audience = listOf("myaud"),
                    claims = mapOf("someclaim" to "claimvalue")
                )
            )
            val wellKnown = client.get(this.wellKnownUrl("default")).parse<WellKnown>()
            val jwks = client.get(wellKnown.jwksUri.toHttpUrl()).body?.let { JWKSet.parse(it.string()) }

            jwks.shouldNotBeNull()

            signedJWT.verifySignatureAndIssuer(Issuer(wellKnown.issuer), jwks).asClue {
                it.issuer shouldBe wellKnown.issuer
                it.subject shouldBe "mysub"
                it.audience shouldContainExactly listOf("myaud")
                it.claims["someclaim"] shouldBe "claimvalue"
            }
        }
    }

    @Test
    fun `anyToken should issue token with claims from input and be verifyable by servers keys`() {
        withMockOAuth2Server {
            val customIssuer = "https://customissuer/default".toHttpUrl()
            val token = this.anyToken(
                customIssuer,
                mutableMapOf(
                    "sub" to "mysub",
                    "aud" to listOf("myapp"),
                    "customInt" to 123,
                    "customList" to listOf(1, 2, 3)
                ),
                Duration.ofSeconds(10)
            )

            val wellKnown = client.get(this.wellKnownUrl("default")).parse<WellKnown>()
            val jwks = client.get(wellKnown.jwksUri.toHttpUrl()).body?.let { JWKSet.parse(it.string()) }

            jwks.shouldNotBeNull()

            token.verifySignatureAndIssuer(Issuer(customIssuer.toString()), jwks).asClue {
                it.issuer shouldBe customIssuer.toString()
                it.subject shouldBe "mysub"
                it.audience shouldContainExactly listOf("myapp")
                it.claims["customInt"] shouldBe 123
                it.claims["customList"] to listOf(1, 2, 3)
            }
        }
    }

    @Test
    fun `token request matching RequestMappingTokenCallback should return configured claims`() {
        val server = MockOAuth2Server(OAuth2Config.fromJson(configJson)).apply { start() }
        client.tokenRequest(
            server.tokenEndpointUrl("issuer1"),
            "client1" to "secret",
            mapOf(
                "grant_type" to "client_credentials",
                "scope" to "scope1"
            )
        ).toTokenResponse().accessToken.asClue {

            it.shouldNotBeNull()
            it.claims shouldContainAll mapOf(
                "sub" to "subByScope",
                "aud" to listOf("audByScope")
            )
        }
    }

    private infix fun WellKnown.urlsShouldStartWith(url: String) {
        issuer shouldStartWith url
        authorizationEndpoint shouldStartWith url
        tokenEndpoint shouldStartWith url
        jwksUri shouldStartWith url
        endSessionEndpoint shouldStartWith url
    }

    @Language("json")
    private val configJson = """{
      "interactiveLogin" : true,
      "httpServer": "MockWebServerWrapper",
      "tokenCallbacks": [
        {
          "issuerId": "issuer1",
          "tokenExpiry": 120,
          "requestMappings": [
            {
              "requestParam": "scope",
              "match": "scope1",
              "claims": {
                "sub": "subByScope",
                "aud": [
                  "audByScope"
                ]
              }
            }
          ]
        },
        {
          "issuerId": "issuer2",
          "requestMappings": [
            {
              "requestParam": "someparam",
              "match": "somevalue",
              "claims": {
                "sub": "subBySomeParam",
                "aud": [
                  "audBySomeParam"
                ]
              }
            }
          ]
        }
      ]
    }
    """.trimIndent()
}
