package no.nav.security.mock.oauth2.http

import mu.KotlinLogging
import no.nav.security.mock.oauth2.extensions.endsWith

private val log = KotlinLogging.logger { }

typealias RequestHandler = (OAuth2HttpRequest) -> OAuth2HttpResponse

interface Route : RequestHandler {
    fun match(request: OAuth2HttpRequest): Boolean
}

interface PathRoute: Route {

    fun matchPath(request: OAuth2HttpRequest): Boolean

    override fun match(request: OAuth2HttpRequest): Boolean {
        return matchPath(request)
    }

    class Builder {
        private val routes: MutableList<PathRoute> = mutableListOf()

        fun attach(vararg route: PathRoute) = apply {
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
            routes.add(PathMethodRoute(path, method, requestHandler))
        }

        fun build(): PathRoute = object : PathRoute {

            override fun matchPath(request: OAuth2HttpRequest): Boolean =
                routes.any { it.matchPath(request) }

            override fun match(request: OAuth2HttpRequest): Boolean {
                val match = routes.firstOrNull { it.match(request) } != null
                return match.also {
                    log.debug("route match invoked for request: ${request.url.encodedPath}, match=$it")
                    log.debug("searched through ${routes.size} routes")
                }
            }

            override fun invoke(request: OAuth2HttpRequest): OAuth2HttpResponse {
                log.debug("attempt to route request with url=${request.url}")
                return routes.firstOrNull { it.match(request) }?.invoke(request) ?: noMatch(request)
            }

            private fun noMatch(request: OAuth2HttpRequest): OAuth2HttpResponse {
                log.debug("number of routes when in nomatch: ${routes.size}")
                log.debug("all routes: $routes")
                return if (matchPath(request)){
                    OAuth2HttpResponse(status = 405, body = "method not allowed")
                } else {
                    OAuth2HttpResponse(status = 404, body = "no route found")
                }
            }

            override fun toString(): String {
                return routes.toString()
            }
        }
    }
}

fun rou(config: PathRoute.Builder.() -> Unit): PathRoute = PathRoute.Builder().apply(config).build()

class PathMethodRoute(
    private val path: String,
    private val method: String?,
    private val requestHandler: RequestHandler
): PathRoute {
    override fun matchPath(request: OAuth2HttpRequest) = request.url.endsWith(path)
    private fun matchMethod(request: OAuth2HttpRequest) = method?.let { it == request.method } ?: true

    override fun match(request: OAuth2HttpRequest): Boolean = matchPath(request) && matchMethod(request)
    override fun invoke(request: OAuth2HttpRequest): OAuth2HttpResponse = requestHandler.invoke(request)
    override fun toString(): String = "path=$path, method=$method"
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
