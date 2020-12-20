package no.nav.security.mock.oauth2

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.net.InetSocketAddress
import no.nav.security.mock.oauth2.http.OAuth2HttpRequestHandler
import no.nav.security.mock.oauth2.server.MockWebServerConfig
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.mockwebserver.MockWebServer

private val config = ConfigurationProperties.systemProperties() overriding
    EnvironmentVariables()

data class Configuration(
    val server: Server = Server()
) {
    data class Server(
        val hostname: String = config.getOrElse(Key("server.hostname", stringType), "localhost"),
        val port: Int = config.getOrElse(Key("server.port", intType), 8080)
    )
}

fun main() {
    val config = Configuration()
    MockOAuth2Server(
        OAuth2Config(
            interactiveLogin = true
        )
    ).start(InetSocketAddress(0).address, config.server.port)

    /*MockWebServerConfig(MockWebServer()).toServer(OAuth2HttpRequestHandler(OAuth2Config(
        interactiveLogin = true
    )))*/
}
