package examples.kotlin.ktor.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.http.Headers
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import io.ktor.util.InternalAPI
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

val httpClient = HttpClient(CIO) {
    install(JsonFeature) {
        serializer = JacksonSerializer {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }
}

@OptIn(InternalAPI::class)
suspend fun HttpClient.tokenRequest(url: String, auth: Auth, params: Map<String, String>) =
    submitForm<TokenResponse>(
        url = url,
        formParameters = Parameters.build {
            auth.parameters.forEach {
                append(it.key, it.value)
            }
            params.forEach {
                append(it.key, it.value)
            }
        }
    ) {
        auth.headers.forEach { s, list -> header(s, list.first()) }
    }

suspend fun HttpClient.clientCredentialsGrant(url: String, auth: Auth, scope: String) =
    tokenRequest(
        url = url,
        auth = auth,
        params = mapOf(
            "grant_type" to "client_credentials",
            "scope" to scope
        )
    )

suspend fun HttpClient.onBehalfOfGrant(url: String, auth: Auth, token: String, scope: String) =
    tokenRequest(
        url = url,
        auth = auth,
        params = mapOf(
            "scope" to scope,
            "grant_type" to "urn:ietf:params:oauth:grant-type:jwt-bearer",
            "requested_token_use" to "on_behalf_of",
            "assertion" to token
        )
    )

class Auth internal constructor(
    val parameters: Map<String, String> = emptyMap(),
    val headers: Headers = Headers.Empty
) {
    companion object {
        private const val CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"

        fun ClientSecretBasic(clientId: String, clientSecret: String): Auth =
            Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray(StandardCharsets.UTF_8)).let {
                Auth(headers = headersOf("Authorization", "Basic $it"))
            }

        fun PrivateKeyJwt(jwt: String): Auth = Auth(
            parameters = mapOf(
                "client_assertion_type" to CLIENT_ASSERTION_TYPE,
                "client_assertion" to jwt
            )
        )

        fun PrivateKeyJwt(
            keyPair: KeyPair,
            clientId: String,
            tokenEndpoint: String,
            expiry: Duration = Duration.ofSeconds(120)
        ): Auth = Auth(
            parameters = mapOf(
                "client_assertion_type" to CLIENT_ASSERTION_TYPE,
                "client_assertion" to keyPair.clientAssertion(clientId, tokenEndpoint, expiry)
            )
        )

        private fun KeyPair.clientAssertion(
            clientId: String,
            tokenEndpoint: String,
            expiry: Duration = Duration.ofSeconds(120)
        ): String {
            val now = Instant.now()
            return JWT.create()
                .withAudience(tokenEndpoint)
                .withIssuer(clientId)
                .withSubject(clientId)
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Date.from(now))
                .withNotBefore(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(expiry.toSeconds())))
                .sign(Algorithm.RSA256(this.public as RSAPublicKey, this.private as RSAPrivateKey))
        }
    }
}

data class TokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Int,
    @JsonProperty("token_type")
    val tokenType: String
)
