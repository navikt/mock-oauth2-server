package no.nav.security.mock.oauth2.http

import mu.KotlinLogging
import no.nav.security.mock.oauth2.extensions.endsWith

private val log = KotlinLogging.logger { }

typealias RequestHandler = (OAuth2HttpRequest) -> OAuth2HttpResponse

// @TODO: should a request with incorrect method fail with 400 bad request to a known URL instead of 404 not found (YES, but how generically)?
interface Route : RequestHandler {
    fun match(request: OAuth2HttpRequest): Boolean

    class Builder {
        val routes: MutableList<Route> = mutableListOf()

        fun attach(vararg route: Route) = apply {
            route.forEach {
                routes.add(it)
            }
        }

        fun any(vararg path: String, requestHandler: RequestHandler) = apply {
            path.forEach {
                addRoute(it, null, requestHandler)
            }
        }

        fun get(vararg path: String, requestHandler: RequestHandler) = apply {
            path.forEach {
                addRoute(it, "GET", requestHandler)
            }
        }

        fun post(path: String, requestHandler: RequestHandler) = apply {
            addRoute(path, "POST", requestHandler)
        }

        fun put(path: String, requestHandler: RequestHandler) = apply {
            addRoute(path, "PUT", requestHandler)
        }

        private fun addRoute(path: String, method: String? = null, requestHandler: RequestHandler) {
            routes.add(routeFromPathAndMethod(path, method, requestHandler))
        }

        fun build(): Route = object : Route {

            override fun match(request: OAuth2HttpRequest): Boolean = (routes.firstOrNull { it.match(request) } != null).also {
                log.debug("route match invoked for request: ${request.url.encodedPath}, match=$it")
                log.debug("searched through ${routes.size} routes")
            }

            override fun invoke(request: OAuth2HttpRequest): OAuth2HttpResponse =
                routes.also {
                    log.debug("attempt to route request with url=${request.url}")
                }.firstOrNull { it.match(request) }?.invoke(request)
                    ?: OAuth2HttpResponse(status = 404, body = "no route found")
                        .also { log.debug("no handler found returning 404") }

            override fun toString(): String {
                return routes.toString()
            }
        }
    }
}

class OAuth2HttpRouter(
    private val routes: MutableList<Route> = mutableListOf()
) : Route {

    var notFoundHandler: (OAuth2HttpRequest) -> OAuth2HttpResponse = { OAuth2HttpResponse(status = 404, body = "no route found") }

    override fun invoke(request: OAuth2HttpRequest): OAuth2HttpResponse = handle(request)

    override fun match(request: OAuth2HttpRequest): Boolean = routes.firstOrNull { it.match(request) } != null

    override fun toString(): String = routes.toString()

    private fun handle(request: OAuth2HttpRequest): OAuth2HttpResponse =
        routes.also { log.debug("attempt to route request with url=${request.url}") }.firstOrNull { it.match(request) }
            ?.invoke(request)
            ?: notFoundHandler.invoke(request).also { log.debug("no handler found, using notFoundHandler") }

    companion object {
        fun routes(vararg route: Route): OAuth2HttpRouter = OAuth2HttpRouter(mutableListOf(*route))
    }
}

fun routes(vararg route: Route): Route = routes {
    attach(*route)
}

fun routes(config: Route.Builder.() -> Unit): Route = Route.Builder().apply(config).build()

@JvmOverloads
fun route(path: String, method: String? = null, requestHandler: RequestHandler): Route =
    routeFromPathAndMethod(path, method, requestHandler)

fun put(path: String, requestHandler: RequestHandler): Route =
    routeFromPathAndMethod(path, "PUT", requestHandler)

fun post(path: String, requestHandler: RequestHandler): Route =
    routeFromPathAndMethod(path, "POST", requestHandler)

fun get(path: String, requestHandler: RequestHandler): Route =
    routeFromPathAndMethod(path, "GET", requestHandler)

private fun routeFromPathAndMethod(path: String, method: String? = null, requestHandler: RequestHandler): Route =
    object : Route {
        override fun match(request: OAuth2HttpRequest): Boolean =
            if (request.url.endsWith(path)) {
                method?.let { it == request.method } ?: true
            } else {
                false
            }

        override fun invoke(request: OAuth2HttpRequest): OAuth2HttpResponse = requestHandler.invoke(request)
        override fun toString(): String = "path=$path, method=$method"
    }
