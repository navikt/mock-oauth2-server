package no.nav.security.mock.oauth2.http

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

internal class OAuth2HttpRouterTest {
    @Test
    fun `routes should be matched`() {
        val routes =
            routes(
                get("/shouldmatch") {
                    OAuth2HttpResponse(status = 200, body = "GET")
                },
                options("/shouldmatch") {
                    OAuth2HttpResponse(status = 200, body = "OPTIONS")
                },
                route("shouldmatch") {
                    OAuth2HttpResponse(status = 200, body = "ANY")
                },
            )
        routes.invoke(post("/something/shouldmatch")).body shouldBe "ANY"
        routes.invoke(options("/something/shouldmatch")).body shouldBe "OPTIONS"
        routes.invoke(get("/something/shouldmatch")).body shouldBe "GET"
    }

    @Test
    fun `routes from route builder should be matched`() {
        val route =
            routes {
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
        val firstRoutes =
            routes {
                get("/first") { ok("firstget") }
                get("/first/second") { ok("second") }
                post("/first") { ok("firstpost") }
                get("/any") { ok("anyget") }
            }
        val finalRoutes =
            routes {
                attach(firstRoutes)
                any("/any") { ok("any") }
            }

        finalRoutes.invoke(post("/any")).body shouldBe "any"
        finalRoutes.invoke(post("/first/second")).status shouldBe 405
        finalRoutes.invoke(get("/notfound")).status shouldBe 404
    }

    @Test
    fun `405 response should list the methods supported by the path in an Allow header`() {
        val firstRoutes =
            routes {
                get("/first") { ok("firstget") }
                post("/first") { ok("firstpost") }
                get("/second") { ok("second") }
            }
        val finalRoutes =
            routes {
                attach(firstRoutes)
                put("/first") { ok("firstput") }
            }

        finalRoutes.invoke(request("http://localhost/first", "DELETE")).asClue {
            it.status shouldBe 405
            it.headers["Allow"] shouldBe "GET, HEAD, POST, PUT"
        }
        finalRoutes.invoke(post("/second")).asClue {
            it.status shouldBe 405
            it.headers["Allow"] shouldBe "GET, HEAD"
        }
    }

    @Test
    fun `405 response for a path of a nested router should get the interceptors of that router`() {
        val nestedRoutes =
            routes {
                interceptors(ResponseInterceptor { _, response -> response.withHeader("fromInterceptor", "nested") })
                post("/foo") { ok("foo") }
            }
        val finalRoutes =
            routes {
                attach(nestedRoutes)
                put("/foo") { ok("put foo") }
            }

        finalRoutes.invoke(get("/foo")).asClue {
            it.status shouldBe 405
            it.headers["Allow"] shouldBe "POST, PUT"
            it.headers["fromInterceptor"] shouldBe "nested"
        }
    }

    @Test
    fun `HEAD request to a GET route should return the GET status and headers without a body`() {
        val routes =
            routes {
                get("/foo") { ok("foo body") }
            }

        routes.invoke(head("/foo")).asClue {
            it.status shouldBe 200
            it.headers["Content-Type"] shouldBe "text/plain"
            it.headers["Content-Length"] shouldBe "8"
            it.body shouldBe null
            it.bytesBody shouldBe null
        }
    }

    @Test
    fun `HEAD request to a path without a GET route should return 405 without a body`() {
        val routes =
            routes {
                post("/foo") { ok("foo") }
            }

        routes.invoke(head("/foo")).asClue {
            it.status shouldBe 405
            it.headers["Allow"] shouldBe "POST"
            it.body shouldBe null
        }
    }

    @Test
    fun `OPTIONS route for every path should not turn unknown paths into 405`() {
        val routes =
            routes {
                options { OAuth2HttpResponse(status = 204) }
                get("/foo") { ok("foo") }
            }

        routes.invoke(get("/notfound")).status shouldBe 404
        routes.invoke(options("/notfound")).status shouldBe 204
        routes.invoke(post("/foo")).asClue {
            it.status shouldBe 405
            it.headers["Allow"] shouldBe "GET, HEAD, OPTIONS"
        }
    }

    @Test
    fun `request and response interceptors should be applied on every route`() {
        val routes =
            routes {
                interceptors(
                    RequestInterceptor {
                        val headers =
                            it.headers
                                .newBuilder()
                                .add("yolo", "forever")
                                .build()
                        it.copy(headers = headers)
                    },
                    ResponseInterceptor { _, response ->
                        val headers =
                            response.headers
                                .newBuilder()
                                .add("fromInterceptor", "fromInterceptor")
                                .build()
                        response.copy(headers = headers)
                    },
                )
                get("/1") {
                    it.headers shouldContain ("yolo" to "forever")
                    ok("1")
                }
                get("/2") {
                    it.headers shouldContain ("yolo" to "forever")
                    ok("2")
                }
            }
        routes.invoke(get("/1")).asClue {
            it.headers shouldContainAll
                listOf(
                    "Content-Type" to "text/plain",
                    "fromInterceptor" to "fromInterceptor",
                )
            it.body shouldBe "1"
        }
        routes.invoke(get("/2")).asClue {
            it.headers shouldContainAll
                listOf(
                    "Content-Type" to "text/plain",
                    "fromInterceptor" to "fromInterceptor",
                )
            it.body shouldBe "2"
        }
    }

    @Test
    fun `response interceptors should be applied when route handler throws and exception handler returns a response`() {
        val routes =
            routes {
                interceptors(
                    ResponseInterceptor { _, response ->
                        val headers =
                            response.headers
                                .newBuilder()
                                .add("fromInterceptor", "fromInterceptor")
                                .build()
                        response.copy(headers = headers)
                    },
                )
                exceptionHandler { _, _ ->
                    OAuth2HttpResponse(status = 400, body = "fromExceptionHandler")
                }
                get("/boom") {
                    error("boom")
                }
            }

        routes.invoke(get("/boom")).asClue {
            it.status shouldBe 400
            it.headers shouldContain ("fromInterceptor" to "fromInterceptor")
            it.body shouldBe "fromExceptionHandler"
        }
    }

    @Test
    fun `exception handler response should still be returned when response interceptor throws in error path`() {
        val routes =
            routes {
                interceptors(
                    ResponseInterceptor { _, _ ->
                        error("interceptor boom")
                    },
                )
                exceptionHandler { _, _ ->
                    OAuth2HttpResponse(status = 400, body = "fromExceptionHandler")
                }
                get("/boom") {
                    error("boom")
                }
            }

        routes.invoke(get("/boom")).asClue {
            it.status shouldBe 400
            it.body shouldBe "fromExceptionHandler"
        }
    }

    @Test
    fun `routes with wildcard should be matched`() {
        val routes =
            routes(
                get("/shouldmatch/*") {
                    OAuth2HttpResponse(status = 200, body = "GET")
                },
                options("/shouldmatch/*") {
                    OAuth2HttpResponse(status = 200, body = "OPTIONS")
                },
                route("/shouldmatch/*") {
                    OAuth2HttpResponse(status = 200, body = "ANY")
                },
            )

        routes.invoke(post("/shouldmatch/1/3")).body shouldBe "ANY"
        routes.invoke(options("/shouldmatch/2/4")).body shouldBe "OPTIONS"
        routes.invoke(get("/shouldmatch/3/6")).body shouldBe "GET"
    }

    private fun get(path: String) = request("http://localhost$path", "GET")

    private fun head(path: String) = request("http://localhost$path", "HEAD")

    private fun post(
        path: String,
        body: String? = "na",
    ) = request("http://localhost$path", "POST", body)

    private fun options(
        path: String,
        body: String? = "na",
    ) = request("http://localhost$path", "OPTIONS", body)

    private fun request(
        url: String,
        method: String,
        body: String? = null,
    ) = OAuth2HttpRequest(
        Headers.headersOf(),
        method,
        url.toHttpUrl(),
        body,
    )

    private fun ok(body: String? = null) =
        OAuth2HttpResponse(
            headers = Headers.headersOf("Content-Type", "text/plain"),
            status = 200,
            body = body,
        )
}
