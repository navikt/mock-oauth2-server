package no.nav.security.mock.oauth2

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.id.ClientID
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import no.nav.security.mock.oauth2.extensions.asOAuth2HttpRequest
import no.nav.security.mock.oauth2.extensions.toAuthorizationEndpointUrl
import no.nav.security.mock.oauth2.extensions.toEndSessionEndpointUrl
import no.nav.security.mock.oauth2.extensions.toJwksUrl
import no.nav.security.mock.oauth2.extensions.toOAuth2HttpRequest
import no.nav.security.mock.oauth2.extensions.toResponse
import no.nav.security.mock.oauth2.extensions.toTokenEndpointUrl
import no.nav.security.mock.oauth2.extensions.toWellKnownUrl
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2HttpRequestHandler
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.server.MockWebServerConfig
import no.nav.security.mock.oauth2.server.asMockWebServerHttp4kServer
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.server.asServer

// TODO make open so others can extend?
@Suppress("unused", "MemberVisibilityCanBePrivate")
class MockOAuth2Server(
    val config: OAuth2Config = OAuth2Config(),
    val serverConfig: ServerConfig = MockWebServerConfig(),
    vararg customRoutes: RoutingHttpHandler
) {
    private val oAuth2HttpRequestHandler: OAuth2HttpRequestHandler = OAuth2HttpRequestHandler(config)
    private val oauth2Route: HttpHandler = {
      oAuth2HttpRequestHandler.handleRequest(it.toOAuth2HttpRequest()).toResponse()
    }

    private val routes = routes(*customRoutes,"/{issuerId:.*}" bind oauth2Route)


    private var httpServer: Http4kServer? = null//routes.asServer(serverConfig)

    //private val mockWebServer: MockWebServer = MockWebServer()
    var port: Int? = null
    //var dispatcher: Dispatcher = MockOAuth2Dispatcher(config)

    @JvmOverloads
    @Throws(IOException::class)
    fun start(
        inetAddress: InetAddress = InetAddress.getByName("localhost"),
        port: Int = 0
    ) {
        println("attempt to start server on port=$port")
        httpServer = routes.asServer(
            MockWebServerConfig(
                inetAddress,
                port
            )
        )
        httpServer?.start()
        this.port = httpServer?.port()
        /*
        httpServer = if (port != 0 || inetAddress != InetAddress.getByName("localhost")) {
            routes.asServer(MockWebServerConfig(address = inetAddress, port = port))
        } else {
            routes.asServer(serverConfig)
        }.start()*/

        /*mockWebServer.start(inetAddress, port)
        mockWebServer.dispatcher = dispatcher*/
    }

    @Throws(IOException::class)
    fun shutdown() {
        httpServer?.stop()
        //mockWebServer.shutdown()
    }

    fun url(path: String): HttpUrl = httpServer!!.asMockWebServerHttp4kServer().url(path)
    //fun url(path: String): HttpUrl = mockWebServer.url(path)

    @Deprecated("Use MockWebServer method/function instead", ReplaceWith("MockWebServer.enqueue()"))
    fun enqueueResponse(response: MockResponse) {
        throw UnsupportedOperationException("cannot enqueue MockResponse, use MockWebServer with QueueDispatcher")
    }

    //(dispatcher as MockOAuth2Dispatcher).enqueueResponse(response)
    fun enqueueCallback(oAuth2TokenCallback: OAuth2TokenCallback) = oAuth2HttpRequestHandler.enqueueTokenCallback(oAuth2TokenCallback)

    //(dispatcher as MockOAuth2Dispatcher).enqueueTokenCallback(oAuth2TokenCallback)
    fun takeRequest(): RecordedRequest = httpServer!!.asMockWebServerHttp4kServer().mockWebServer.takeRequest()
    //mockWebServer.takeRequest()

    fun wellKnownUrl(issuerId: String): HttpUrl = url(issuerId).toWellKnownUrl()

    //mockWebServer.url(issuerId).toWellKnownUrl()
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
            audience?.let { listOf(it) },
            claims,
            expiry
        )
    )
}


class MockOAuth2Dispatcher(
    config: OAuth2Config
) : Dispatcher() {
    private val httpRequestHandler: OAuth2HttpRequestHandler = OAuth2HttpRequestHandler(config)
    private val responseQueue: BlockingQueue<MockResponse> = LinkedBlockingQueue()

    fun enqueueResponse(mockResponse: MockResponse) = responseQueue.add(mockResponse)
    fun enqueueTokenCallback(oAuth2TokenCallback: OAuth2TokenCallback) = httpRequestHandler.enqueueTokenCallback(oAuth2TokenCallback)

    override fun dispatch(request: RecordedRequest): MockResponse =
        responseQueue.peek()?.let {
            responseQueue.take()
        } ?: mockResponse(httpRequestHandler.handleRequest(request.asOAuth2HttpRequest()))

    private fun mockResponse(response: OAuth2HttpResponse): MockResponse =
        MockResponse()
            .setHeaders(response.headers)
            .setResponseCode(response.status)
            .apply {
                response.body?.let { this.setBody(it) }
            }
}

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
