package no.nav.security.mock.oauth2.server

import mu.KotlinLogging
import no.nav.security.mock.oauth2.extensions.endsWith
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import okhttp3.HttpUrl

private val log = KotlinLogging.logger { }

typealias RequestHandler = (OAuth2HttpRequest) -> OAuth2HttpResponse

interface Route : RequestHandler {
    fun match(request: OAuth2HttpRequest): Boolean
}

class HttpRouter(
    private vararg val routes: Route
) : RequestHandler {

    var notFoundHandler: (OAuth2HttpRequest) -> OAuth2HttpResponse = { OAuth2HttpResponse(status = 404, body = "no route found") }

    private fun match(request: OAuth2HttpRequest): OAuth2HttpResponse =
        routes.also {
            log.debug("attempt to route request with url=${request.url}")
        }.firstOrNull {
            it.match(request)
        }?.invoke(request)
            ?: notFoundHandler.invoke(request)
                .also { log.debug("no handler found, using notFoundHandler") }

    override fun invoke(request: OAuth2HttpRequest): OAuth2HttpResponse = match(request)
}

@JvmOverloads
fun route(path: String, method: String? = null, requestHandler: RequestHandler): Route =
    routeFromPathAndMethod(path, method, requestHandler)

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
    }
