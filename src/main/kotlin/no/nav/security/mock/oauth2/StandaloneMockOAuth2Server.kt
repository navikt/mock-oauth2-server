package no.nav.security.mock.oauth2

import java.net.InetAddress
import java.net.InetSocketAddress
import no.nav.security.mock.oauth2.http.NettyWrapper
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.http.route

data class Configuration(
    val server: Server = Server()
) {
    data class Server(
        val hostname: InetAddress = "SERVER_HOSTNAME".fromEnv()?.let { InetAddress.getByName(it) } ?: InetSocketAddress(0).address,
        val port: Int = "SERVER_PORT".fromEnv()?.toInt() ?: 8080
    )
}

fun main() {
    val config = Configuration()
    MockOAuth2Server(
        OAuth2Config(
            interactiveLogin = true,
            httpServer = NettyWrapper()
        ),
        route("/isalive") {
            OAuth2HttpResponse(status = 200, body = "alive and well")
        }
    ).start(config.server.hostname, config.server.port)
}

fun String.fromEnv(): String? = System.getenv(this)
