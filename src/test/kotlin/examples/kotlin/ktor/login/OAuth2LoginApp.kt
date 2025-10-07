package examples.kotlin.ktor.login

import com.auth0.jwt.JWT
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.resources.Resource
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.oauth
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.resources.Resources
import io.ktor.server.resources.href
import io.ktor.server.resources.resource
import io.ktor.server.response.respondText
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.param
import io.ktor.server.routing.routing
import io.ktor.server.util.url
import kotlinx.serialization.Serializable

fun main() {
    embeddedServer(Netty, port = 8080) {
        module(
            AuthConfig(
                listOf(
                    AuthConfig.IdProvider(
                        name = "google",
                        authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
                        tokenEndpoint = "https://oauth2.googleapis.com/token",
                    ),
                    AuthConfig.IdProvider(
                        name = "github",
                        authorizationEndpoint = "https://github.com/login/oauth/authorize",
                        tokenEndpoint = "https://github.com/login/oauth/access_token",
                    ),
                ),
            ),
        )
    }.start(true)
}

fun Application.module(authConfig: AuthConfig) {
    val idProviders = authConfig.providers.map { it.settings }.associateBy { it.name }

    install(Resources)
    install(Authentication) {
        oauth("oauth2") {
            client = httpClient
            providerLookup = {
                val t = this.parameters["type"].orEmpty()
                idProviders[t] ?: idProviders.values.first()
            }
            urlProvider = {
                url {
                    encodedPath = application.href(Login(it.name))
                }
            }
        }
    }

    routing {
        trace { application.log.info(it.buildText()) }
        authenticate("oauth2") {
            get {
                call.respondText("nothing to see here really")
            }
            resource<Login> {
                // /login/{type}?error=...
                param("error") {
                    get {
                        call.respondText(
                            ContentType.Text.Html,
                            HttpStatusCode.BadRequest
                        ) {
                            "received error on login: ${call.parameters.getAll("error").orEmpty()}"
                        }
                    }
                }
                // /login/{type}
                get {
                    call.respondText("welcome ${call.subject()}")
                }
            }

        }
    }
}

@Serializable
@Resource("/login/{type?}")
class Login(
    val type: String? = "",
)

class AuthConfig(
    val providers: List<IdProvider> = emptyList(),
) {
    class IdProvider(
        val name: String,
        authorizationEndpoint: String,
        tokenEndpoint: String,
    ) {
        val settings =
            OAuthServerSettings.OAuth2ServerSettings(
                name = name,
                authorizeUrl = authorizationEndpoint,
                accessTokenUrl = tokenEndpoint,
                requestMethod = HttpMethod.Post,
                clientId = "***",
                clientSecret = "***",
                defaultScopes = listOf("openid"),
            )
    }
}

private fun ApplicationCall.subject(): String? {
    val idToken = authentication.principal<OAuthAccessTokenResponse.OAuth2>()?.extraParameters?.get("id_token")
    // should verify id_token before use as ktor doesnt, however left out in the example
    return if (idToken != null) {
        JWT.decode(idToken).subject
    } else {
        null
    }
}

internal val httpClient =
    HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            }
        }
    }
