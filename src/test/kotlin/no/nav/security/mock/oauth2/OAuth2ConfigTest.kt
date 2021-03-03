package no.nav.security.mock.oauth2

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import no.nav.security.mock.oauth2.FullConfig.configJson
import no.nav.security.mock.oauth2.HttpServerConfig.withMockWebServerWrapper
import no.nav.security.mock.oauth2.HttpServerConfig.withNettyHttpServer
import no.nav.security.mock.oauth2.HttpServerConfig.withUnknownHttpServer
import no.nav.security.mock.oauth2.http.MockWebServerWrapper
import no.nav.security.mock.oauth2.http.NettyWrapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class OAuth2ConfigTest {

    @Test
    fun `create httpServer from json`() {
        OAuth2Config.fromJson(withNettyHttpServer).httpServer should beInstanceOf<NettyWrapper>()
        OAuth2Config.fromJson(withMockWebServerWrapper).httpServer should beInstanceOf<MockWebServerWrapper>()
        shouldThrow<InvalidFormatException> {
            OAuth2Config.fromJson(withUnknownHttpServer)
        }
    }

    @Test
    fun `create full config from json with multiple tokenCallbacks`() {
        val config = OAuth2Config.fromJson(configJson)
        config.interactiveLogin shouldBe true
        config.httpServer should beInstanceOf<NettyWrapper>()
        config.tokenCallbacks.size shouldBe 2
        config.tokenCallbacks.map {
            it.issuerId()
        }.toList() shouldContainAll listOf("issuer1", "issuer2")
    }
}

object FullConfig {
    @Language("json")
    val configJson = """{
      "interactiveLogin" : true,
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
}
