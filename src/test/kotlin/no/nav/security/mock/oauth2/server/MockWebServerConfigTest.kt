package no.nav.security.mock.oauth2.server

import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.http.OAuth2HttpRequestHandler
import no.nav.security.mock.oauth2.testutils.get
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

internal class MockWebServerConfigTest {

    private val client: OkHttpClient = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .build()

    @Test
    fun `yolo`() {
        val routes = routes(
            "/{issuerid:.*}/token" bind Method.GET to { req ->
                Response(Status.OK)
                    .header("respheader", "doh")
                    .header("respheader", "doh2")
                    .body("wip")
            },
            "/token/specific" bind Method.GET to { req ->
                Response(Status.OK)
                    .header("respheader", "doh")
                    .header("respheader", "doh2")
                    .body("spec")
            }
        )
        val testServer = MockWebServerConfig().toServer(routes)
        testServer.start()

        val resp = client.get("http://localhost:${testServer.port()}/yolo/ball/token".toHttpUrl())

        println(resp.body?.string())
        println(resp.headers)

    }
}
val defaultHandler: HttpHandler = {
    Response(Status.OK)
}
fun doh(vararg routingHttpHandler: RoutingHttpHandler):RoutingHttpHandler {

    val tmp = "" bind defaultHandler
    return routes(*routingHttpHandler,tmp)
}
