package no.nav.security.mock.oauth2.mockwebserver

import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.callback.TokenCallback
import no.nav.security.mock.oauth2.http.OAuth2HttpRequestHandler
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

private val log = KotlinLogging.logger {}

class OAuth2Dispatcher(
    config: OAuth2Config
) : Dispatcher() {
    private val httpRequestHandler: OAuth2HttpRequestHandler = OAuth2HttpRequestHandler(config)

    fun enqueueTokenCallback(tokenCallback: TokenCallback) = httpRequestHandler.enqueueJwtCallback(tokenCallback)

    override fun dispatch(request: RecordedRequest): MockResponse =
        mockResponse(httpRequestHandler.handleRequest(request.asOAuth2HttpRequest()))

    private fun mockResponse(response: OAuth2HttpResponse): MockResponse =
        MockResponse()
            .setHeaders(response.headers)
            .setResponseCode(response.status)
            .apply {
                response.body?.let { this.setBody(it) }
            }
}
