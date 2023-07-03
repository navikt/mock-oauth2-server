package no.nav.security.mock.oauth2

import no.nav.security.mock.oauth2.StandaloneConfig.hostname
import no.nav.security.mock.oauth2.StandaloneConfig.oauth2Config
import no.nav.security.mock.oauth2.StandaloneConfig.port
import no.nav.security.mock.oauth2.http.NettyWrapper
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.http.route
import java.io.File
import java.io.FileNotFoundException
import java.net.InetAddress
import java.net.InetSocketAddress

object StandaloneConfig {
    const val JSON_CONFIG = "JSON_CONFIG"
    const val JSON_CONFIG_PATH = "JSON_CONFIG_PATH"
    const val SERVER_HOSTNAME = "SERVER_HOSTNAME"
    const val SERVER_PORT = "SERVER_PORT"
    const val PORT = "PORT" // Supports running Docker image on Heroku.

    fun hostname(): InetAddress = SERVER_HOSTNAME.fromEnv()
        ?.let { InetAddress.getByName(it) } ?: InetSocketAddress(0).address

    fun port(): Int = (SERVER_PORT.fromEnv()?.toInt() ?: PORT.fromEnv()?.toInt()) ?: 8080

    fun oauth2Config(): OAuth2Config = with(jsonFromEnv()) {
        if (this != null) {
            OAuth2Config.fromJson(this)
        } else {
            OAuth2Config(
                interactiveLogin = true,
                httpServer = NettyWrapper(),
            )
        }
    }

    private fun jsonFromEnv() = JSON_CONFIG.fromEnv() ?: JSON_CONFIG_PATH.fromEnv("config.json").readFile()

    private fun String.readFile(): String? =
        try {
            File(this).readText()
        } catch (e: FileNotFoundException) {
            null
        }
}

fun main() {
    MockOAuth2Server(
        oauth2Config(),
        route("/isalive") {
            OAuth2HttpResponse(status = 200, body = "alive and well")
        },
    ).apply {
        start(hostname(), port())
    }
}

fun String.fromEnv(default: String): String = System.getenv(this) ?: default
fun String.fromEnv(): String? = System.getenv(this)
