package no.nav.security.mock.oauth2

import com.fasterxml.jackson.databind.JsonMappingException
import com.nimbusds.jose.jwk.KeyType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf
import no.nav.security.mock.oauth2.FullConfig.configJson
import no.nav.security.mock.oauth2.HttpServerConfig.mockWebServerWithGeneratedKeystore
import no.nav.security.mock.oauth2.HttpServerConfig.mockWebServerWithProvidedKeystore
import no.nav.security.mock.oauth2.HttpServerConfig.nettyWithGeneratedKeystore
import no.nav.security.mock.oauth2.HttpServerConfig.nettyWithProvidedKeystore
import no.nav.security.mock.oauth2.HttpServerConfig.withMockWebServerWrapper
import no.nav.security.mock.oauth2.HttpServerConfig.withNettyHttpServer
import no.nav.security.mock.oauth2.HttpServerConfig.withUnknownHttpServer
import no.nav.security.mock.oauth2.SigningKey.signingJsonGenerated
import no.nav.security.mock.oauth2.SigningKey.signingJsonSpecified
import no.nav.security.mock.oauth2.SigningKey.signingJsonUnsupported
import no.nav.security.mock.oauth2.http.MockWebServerWrapper
import no.nav.security.mock.oauth2.http.NettyWrapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import java.util.Date

internal class OAuth2ConfigTest {

    @Test
    fun `create httpServer from json`() {
        OAuth2Config.fromJson(withNettyHttpServer).httpServer should beInstanceOf<NettyWrapper>()
        OAuth2Config.fromJson(withMockWebServerWrapper).httpServer should beInstanceOf<MockWebServerWrapper>()
        shouldThrow<JsonMappingException> {
            OAuth2Config.fromJson(withUnknownHttpServer)
        }
    }

    @Test
    fun `create full config from json with multiple tokenCallbacks`() {
        val config = OAuth2Config.fromJson(configJson)
        config.interactiveLogin shouldBe true
        config.loginPagePath shouldBe "./login.html"
        config.httpServer should beInstanceOf<NettyWrapper>()
        config.tokenCallbacks.size shouldBe 2
        config.tokenCallbacks.map {
            it.issuerId()
        }.toList() shouldContainAll listOf("issuer1", "issuer2")
    }

    @Test
    fun `create tokenProvider with generated signing algorithm EC`() {
        val config = OAuth2Config.fromJson(signingJsonGenerated)
        config.tokenProvider.publicJwkSet().keys[0].keyType.value shouldBe KeyType.EC.value
    }

    @Test
    fun `create tokenProvider with specified signing algorithm EC`() {
        val config = OAuth2Config.fromJson(signingJsonSpecified)
        config.tokenProvider.publicJwkSet().keys[0].keyType.value shouldBe KeyType.EC.value
        config.tokenProvider.publicJwkSet("issuer0").keys[0].keyID shouldBe "issuer0"
    }

    @Test
    fun `create config from json with an unsupported algorithm should throw Oauth2Exception`() {
        shouldThrow<JsonMappingException> {
            OAuth2Config.fromJson(signingJsonUnsupported)
        }.message shouldContain "Unsupported algorithm: EdDSA"
    }

    @Test
    fun `create NettyWrapper with https enabled and provided keystore`() {
        val server = OAuth2Config.fromJson(nettyWithProvidedKeystore).httpServer as NettyWrapper
        val actualKeyStore = server.ssl?.sslKeystore?.keyStore
        val actualPrivateKey = actualKeyStore?.getKey("localhost", "".toCharArray())
        val expectedPrivateKey = privateKeyFromFile()
        actualPrivateKey shouldBe expectedPrivateKey
    }

    @Test
    fun `create MockWebServerWrapper with https enabled and provided keystore`() {
        val server = OAuth2Config.fromJson(mockWebServerWithProvidedKeystore).httpServer as MockWebServerWrapper
        val actualKeyStore = server.ssl?.sslKeystore?.keyStore
        val actualPrivateKey = actualKeyStore?.getKey("localhost", "".toCharArray())
        val expectedPrivateKey = privateKeyFromFile()
        actualPrivateKey shouldBe expectedPrivateKey
    }

    @Test
    fun `create NettyWrapper with https enabled and generated keystore`() {
        val server = OAuth2Config.fromJson(nettyWithGeneratedKeystore).httpServer as NettyWrapper
        val actualKeyStore = server.ssl?.sslKeystore?.keyStore
        val actualCert = actualKeyStore?.getCertificate("localhost") as X509Certificate
        actualCert.notBefore should beAroundNow()
    }

    @Test
    fun `create MockWebServerWrapper with https enabled and generated keystore`() {
        val server = OAuth2Config.fromJson(mockWebServerWithGeneratedKeystore).httpServer as MockWebServerWrapper
        val actualKeyStore = server.ssl?.sslKeystore?.keyStore
        val actualCert = actualKeyStore?.getCertificate("localhost") as X509Certificate
        actualCert.notBefore should beAroundNow()
    }

    private fun beAroundNow(skew: Duration = Duration.ofSeconds(2)) = object : Matcher<Date> {
        override fun test(value: Date): MatcherResult {
            val now = Instant.now()
            val withSkew = value.toInstant().plus(skew)
            return MatcherResult(
                withSkew.isAfter(now),
                { "Date $withSkew should be after $now" },
                {
                    "Date $withSkew should not be after $now"
                }
            )
        }
    }

    private fun privateKeyFromFile() =
        KeyStore.getInstance("PKCS12").apply {
            File("src/test/resources/localhost.p12").inputStream().use { load(it, "".toCharArray()) }
        }.getKey("localhost", "".toCharArray())
}

object FullConfig {
    @Language("json")
    val configJson = """{
      "interactiveLogin" : true,
      "loginPagePath": "./login.html",
      "httpServer": "NettyWrapper",
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

object SigningKey {
    @Language("json")
    val signingJsonGenerated = """
        {
        "tokenProvider" : {
            "keyProvider" : {
               "algorithm" : "ES256"
            }
          }
        }
    """.trimIndent()

    @Language("json")
    val signingJsonSpecified = """
        {
        "tokenProvider" : {
            "keyProvider" : {
               "initialKeys" : "{\"kty\": \"EC\",\"d\": \"o9INzHyU_I97djF36YQRpHCJxFTgDTbS1OtwUnHc34U\",\"use\":\"sig\",\"crv\": \"P-256\",\"kid\": \"issuer0\",\"x\": \"umybCYzE-VX_UAIJaX3wc-GTOgB7WDp7A3JJAKW_hqU\",\"y\": \"m_sCzuMjiBSQ7At9yNktMQvE1cCKq68jO7wnRczwKw8\"}",
               "algorithm" : "ES256"
            }
          }
        }
    """.trimIndent()

    @Language("json")
    val signingJsonUnsupported = """
        {
        "tokenProvider" : {
            "keyProvider" : {
               "algorithm" : "EdDSA"
            }
          }
        }
    """.trimIndent()
}

object HttpServerConfig {
    @Language("json")
    val withMockWebServerWrapper = """
        {
          "httpServer": "MockWebServerWrapper"
        }
    """.trimIndent()

    @Language("json")
    val withNettyHttpServer = """
        {
          "httpServer": "NettyWrapper"
        }
    """.trimIndent()

    @Language("json")
    val withUnknownHttpServer = """
        {
          "httpServer": "UnknownServer"
        }
    """.trimIndent()

    @Language("json")
    val nettyWithProvidedKeystore = """
        {
          "httpServer" : {
            "type" : "NettyWrapper",
            "ssl" : {
              "keystoreFile" : "src/test/resources/localhost.p12"
            }
          }
        }
    """.trimIndent()

    @Language("json")
    val nettyWithGeneratedKeystore = """
        {
          "httpServer" : {
            "type" : "NettyWrapper",
            "ssl" : {}
          }
        }
    """.trimIndent()

    @Language("json")
    val mockWebServerWithGeneratedKeystore = """
        {
          "httpServer" : {
            "type" : "MockWebServerWrapper",
            "ssl" : {}
          }
        }
    """.trimIndent()

    @Language("json")
    val mockWebServerWithProvidedKeystore = """
        {
          "httpServer" : {
            "type" : "MockWebServerWrapper",
            "ssl" : {
              "keystoreFile" : "src/test/resources/localhost.p12"
            }
          }
        }
    """.trimIndent()
}
