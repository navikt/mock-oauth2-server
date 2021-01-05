package no.nav.security.mock.oauth2.http

import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.http.OAuth2HttpRouter.Companion.routes
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

internal class OAuth2HttpRequestHandlerTest {

    // TODO: proper tests
    @Test
    fun `yolo`() {
        val handler = OAuth2HttpRequestHandler(OAuth2Config())
        val httpRouter = routes(route("") { handler.handleRequest(it) })
        val response = httpRouter.invoke(
            OAuth2HttpRequest(
                Headers.headersOf(),
                "GET",
                "http://localhost/someissuer/.well-known/openid-configuration".toHttpUrl()
            )
        )
        println("handler response: ${response.body}")
    }
}
