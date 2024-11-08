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
import mu.KotlinLogging
import no.nav.security.mock.oauth2.extensions.toAuthorizationEndpointUrl
import no.nav.security.mock.oauth2.extensions.toEndSessionEndpointUrl
import no.nav.security.mock.oauth2.extensions.toJwksUrl
import no.nav.security.mock.oauth2.extensions.toOAuth2AuthorizationServerMetadataUrl
import no.nav.security.mock.oauth2.extensions.toRevocationEndpointUrl
import no.nav.security.mock.oauth2.extensions.toTokenEndpointUrl
import no.nav.security.mock.oauth2.extensions.toUserInfoUrl
import no.nav.security.mock.oauth2.extensions.toWellKnownUrl
import no.nav.security.mock.oauth2.http.MockWebServerWrapper
import no.nav.security.mock.oauth2.http.OAuth2HttpRequestHandler
import no.nav.security.mock.oauth2.http.RequestHandler
import no.nav.security.mock.oauth2.http.Route
import no.nav.security.mock.oauth2.http.routes
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class MockOAuth2Server(
    val config: OAuth2Config = OAuth2Config(),
    vararg additionalRoutes: Route,
) {
    constructor(vararg additionalRoutes: Route) : this(config = OAuth2Config(), additionalRoutes = additionalRoutes)
    constructor(config: OAuth2Config) : this(config = config, additionalRoutes = emptyArray())

    private val httpServer = config.httpServer
    private val defaultRequestHandler: OAuth2HttpRequestHandler = OAuth2HttpRequestHandler(config)
    private val router: RequestHandler =
        routes(
            *additionalRoutes,
            defaultRequestHandler.authorizationServer,
        )

    /**
     * Starts the [MockOAuth2Server] on the localhost interface.
     *
     * @param port The port the server should listen on, a value of 0 (which is the default) selects any available port.
     *
     * @exception OAuth2Exception Runtime error if unable to start server.
     */
    @JvmOverloads
    fun start(port: Int = 0) =
        try {
            start(InetAddress.getByName("localhost"), port)
        } catch (ex: IOException) {
            throw OAuth2Exception("unable to start server: ${ex.message}", ex)
        }

    /**
     * Starts the [MockOAuth2Server] on the given [inetAddress] IP address at the given [port].
     *
     * @param port The port that the server should listen on, a value of 0 selects any available port.
     * @param inetAddress The IP address that the server should bind to.
     *
     * @exception OAuth2Exception Runtime error if unable to start server.
     */
    fun start(
        inetAddress: InetAddress,
        port: Int,
    ) {
        log.debug("attempt to start server on port=$port")
        httpServer.start(inetAddress, port, router)
    }

    /**
     * Gracefully shuts down the [MockOAuth2Server].
     *
     * @exception OAuth2Exception Runtime error if unable to shut down server
     */
    fun shutdown() {
        try {
            httpServer.stop()
        } catch (ex: IOException) {
            throw OAuth2Exception("unable to shutdown server: ${ex.message}", ex)
        }
    }

    /**
     * Returns the authorization server's issuer identifier for the given [path].
     * The identifier is a URL without query or fragment components, e.g. `http://localhost:8080/some-issuer`.
     *
     * @param path The path or identifier for the issuer.
     */
    fun url(path: String): HttpUrl = httpServer.url(path)

    @Deprecated("Use MockWebServer method/function instead", ReplaceWith("MockWebServer.enqueue()"))
    fun enqueueResponse(
        @Suppress("UNUSED_PARAMETER") response: MockResponse,
    ): Unit = throw UnsupportedOperationException("cannot enqueue MockResponse, please use the MockWebServer directly with QueueDispatcher")

    /**
     * Enqueues a callback at the server's HTTP request handler.
     * This allows for customization of the token that the server issues whenever a Relying Party requests a token from the [tokenEndpointUrl].
     *
     * @param oAuth2TokenCallback A callback that implements the [OAuth2TokenCallback] interface.
     */
    fun enqueueCallback(oAuth2TokenCallback: OAuth2TokenCallback) = defaultRequestHandler.enqueueTokenCallback(oAuth2TokenCallback)

    /**
     * Awaits the next HTTP request (waiting up to the specified wait time if necessary), removes it from the queue,
     * and returns it. Callers should use this to verify the request was sent as intended within the
     * given time.
     *
     * @param timeout How long to wait before giving up, in units of [unit]
     * @param unit A [TimeUnit] determining how to interpret the [timeout] parameter
     */
    @JvmOverloads
    fun takeRequest(
        timeout: Long = 2,
        unit: TimeUnit = TimeUnit.SECONDS,
    ): RecordedRequest =
        (httpServer as? MockWebServerWrapper)?.mockWebServer?.let {
            it.takeRequest(timeout, unit) ?: throw RuntimeException("no request found in queue within timeout $timeout $unit")
        } ?: throw UnsupportedOperationException("can only takeRequest when httpServer is of type MockWebServer")

    /**
     * Returns the authorization server's well-known OpenID Connect metadata discovery URL for the given [issuerId].
     *
     * E.g. `http://localhost:8080/some-issuer/.well-known/openid-configuration`.
     *
     * See also [OpenID Provider metadata](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata).
     *
     * @param issuerId The path or identifier for the issuer.
     */
    fun wellKnownUrl(issuerId: String): HttpUrl = url(issuerId).toWellKnownUrl()

    /**
     * Returns the authorization server's well-known OAuth2 metadata discovery URL for the given [issuerId].
     *
     * E.g. `http://localhost:8080/some-issuer/.well-known/oauth-authorization-server`.
     *
     * See also [RFC8414 - OAuth 2.0 Authorization Server Metadata](https://datatracker.ietf.org/doc/html/rfc8414).
     *
     * @param issuerId The path or identifier for the issuer.
     */
    fun oauth2AuthorizationServerMetadataUrl(issuerId: String): HttpUrl = url(issuerId).toOAuth2AuthorizationServerMetadataUrl()

    /**
     * Returns the authorization server's `token_endpoint` for the given [issuerId].
     *
     * E.g. `http://localhost:8080/some-issuer/token`.
     *
     * @param issuerId The path or identifier for the issuer.
     */
    fun tokenEndpointUrl(issuerId: String): HttpUrl = url(issuerId).toTokenEndpointUrl()

    /**
     * Returns the authorization server's `jwks_uri` for the given [issuerId].
     *
     * E.g. `http://localhost:8080/some-issuer/jwks`.
     *
     * @param issuerId The path or identifier for the issuer.
     */
    fun jwksUrl(issuerId: String): HttpUrl = url(issuerId).toJwksUrl()

    /**
     * Returns the authorization server's `issuer` for the given [issuerId].
     *
     * See [url].
     */
    fun issuerUrl(issuerId: String): HttpUrl = url(issuerId)

    /**
     * Returns the authorization server's `authorization_endpoint` for the given [issuerId].
     *
     * E.g. `http://localhost:8080/some-issuer/authorize`.
     *
     * @param issuerId The path or identifier for the issuer.
     */
    fun authorizationEndpointUrl(issuerId: String): HttpUrl = url(issuerId).toAuthorizationEndpointUrl()

    /**
     * Returns the authorization server's `end_session_endpoint` for the given [issuerId].
     *
     * E.g. `http://localhost:8080/some-issuer/endsession`.
     *
     * @param issuerId The path or identifier for the issuer.
     */
    fun endSessionEndpointUrl(issuerId: String): HttpUrl = url(issuerId).toEndSessionEndpointUrl()

    /**
     * Returns the authorization server's `revocation_endpoint` for the given [issuerId].
     *
     * E.g. `http://localhost:8080/some-issuer/revoke`.
     *
     * @param issuerId The path or identifier for the issuer.
     */
    fun revocationEndpointUrl(issuerId: String): HttpUrl = url(issuerId).toRevocationEndpointUrl()

    /**
     * Returns the authorization server's `userinfo_endpoint` for the given [issuerId].
     *
     * E.g. `http://localhost:8080/some-issuer/userinfo`.
     *
     * @param issuerId The path or identifier for the issuer.
     */
    fun userInfoUrl(issuerId: String): HttpUrl = url(issuerId).toUserInfoUrl()

    /**
     * Returns the base URL for this server.
     */
    fun baseUrl(): HttpUrl = url("")

    /**
     * Issues a signed JWT as part of the authorization code grant.
     *
     * @param issuerId The path or identifier for the issuer.
     * @param clientId The identifier for the client or Relying Party that requests the token.
     * @param tokenCallback A callback that implements the [OAuth2TokenCallback] interface for token customization.
     */
    fun issueToken(
        issuerId: String,
        clientId: String,
        tokenCallback: OAuth2TokenCallback,
    ): SignedJWT {
        val uri = tokenEndpointUrl(issuerId)
        val issuerUrl = issuerUrl(issuerId)
        val tokenRequest =
            TokenRequest
                .Builder(
                    uri.toUri(),
                    ClientSecretBasic(ClientID(clientId), Secret("secret")),
                    AuthorizationCodeGrant(AuthorizationCode("123"), URI.create("http://localhost")),
                ).build()
        return config.tokenProvider.accessToken(tokenRequest, issuerUrl, tokenCallback, null)
    }

    /**
     * Convenience method for issuing a signed JWT with default values.
     *
     * See [issueToken].
     */
    @JvmOverloads
    fun issueToken(
        issuerId: String = "default",
        subject: String = UUID.randomUUID().toString(),
        audience: String? = "default",
        claims: Map<String, Any> = emptyMap(),
        expiry: Long = 3600,
    ): SignedJWT =
        issueToken(
            issuerId,
            "default",
            DefaultOAuth2TokenCallback(
                issuerId,
                subject,
                JOSEObjectType.JWT.type,
                audience?.let { listOf(it) },
                claims,
                expiry,
            ),
        )

    /**
     * Issues a signed JWT for a given [issuerUrl] containing the input set of [claims].
     * The JWT's signature can be verified with the server's keys found at the [jwksUrl] endpoint.
     */
    @JvmOverloads
    fun anyToken(
        issuerUrl: HttpUrl,
        claims: Map<String, Any>,
        expiry: Duration = Duration.ofHours(1),
    ): SignedJWT {
        val jwtClaimsSet = claims.toJwtClaimsSet()
        val mockGrant: AuthorizationGrant =
            object : AuthorizationGrant(GrantType("MockGrant")) {
                override fun toParameters(): MutableMap<String, MutableList<String>> = mutableMapOf()
            }
        val request =
            TokenRequest
                .Builder(
                    URI.create("http://mockgrant"),
                    ClientID("mockclientid"),
                    mockGrant,
                ).build()
        return this.config.tokenProvider.exchangeAccessToken(
            request,
            issuerUrl,
            jwtClaimsSet,
            DefaultOAuth2TokenCallback(
                audience = jwtClaimsSet.audience,
                expiry = expiry.toMillis(),
            ),
        )
    }

    companion object {
        /**
         * This attempts to reference a method that does not exist in com.squareup.okio:okio < 2.4.0,
         * and incidentally also com.squareup.okhttp3:mockwebserver < 4.3.0.
         *
         * The method is required by mock-oauth2-server, see [no.nav.security.mock.oauth2.extensions.RecordedRequest.asOAuth2HttpRequest()].
         *
         * If the block throws a RuntimeException, an incompatible version of the okio library was included in the classpath.
         *
         * This is true for e.g. Spring Boot projects, which as of version 2.6.1 still uses mockwebserver 3.14.9 as the
         * [default managed dependency version](https://docs.spring.io/spring-boot/docs/2.6.1/reference/html/dependency-versions.html).
         *
         * We recommend that users of this library use a matching version of mockwebserver.
         */
        init {
            try {
                Buffer().copy()
            } catch (e: NoSuchMethodError) {
                throw RuntimeException("Unsupported version of com.squareup.okhttp3:mockwebserver in classpath. Version should be >= 4.9.2", e)
            }
        }
    }
}

internal fun Map<String, Any>.toJwtClaimsSet(): JWTClaimsSet =
    JWTClaimsSet
        .Builder()
        .apply {
            this@toJwtClaimsSet.forEach {
                this.claim(it.key, it.value)
            }
        }.build()

fun <R> withMockOAuth2Server(
    config: OAuth2Config = OAuth2Config(),
    test: MockOAuth2Server.() -> R,
): R {
    val server = MockOAuth2Server(config)
    server.start()
    try {
        return server.test()
    } finally {
        server.shutdown()
    }
}
