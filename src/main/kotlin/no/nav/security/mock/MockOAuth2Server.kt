package no.nav.security.mock

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.id.ClientID
import no.nav.security.mock.callback.DefaultTokenCallback
import no.nav.security.mock.callback.TokenCallback
import no.nav.security.mock.extensions.authorizationEndpointUrl
import no.nav.security.mock.extensions.issuerUrl
import no.nav.security.mock.extensions.jwksUrl
import no.nav.security.mock.extensions.tokenEndpointUrl
import no.nav.security.mock.extensions.wellKnownUrl
import no.nav.security.mock.oauth2.OAuth2Dispatcher
import no.nav.security.mock.oauth2.OAuth2TokenProvider
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.IOException
import java.net.URI

class MockOAuth2Server(
    tokenCallbacks: Set<TokenCallback> = setOf(DefaultTokenCallback())
) {
    private val mockWebServer: MockWebServer = MockWebServer()
    private val tokenProvider: OAuth2TokenProvider = OAuth2TokenProvider()

    var dispatcher: Dispatcher = OAuth2Dispatcher(tokenProvider, tokenCallbacks)

    fun start() {
        mockWebServer.start()
        mockWebServer.dispatcher = dispatcher
    }

    @Throws(IOException::class)
    fun shutdown() {
        mockWebServer.shutdown()
    }

    fun enqueueCallback(tokenCallback: TokenCallback) =
        (dispatcher as OAuth2Dispatcher).enqueueJwtCallback(tokenCallback)

    fun takeRequest(): RecordedRequest = mockWebServer.takeRequest()

    fun wellKnownUrl(issuerId: String): HttpUrl = mockWebServer.wellKnownUrl(issuerId)
    fun tokenEndpointUrl(issuerId: String): HttpUrl = mockWebServer.tokenEndpointUrl(issuerId)
    fun jwksUrl(issuerId: String): HttpUrl = mockWebServer.jwksUrl(issuerId)
    fun issuerUrl(issuerId: String): HttpUrl = mockWebServer.issuerUrl(issuerId)
    fun authorizationEndpointUrl(issuerId: String): HttpUrl = mockWebServer.authorizationEndpointUrl(issuerId)
    fun baseUrl(): HttpUrl = mockWebServer.url("")

    fun issueToken(issuerId: String, clientId: String, tokenCallback: TokenCallback): SignedJWT {
        val uri = tokenEndpointUrl(issuerId)
        val issuerUrl = issuerUrl(issuerId)
        val tokenRequest = TokenRequest(
            uri.toUri(),
            ClientSecretBasic(ClientID(clientId), Secret("secret")),
            AuthorizationCodeGrant(AuthorizationCode("123"), URI.create("http://localhost"))
        )
        return tokenProvider.accessToken(tokenRequest, issuerUrl, null, tokenCallback)
    }
}