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
            route("shouldmatch") {
                OAuth2HttpResponse(status = 200, body = "ANY")
            }
        )
        routes.invoke(post("http://localhost:1234/something/shouldmatch")).body shouldBe "ANY"
        routes.invoke(get("http://localhost:1234/something/shouldmatch")).body shouldBe "GET"
    }

    @Test
    fun `routes from route builder should be matched`() {
        val route = routes {
            any("/foo") { OAuth2HttpResponse(status = 200, body = "foo") }
            get("/bar") { OAuth2HttpResponse(status = 200, body = "get bar") }
            post("/bar") {OAuth2HttpResponse(status = 200, body = "post bar") }
        }
        route.invoke(get("http://localhost/foo")).body shouldBe "foo"
        route.invoke(get("http://localhost/bar")).body shouldBe "get bar"
        route.invoke(post("http://localhost/bar")).body shouldBe "post bar"
    }

    private fun get(url: String) = request(url, "GET")
    private fun post(url: String, body: String? = "na") = request(url, "POST", body)

    private fun request(url: String, method: String, body: String? = null) =
        OAuth2HttpRequest(
            Headers.headersOf(),
            method,
            url.toHttpUrl(),
            body
        )
}
