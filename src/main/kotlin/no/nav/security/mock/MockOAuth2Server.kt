package no.nav.security.mock

import OAuth2TokenIssuer
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import no.nav.security.mock.callback.MockOAuth2Callback
import no.nav.security.mock.extensions.authorizationEndpointUrl
import no.nav.security.mock.extensions.issuerUrl
import no.nav.security.mock.extensions.jwksUrl
import no.nav.security.mock.extensions.tokenEndpointUrl
import no.nav.security.mock.extensions.wellKnownUrl
import no.nav.security.mock.oauth2.OAuth2Dispatcher
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockWebServer
import java.io.IOException

internal class MockOAuth2Server(
    private val mockOAuth2Callbacks: Set<MockOAuth2Callback> = setOf(MockOAuth2Callback("default"))
) {
    private val mockWebServer: MockWebServer = MockWebServer()
    private val oAuth2TokenIssuer: OAuth2TokenIssuer = OAuth2TokenIssuer()

    fun start() {
        mockWebServer.start()
        mockWebServer.dispatcher = OAuth2Dispatcher(mockOAuth2Callbacks, oAuth2TokenIssuer)
    }

    @Throws(IOException::class)
    fun shutdown() {
        mockWebServer.shutdown()
    }

    fun wellKnownUrl(issuerId: String): HttpUrl = mockWebServer.wellKnownUrl(issuerId)
    fun tokenEndpointUrl(issuerId: String): HttpUrl = mockWebServer.tokenEndpointUrl(issuerId)
    fun jwksUrl(issuerId: String): HttpUrl = mockWebServer.jwksUrl(issuerId)
    fun issuerUrl(issuerId: String): HttpUrl = mockWebServer.issuerUrl(issuerId)
    fun authorizationEndpointUrl(issuerId: String): HttpUrl = mockWebServer.authorizationEndpointUrl(issuerId)

    fun issueAuthorizationCodeForTest(authenticationRequest: AuthenticationRequest): AuthorizationCode =
        oAuth2TokenIssuer.authorizationCodeResponse(authenticationRequest).authorizationCode

    fun baseUrl(): HttpUrl = mockWebServer.url("")
}