package no.nav.security.mock.oauth2.http

class CorsInterceptor(
    private val allowedMethods: List<String> = listOf("POST", "GET", "OPTIONS")
) : ResponseInterceptor {

    companion object HeaderNames {
        const val ORIGIN = "origin"
        const val ACCESS_CONTROL_ALLOW_CREDENTIALS = "access-control-allow-credentials"
        const val ACCESS_CONTROL_REQUEST_HEADERS = "access-control-request-headers"
        const val ACCESS_CONTROL_ALLOW_HEADERS = "access-control-allow-headers"
        const val ACCESS_CONTROL_ALLOW_METHODS = "access-control-allow-methods"
        const val ACCESS_CONTROL_ALLOW_ORIGIN = "access-control-allow-origin"
    }

    override fun intercept(request: OAuth2HttpRequest, response: OAuth2HttpResponse): OAuth2HttpResponse {
        val origin = request.headers[ORIGIN]
        return if (origin != null) {
            val headers = request.headers.newBuilder()
            if (request.method == "OPTIONS") {
                val reqHeader = headers[ACCESS_CONTROL_REQUEST_HEADERS]
                if (reqHeader != null) {
                    headers.add(ACCESS_CONTROL_ALLOW_HEADERS, reqHeader)
                }
                headers.add(ACCESS_CONTROL_ALLOW_METHODS, allowedMethods.joinToString(", "))
            }
            headers.add(ACCESS_CONTROL_ALLOW_ORIGIN, origin)
            headers.add(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")

            response.copy(headers = headers.build())
        } else {
            return response
        }
    }
}
