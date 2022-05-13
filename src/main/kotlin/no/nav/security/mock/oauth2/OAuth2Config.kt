package no.nav.security.mock.oauth2

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK
import no.nav.security.mock.oauth2.http.MockWebServerWrapper
import no.nav.security.mock.oauth2.http.NettyWrapper
import no.nav.security.mock.oauth2.http.OAuth2HttpServer
import no.nav.security.mock.oauth2.http.Ssl
import no.nav.security.mock.oauth2.http.SslKeystore
import no.nav.security.mock.oauth2.token.DAYS_TO_EXPIRE
import no.nav.security.mock.oauth2.token.KeyProvider
import no.nav.security.mock.oauth2.token.MOCK_OAUTH2_SERVER_NAME
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback
import java.io.File

data class OAuth2Config @JvmOverloads constructor(
    val interactiveLogin: Boolean = false,
    val loginPagePath: String? = null,
    @JsonDeserialize(using = OAuth2TokenProviderDeserializer::class)
    val tokenProvider: OAuth2TokenProvider = OAuth2TokenProvider(),
    @JsonDeserialize(contentAs = RequestMappingTokenCallback::class)
    val tokenCallbacks: Set<OAuth2TokenCallback> = emptySet(),
    @JsonDeserialize(using = OAuth2HttpServerDeserializer::class)
    val httpServer: OAuth2HttpServer = MockWebServerWrapper()
) {

    class OAuth2TokenProviderDeserializer : JsonDeserializer<OAuth2TokenProvider>() {

        data class ProviderConfig(
            val keyProvider: KeyProviderConfig?
        )

        data class KeyProviderConfig(
            val initialKeys: String?,
            val algorithm: String?,
            val certificate: CertificateConfig?
        )

        data class CertificateConfig(
            val x509CertChain: Boolean?,
            val expiresInDays: Int?,
            val cn: String?
        )

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): OAuth2TokenProvider {
            val node: JsonNode = p.readValueAsTree()
            val config: ProviderConfig = if (!node.isObject) {
                return OAuth2TokenProvider()
            } else {
                p.codec.treeToValue(node, ProviderConfig::class.java)
            }
            val jwks = config.keyProvider?.initialKeys?.let {
                listOf(JWK.parse(it))
            } ?: emptyList()

            return OAuth2TokenProvider(
                KeyProvider(
                    certCfg = no.nav.security.mock.oauth2.token.CertificateConfig(
                        x509CertChain = config.keyProvider?.certificate?.x509CertChain ?: false,
                        expiresInDays = config.keyProvider?.certificate?.expiresInDays ?: DAYS_TO_EXPIRE,
                        cn = config.keyProvider?.certificate?.cn ?: MOCK_OAUTH2_SERVER_NAME
                    ),
                    initialKeys = jwks,
                    algorithm = config.keyProvider?.algorithm ?: JWSAlgorithm.RS256.name,
                )
            )
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
}
