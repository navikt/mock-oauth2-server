package no.nav.security.mock.oauth2.examples

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier
import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.util.DefaultResourceRetriever
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.nimbusds.oauth2.sdk.id.Issuer
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.net.URL
import java.util.HashSet

private val log = KotlinLogging.logger {}

abstract class AbstractExampleApp(oauth2DiscoveryUrl: String) {

    val oauth2Client: OkHttpClient = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .build()

    val metadata = OIDCProviderMetadata.parse(DefaultResourceRetriever().retrieveResource(URL(oauth2DiscoveryUrl)).content)

    lateinit var exampleApp: MockWebServer

    fun start() {
        exampleApp = MockWebServer()
        exampleApp.start()
        exampleApp.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return runCatching {
                    handleRequest(request)
                }.fold(
                    onSuccess = { result -> result },
                    onFailure = { error ->
                        log.error("received unhandled exception.", error)
                        MockResponse()
                            .setResponseCode(500)
                            .setBody("unhandled exception with message ${error.message}")
                    },
                )
            }
        }
    }

    fun shutdown() {
        exampleApp.shutdown()
    }

    fun url(path: String): HttpUrl = exampleApp.url(path)

    fun retrieveJwks(): JWKSet {
        return oauth2Client.newCall(
            Request.Builder()
                .url(metadata.jwkSetURI.toURL())
                .get()
                .build(),
        ).execute().body?.string()?.let {
            JWKSet.parse(it)
        } ?: throw RuntimeException("could not retrieve jwks")
    }

    fun verifyJwt(jwt: String, issuer: Issuer, jwkSet: JWKSet): JWTClaimsSet {
        val jwtProcessor: ConfigurableJWTProcessor<SecurityContext?> = DefaultJWTProcessor()
        jwtProcessor.jwsTypeVerifier = DefaultJOSEObjectTypeVerifier(JOSEObjectType("JWT"))
        val keySelector: JWSKeySelector<SecurityContext?> = JWSVerificationKeySelector(
            JWSAlgorithm.RS256,
            ImmutableJWKSet(jwkSet),
        )
        jwtProcessor.jwsKeySelector = keySelector
        jwtProcessor.jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(
            JWTClaimsSet.Builder().issuer(issuer.toString()).build(),
            HashSet(listOf("sub", "iat", "exp", "aud")),
        )
        return try {
            jwtProcessor.process(jwt, null)
        } catch (e: Exception) {
            throw RuntimeException("invalid jwt.", e)
        }
    }

    fun bearerToken(request: RecordedRequest): String? =
        request.headers["Authorization"]
            ?.split("Bearer ")
            ?.let { it[1] }

    fun notAuthorized(): MockResponse = MockResponse().setResponseCode(401)

    fun json(value: Any): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(ObjectMapper().writeValueAsString(value))

    abstract fun handleRequest(request: RecordedRequest): MockResponse
}
