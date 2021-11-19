package no.nav.security.mock.oauth2.http

import io.kotest.matchers.shouldBe
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
        routes.invoke(post("/something/shouldmatch")).body shouldBe "ANY"
        routes.invoke(options("/something/shouldmatch")).body shouldBe "OPTIONS"
        routes.invoke(get("/something/shouldmatch")).body shouldBe "GET"
    }
    @Test
    fun `routes from route builder should be matched`() {
        val route = routes {
            any("/foo") { OAuth2HttpResponse(status = 200, body = "foo") }
            get("/bar") { OAuth2HttpResponse(status = 200, body = "get bar") }
            post("/bar") { OAuth2HttpResponse(status = 200, body = "post bar") }
        }
        route.invoke(get("/foo")).body shouldBe "foo"
        route.invoke(get("/bar")).body shouldBe "get bar"
        route.invoke(post("/bar")).body shouldBe "post bar"
    }

    @Test
    fun `routes with matching path but incorrect method should return 405`() {
        val firstRoutes = routes {
            get("/first") { ok("firstget") }
            get("/first/second") { ok("second") }
            post("/first") { ok("firstpost") }
            get("/any") { ok("anyget") }
        }
        val finalRoutes = routes {
            attach(firstRoutes)
            any("/any") { ok("any") }
        }

        finalRoutes.invoke(post("/any")).body shouldBe "any"
        finalRoutes.invoke(post("/first/second")).status shouldBe 405
        finalRoutes.invoke(get("/notfound")).status shouldBe 404
    }
    private fun get(path: String) = request("http://localhost$path", "GET")
    private fun post(path: String, body: String? = "na") = request("http://localhost$path", "POST", body)
    private fun options(path: String, body: String? = "na") = request("http://localhost$path", "OPTIONS", body)

    private fun request(url: String, method: String, body: String? = null) =
        OAuth2HttpRequest(
            Headers.headersOf(),
            method,
            url.toHttpUrl(),
            body
        )

    private fun ok(body: String? = null) = OAuth2HttpResponse(status = 200, body = body)
}
