package no.nav.security.mock.oauth2

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.AuthorizationGrant
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.id.ClientID
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import mu.KotlinLogging
import no.nav.security.mock.oauth2.extensions.toAuthorizationEndpointUrl
import no.nav.security.mock.oauth2.extensions.toEndSessionEndpointUrl
import no.nav.security.mock.oauth2.extensions.toJwksUrl
import no.nav.security.mock.oauth2.extensions.toOAuth2AuthorizationServerMetadataUrl
import no.nav.security.mock.oauth2.extensions.toTokenEndpointUrl
import no.nav.security.mock.oauth2.extensions.toWellKnownUrl
import no.nav.security.mock.oauth2.http.MockWebServerWrapper
import no.nav.security.mock.oauth2.http.OAuth2HttpRequestHandler
import no.nav.security.mock.oauth2.http.OAuth2HttpRouter
import no.nav.security.mock.oauth2.http.OAuth2HttpRouter.Companion.routes
import no.nav.security.mock.oauth2.http.Route
import no.nav.security.mock.oauth2.http.route
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

private val log = KotlinLogging.logger { }

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class MockOAuth2Server(
    val config: OAuth2Config = OAuth2Config(),
    vararg additionalRoutes: Route
) {
    constructor(vararg additionalRoutes: Route) : this(config = OAuth2Config(), additionalRoutes = additionalRoutes)

    private val httpServer = config.httpServer
    private val defaultRequestHandler: OAuth2HttpRequestHandler = OAuth2HttpRequestHandler(config)
    private val router: OAuth2HttpRouter = routes(
        *additionalRoutes,
        route("") {
            defaultRequestHandler.handleRequest(it)
        }
    )

    @JvmOverloads
    @Throws(IOException::class)
    fun start(port: Int = 0) = start(InetAddress.getByName("localhost"), port)

    @Throws(IOException::class)
    fun start(inetAddress: InetAddress, port: Int) {
        log.debug("attempt to start server on port=$port")
        httpServer.start(inetAddress, port, router)
    }

    @Throws(IOException::class)
    fun shutdown() {
        httpServer.stop()
    }

    fun url(path: String): HttpUrl = httpServer.url(path)

    @Deprecated("Use MockWebServer method/function instead", ReplaceWith("MockWebServer.enqueue()"))
    fun enqueueResponse(response: MockResponse) {
        throw UnsupportedOperationException("cannot enqueue MockResponse, please use the MockWebServer directly with QueueDispatcher")
    }

    fun enqueueCallback(oAuth2TokenCallback: OAuth2TokenCallback) = defaultRequestHandler.enqueueTokenCallback(oAuth2TokenCallback)

    @JvmOverloads
    fun takeRequest(timeout: Long = 2, unit: TimeUnit = TimeUnit.SECONDS): RecordedRequest =
        (httpServer as? MockWebServerWrapper)?.mockWebServer?.let {
            it.takeRequest(timeout, unit) ?: throw RuntimeException("no request found in queue within timeout $timeout $unit")
        } ?: throw UnsupportedOperationException("can only takeRequest when httpServer is of type MockWebServer")

    fun wellKnownUrl(issuerId: String): HttpUrl = url(issuerId).toWellKnownUrl()
    fun oauth2AuthorizationServerMetadataUrl(issuerId: String): HttpUrl = url(issuerId).toOAuth2AuthorizationServerMetadataUrl()
    fun tokenEndpointUrl(issuerId: String): HttpUrl = url(issuerId).toTokenEndpointUrl()
    fun jwksUrl(issuerId: String): HttpUrl = url(issuerId).toJwksUrl()
    fun issuerUrl(issuerId: String): HttpUrl = url(issuerId)
    fun authorizationEndpointUrl(issuerId: String): HttpUrl = url(issuerId).toAuthorizationEndpointUrl()
    fun endSessionEndpointUrl(issuerId: String): HttpUrl = url(issuerId).toEndSessionEndpointUrl()
    fun baseUrl(): HttpUrl = url("")

    fun issueToken(issuerId: String, clientId: String, tokenCallback: OAuth2TokenCallback): SignedJWT {
        val uri = tokenEndpointUrl(issuerId)
        val issuerUrl = issuerUrl(issuerId)
        val tokenRequest = TokenRequest(
            uri.toUri(),
            ClientSecretBasic(ClientID(clientId), Secret("secret")),
            AuthorizationCodeGrant(AuthorizationCode("123"), URI.create("http://localhost"))
        )
        return config.tokenProvider.accessToken(tokenRequest, issuerUrl, tokenCallback, null)
    }

    @JvmOverloads
    fun issueToken(
        issuerId: String = "default",
        subject: String = UUID.randomUUID().toString(),
        audience: String? = "default",
        claims: Map<String, Any> = emptyMap(),
        expiry: Long = 3600
    ): SignedJWT = issueToken(
        issuerId,
        "default",
        DefaultOAuth2TokenCallback(
            issuerId,
            subject,
            JOSEObjectType.JWT.type,
            audience?.let { listOf(it) },
            claims,
            expiry
        )
    )

    @JvmOverloads
    fun anyToken(issuerUrl: HttpUrl, claims: Map<String, Any>, expiry: Duration = Duration.ofHours(1)): SignedJWT {
        val jwtClaimsSet = claims.toJwtClaimsSet()
        val mockGrant: AuthorizationGrant = object : AuthorizationGrant(GrantType("MockGrant")) {
            override fun toParameters(): MutableMap<String, MutableList<String>> = mutableMapOf()
        }
        return this.config.tokenProvider.exchangeAccessToken(
            TokenRequest(URI.create("http://mockgrant"), ClientID("mockclientid"), mockGrant),
            issuerUrl,
            jwtClaimsSet,
            DefaultOAuth2TokenCallback(
                audience = jwtClaimsSet.audience,
                expiry = expiry.toMillis()
            )
        )
    }
}

internal fun Map<String, Any>.toJwtClaimsSet(): JWTClaimsSet =
    JWTClaimsSet.Builder()
        .apply {
            this@toJwtClaimsSet.forEach {
                this.claim(it.key, it.value)
            }
        }.build()

fun <R> withMockOAuth2Server(
    test: MockOAuth2Server.() -> R
): R {
    val server = MockOAuth2Server()
    server.start()
    try {
        return server.test()
    } finally {
        server.shutdown()
    }
}
