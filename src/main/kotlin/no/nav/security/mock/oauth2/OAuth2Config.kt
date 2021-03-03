package no.nav.security.mock.oauth2

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.security.mock.oauth2.http.MockWebServerWrapper
import no.nav.security.mock.oauth2.http.NettyWrapper
import no.nav.security.mock.oauth2.http.OAuth2HttpServer
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback

data class OAuth2Config @JvmOverloads constructor(
    val interactiveLogin: Boolean = false,
    val tokenProvider: OAuth2TokenProvider = OAuth2TokenProvider(),
    @JsonDeserialize(contentAs = RequestMappingTokenCallback::class)
    val tokenCallbacks: Set<OAuth2TokenCallback> = emptySet(),
    @JsonDeserialize(using = OAuth2HttpServerDeserializer::class)
    val httpServer: OAuth2HttpServer = MockWebServerWrapper()
) {

    class OAuth2HttpServerDeserializer : JsonDeserializer<OAuth2HttpServer>() {
        enum class ServerType {
            MockWebServerWrapper,
            NettyWrapper
        }

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OAuth2HttpServer {
            return when (p.readValueAs<ServerType>(object : TypeReference<ServerType>() {})) {
                ServerType.NettyWrapper -> NettyWrapper()
                ServerType.MockWebServerWrapper -> MockWebServerWrapper()
                else -> throw IllegalArgumentException("unsupported httpServer specified in config")
            }
        }
    }

    companion object {
        fun fromJson(json: String): OAuth2Config {
            return jacksonObjectMapper().readValue(json)
        }
    }
}
