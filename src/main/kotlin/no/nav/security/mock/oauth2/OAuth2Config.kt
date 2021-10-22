package no.nav.security.mock.oauth2

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.security.mock.oauth2.http.MockWebServerWrapper
import no.nav.security.mock.oauth2.http.NettyWrapper
import no.nav.security.mock.oauth2.http.OAuth2HttpServer
import no.nav.security.mock.oauth2.http.Ssl
import no.nav.security.mock.oauth2.http.SslKeystore
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback
import java.io.File

data class OAuth2Config @JvmOverloads constructor(
    val interactiveLogin: Boolean = false,
    val presets: List<Preset> = emptyList(),
    @JsonDeserialize(using = OAuth2TokenProviderDeserializer::class)
    val tokenProvider: OAuth2TokenProvider = OAuth2TokenProvider(),
    @JsonDeserialize(contentAs = RequestMappingTokenCallback::class)
    val tokenCallbacks: Set<OAuth2TokenCallback> = emptySet(),
    @JsonDeserialize(using = OAuth2HttpServerDeserializer::class)
    val httpServer: OAuth2HttpServer = MockWebServerWrapper()
) {

    class OAuth2TokenProviderDeserializer : JsonDeserializer<OAuth2TokenProvider>() {
        override fun deserialize(p0: JsonParser?, p1: DeserializationContext?): OAuth2TokenProvider {
            return OAuth2TokenProvider()
        }
    }

    class OAuth2HttpServerDeserializer : JsonDeserializer<OAuth2HttpServer>() {
        enum class ServerType {
            MockWebServerWrapper,
            NettyWrapper
        }

        data class ServerConfig(
            val type: ServerType,
            val ssl: SslConfig? = null
        )

        data class SslConfig(
            val keyPassword: String = "",
            val keystoreFile: File? = null,
            val keystoreType: SslKeystore.KeyStoreType = SslKeystore.KeyStoreType.PKCS12,
            val keystorePassword: String = ""
        ) {
            fun ssl() = Ssl(sslKeyStore())

            private fun sslKeyStore() =
                if (keystoreFile == null) SslKeystore() else SslKeystore(keyPassword, keystoreFile, keystoreType, keystorePassword)
        }

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OAuth2HttpServer {
            val node: JsonNode = p.readValueAsTree()
            val serverConfig: ServerConfig = if (node.isObject) {
                p.codec.treeToValue(node, ServerConfig::class.java)
            } else {
                ServerConfig(ServerType.valueOf(node.textValue()))
            }
            val ssl: Ssl? = serverConfig.ssl?.ssl()
            return when (serverConfig.type) {
                ServerType.NettyWrapper -> NettyWrapper(ssl)
                ServerType.MockWebServerWrapper -> MockWebServerWrapper(ssl)
            }
        }
    }

    companion object {
        fun fromJson(json: String): OAuth2Config {
            return jacksonObjectMapper().readValue(json)
        }
    }

    fun presetWithName(name: String): Preset =
        presets.first { it.name == name }
}

data class Preset(
    val name: String,
    val username: String,
    val claims: Map<String, Any>
) {
    val claimsAsString: String
        get() = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(claims)
}
