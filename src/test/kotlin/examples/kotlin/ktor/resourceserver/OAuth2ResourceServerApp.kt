package examples.kotlin.ktor.resourceserver

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.interfaces.Payload
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.prepareGet
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.net.URL
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }

fun main() {
    // just some random values for this example, would get from environment in production,
    // when testing you should provide a config that points to the mock-oauth2-server
    val authConfig = AuthConfig(
        mapOf(
            "provider1" to AuthConfig.TokenProvider(
                wellKnownUrl = "https://provider1/.well-known/openid-configuration",
                acceptedAudience = "thisAppClientId",
                requiredClaims = mapOf("groups" to listOf("group1"))
            ),
            "provider2" to AuthConfig.TokenProvider(
                wellKnownUrl = "https://provider2/.well-known/openid-configuration",
                acceptedAudience = "thisAppClientId",
                requiredClaims = mapOf("someClaim" to "someClaim1")
            )
        )
    )

    embeddedServer(Netty, port = 8080) {
        module(authConfig)
    }.start(true)
}

fun Application.module(authConfig: AuthConfig) {
    install(Authentication) {
        // just to show how the MockOAuth2Server enables testing with multiple "token providers"/issuers at the same time
        authConfig.providers.forEach { (shortName, provider) ->
            jwt(shortName) {
                verifier(provider.jwkProvider, provider.wellKnown.issuer) {
                    withAudience(provider.acceptedAudience)
                }
                validate { jwtCredential ->
                    if (jwtCredential.containsAll(provider.requiredClaims)) {
                        JWTPrincipal(jwtCredential.payload)
                    } else {
                        log.error("token does not contain all required claims: ${provider.requiredClaims}")
                        null
                    }
                }
            }
        }
    }
    routing {
        authenticate("provider1") {
            get("/hello1") {
                val token = call.principal<JWTPrincipal>()?.payload
                call.respond("hello1 ${token?.subject} from issuer ${token?.issuer}")
            }
        }
        authenticate("provider2") {
            get("/hello2") {
                val token = call.principal<JWTPrincipal>()?.payload
                call.respond("hello2 ${token?.subject} from issuer ${token?.issuer}")
            }
        }
    }
}

// just to show how the MockOAuth2Server can support multiple "token providers"/issuers at the same time
class AuthConfig(
    val providers: Map<String, TokenProvider> = emptyMap()
) {
    class TokenProvider(
        val wellKnownUrl: String,
        val acceptedAudience: String,
        val requiredClaims: Map<String, Any> = emptyMap()
    ) {
        private val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    setSerializationInclusion(JsonInclude.Include.NON_NULL)
                }
            }
        }

        val wellKnown: WellKnown = runBlocking { httpClient.prepareGet(wellKnownUrl).body() }
        val jwkProvider = JwkProviderBuilder(URL(wellKnown.jwksUri))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

        data class WellKnown(
            val issuer: String,
            @JsonProperty("jwks_uri")
            val jwksUri: String
        )
    }
}

internal fun JWTCredential.containsAll(claims: Map<String, Any>): Boolean =
    claims.filterNot {
        payload.contains(it.key, it.value)
    }.isEmpty()

internal fun Payload.contains(name: String, value: Any): Boolean =
    try {
        val type = if (value is Collection<*> && value.isNotEmpty()) {
            value.firstOrNull()!!.javaClass
        } else {
            value.javaClass
        }
        getClaim(name).asList(type)?.let {
            if (value is Collection<*>) {
                it.containsAll(value)
            } else {
                it.contains(value)
            }
        } ?: (getClaim(name)?.`as`(type) == value)
    } catch (t: Throwable) {
        log.error("received exception when checking for required claim $name=$value", t)
        false
    }
