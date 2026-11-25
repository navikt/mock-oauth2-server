package no.nav.security.mock.oauth2

import no.nav.security.mock.oauth2.StandaloneConfig.hostname
import no.nav.security.mock.oauth2.StandaloneConfig.oauth2Config
import no.nav.security.mock.oauth2.StandaloneConfig.port
import no.nav.security.mock.oauth2.http.NettyWrapper
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.http.route
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.io.FileNotFoundException
import java.net.InetAddress
import java.net.InetSocketAddress

object StandaloneConfig {
    private val jsonMapper = jacksonObjectMapper()

    const val JSON_CONFIG = "JSON_CONFIG"
    const val JSON_CONFIG_PATH = "JSON_CONFIG_PATH"
    const val SERVER_HOSTNAME = "SERVER_HOSTNAME"
    const val SERVER_PORT = "SERVER_PORT"
    const val PORT = "PORT" // Supports running Docker image on Heroku.

    fun hostname(): InetAddress =
        SERVER_HOSTNAME
            .fromEnv()
            ?.let { InetAddress.getByName(it) } ?: InetSocketAddress(0).address

    fun port(): Int = (SERVER_PORT.fromEnv()?.toInt() ?: PORT.fromEnv()?.toInt()) ?: 8080

    fun oauth2Config(): OAuth2Config =
        with(jsonFromEnv()) {
            if (this != null) {
                fromJsonWithNettyAsDefaultHttpServer(this)
            } else {
                OAuth2Config(
                    interactiveLogin = true,
                    httpServer = NettyWrapper(),
                )
            }
        }

    private fun fromJsonWithNettyAsDefaultHttpServer(json: String): OAuth2Config {
        val config = json.asJsonObjectOrNull() ?: return OAuth2Config.fromJson(json)
        if (!config.has("httpServer")) {
            config.put("httpServer", "NettyWrapper")
        }
        return jsonMapper.treeToValue(config, OAuth2Config::class.java)
    }

    private fun String.asJsonObjectOrNull(): ObjectNode? = runCatching { jsonMapper.readTree(this) }.getOrNull() as? ObjectNode

    private fun jsonFromEnv() = JSON_CONFIG.fromEnv() ?: JSON_CONFIG_PATH.fromEnv("config.json").readFile()

    private fun String.readFile(): String? =
        try {
            File(this).readText()
        } catch (e: FileNotFoundException) {
            null
        }
}

fun main() {
    // Literal instead of ch.qos.logback.classic.ClassicConstants: logback is a runtime-only
    // dependency of the standalone server and must not leak onto library consumers' classpath.
    System.setProperty("logback.configurationFile", "LOGBACK_CONFIG".fromEnv("logback-standalone.xml"))
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
