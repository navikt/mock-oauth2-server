package no.nav.security.mock.oauth2.http

import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.http.OAuth2HttpRouter.Companion.routes
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

internal class OAuth2HttpRouterTest {

    @Test
    fun `routes should be matched`() {
        val routes = routes(
            get("/shouldmatch") {
                OAuth2HttpResponse(status = 200, body = "GET")
            },
            options("/shouldmatch") {
                OAuth2HttpResponse(status = 200, body = "OPTIONS")
            },
            route("shouldmatch") {
                OAuth2HttpResponse(status = 200, body = "ANY")
            }
        )
        routes.invoke(post("http://localhost:1234/something/shouldmatch")).body shouldBe "ANY"
        routes.invoke(options("http://localhost:1234/something/shouldmatch")).body shouldBe "OPTIONS"
        routes.invoke(get("http://localhost:1234/something/shouldmatch")).body shouldBe "GET"
    }

    private fun get(url: String) = request(url, "GET")
    private fun post(url: String, body: String? = "na") = request(url, "POST", body)
    private fun options(url: String, body: String? = "na") = request(url, "OPTIONS", body)

    private fun request(url: String, method: String, body: String? = null) =
        OAuth2HttpRequest(
            Headers.headersOf(),
            method,
            url.toHttpUrl(),
            body
        )
}
