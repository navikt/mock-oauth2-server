package no.nav.security.mock.oauth2

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.net.InetAddress
import java.net.InetSocketAddress

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
    ).start(InetAddress.getByName(config.server.hostname), config.server.port)
}
