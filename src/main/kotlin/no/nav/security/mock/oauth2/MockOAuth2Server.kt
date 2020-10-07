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
import no.nav.security.mock.oauth2.extensions.toTokenEndpointUrl
import no.nav.security.mock.oauth2.extensions.toWellKnownUrl
import no.nav.security.mock.oauth2.http.OAuth2HttpRequestHandler
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

// TODO make open so others can extend?
@Suppress("unused", "MemberVisibilityCanBePrivate")
class MockOAuth2Server(
    val config: OAuth2Config = OAuth2Config()
) {
    private val mockWebServer: MockWebServer = MockWebServer()

    var dispatcher: Dispatcher = MockOAuth2Dispatcher(config)

    @JvmOverloads
    @Throws(IOException::class)
    fun start(
        inetAddress: InetAddress = InetAddress.getByName("localhost"),
        port: Int = 0
    ) {
        mockWebServer.start(inetAddress, port)
        mockWebServer.dispatcher = dispatcher
    }

    @Throws(IOException::class)
    fun shutdown() {
        mockWebServer.shutdown()
    }

    fun url(path: String): HttpUrl = mockWebServer.url(path)
    fun enqueueResponse(response: MockResponse) = (dispatcher as MockOAuth2Dispatcher).enqueueResponse(response)
    fun enqueueCallback(oAuth2TokenCallback: OAuth2TokenCallback) = (dispatcher as MockOAuth2Dispatcher).enqueueTokenCallback(oAuth2TokenCallback)
    fun takeRequest(): RecordedRequest = mockWebServer.takeRequest()

    fun wellKnownUrl(issuerId: String): HttpUrl = mockWebServer.url(issuerId).toWellKnownUrl()
    fun tokenEndpointUrl(issuerId: String): HttpUrl = mockWebServer.url(issuerId).toTokenEndpointUrl()
    fun jwksUrl(issuerId: String): HttpUrl = mockWebServer.url(issuerId).toJwksUrl()
    fun issuerUrl(issuerId: String): HttpUrl = mockWebServer.url(issuerId)
    fun authorizationEndpointUrl(issuerId: String): HttpUrl = mockWebServer.url(issuerId).toAuthorizationEndpointUrl()
    fun endSessionEndpointUrl(issuerId: String): HttpUrl = mockWebServer.url(issuerId).toEndSessionEndpointUrl()
    fun baseUrl(): HttpUrl = mockWebServer.url("")

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
