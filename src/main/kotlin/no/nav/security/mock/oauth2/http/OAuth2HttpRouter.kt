package no.nav.security.mock.oauth2.http

import mu.KotlinLogging
import no.nav.security.mock.oauth2.extensions.endsWith

private val log = KotlinLogging.logger { }

typealias RequestHandler = (OAuth2HttpRequest) -> OAuth2HttpResponse
internal typealias ExceptionHandler = (OAuth2HttpRequest, Throwable) -> OAuth2HttpResponse

interface Route : RequestHandler {
    fun match(request: OAuth2HttpRequest): Boolean

    class Builder {
        private val routes: MutableList<Route> = mutableListOf()

        private var exceptionHandler: ExceptionHandler = { _, throwable ->
            throw throwable
        }

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

        fun options(requestHandler: RequestHandler) = apply {
            addRoute("", "OPTIONS", requestHandler)
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

        fun exceptionHandler(exceptionHandler: ExceptionHandler) = apply {
            this.exceptionHandler = exceptionHandler
        }

        private fun addRoute(path: String, method: String? = null, requestHandler: RequestHandler) {
            routes.add(routeFromPathAndMethod(path, method, requestHandler))
        }

        fun build(): Route = object : PathRoute {
            override fun matchPath(request: OAuth2HttpRequest): Boolean =
                routes.any { it.matchPath(request) }

            override fun match(request: OAuth2HttpRequest): Boolean =
                routes.firstOrNull { it.match(request) } != null

            override fun invoke(request: OAuth2HttpRequest): OAuth2HttpResponse =
                try {
                    routes.firstOrNull { it.match(request) }?.invoke(request) ?: noMatch(request)
                } catch (t: Throwable) {
                    exceptionHandler(request, t)
                }

            override fun toString(): String = routes.toString()

            private fun noMatch(request: OAuth2HttpRequest): OAuth2HttpResponse {
                log.debug("no route matching url=${request.url} with method=${request.method}")
                return if (matchPath(request)) {
                    methodNotAllowed()
                } else {
                    notFound("no routes found")
                }
            }

            private fun Route.matchPath(request: OAuth2HttpRequest): Boolean = (this as? PathRoute)?.matchPath(request) ?: false
        }
    }
}

internal interface PathRoute : Route {
    fun matchPath(request: OAuth2HttpRequest): Boolean
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

fun options(path: String, requestHandler: RequestHandler): Route =
    routeFromPathAndMethod(path, "OPTIONS", requestHandler)

private fun routeFromPathAndMethod(path: String, method: String? = null, requestHandler: RequestHandler): Route =
    object : PathRoute {
        override fun matchPath(request: OAuth2HttpRequest): Boolean = request.url.endsWith(path)

        override fun match(request: OAuth2HttpRequest): Boolean = matchPath(request) && matchMethod(request)

        override fun invoke(request: OAuth2HttpRequest): OAuth2HttpResponse = requestHandler.invoke(request)

        override fun toString(): String = "[path=$path, method=$method]"

        private fun matchMethod(request: OAuth2HttpRequest): Boolean = method?.let { it == request.method } ?: true
    }
