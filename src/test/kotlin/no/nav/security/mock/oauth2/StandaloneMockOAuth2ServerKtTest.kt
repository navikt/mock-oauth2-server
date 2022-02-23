package no.nav.security.mock.oauth2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import no.nav.security.mock.oauth2.StandaloneConfig.JSON_CONFIG
import no.nav.security.mock.oauth2.StandaloneConfig.JSON_CONFIG_PATH
import no.nav.security.mock.oauth2.StandaloneConfig.SERVER_PORT
import no.nav.security.mock.oauth2.StandaloneConfig.PORT
import no.nav.security.mock.oauth2.StandaloneConfig.hostname
import no.nav.security.mock.oauth2.StandaloneConfig.oauth2Config
import no.nav.security.mock.oauth2.StandaloneConfig.port
import no.nav.security.mock.oauth2.http.NettyWrapper
import org.junit.jupiter.api.Test
import java.io.File
import java.net.InetSocketAddress

internal class StandaloneMockOAuth2ServerKtTest {

    private val configFile = "src/test/resources/config.json"

    @Test
    fun `load config with no env vars set`() {
        val config = oauth2Config()
        config.tokenCallbacks.size shouldBe 0
        config.interactiveLogin shouldBe true
        config.httpServer should beInstanceOf<NettyWrapper>()
        hostname() shouldBe InetSocketAddress(0).address
        port() shouldBe 8080
    }

    @Test
    fun `with the environment variable SERVER_PORT set`() {
        withEnvironment(SERVER_PORT to "9292") {
            port() shouldBe 9292
        }
    }

    @Test
    fun `with the environment variable PORT set`() {
        withEnvironment(PORT to "9292") {
            port() shouldBe 9292
        }
    }

    @Test
    fun `with the environment variables SERVER_PORT and PORT set`() {
        withEnvironment(mapOf(SERVER_PORT to "9292", PORT to "9393")) {
            port() shouldBe 9292
        }
    }


    @Test
    fun `load oauth2Config from file`() {
        withEnvironment(JSON_CONFIG_PATH to configFile) {
            val config = oauth2Config()
            config.tokenCallbacks.size shouldBe 2
            config.tokenCallbacks shouldContainExactly tokenCallbacksFromFile()
        }
    }

    @Test
    fun `load oauth2Config from env var`() {
        val json = File(configFile).readText()
        withEnvironment(JSON_CONFIG to json) {
            val config = oauth2Config()
            config.tokenCallbacks.size shouldBe 2
            config.tokenCallbacks shouldContainExactly tokenCallbacksFromFile()
        }
    }

    private fun tokenCallbacksFromFile() = jacksonObjectMapper().readValue<OAuth2Config>(File(configFile).readText()).tokenCallbacks
}
