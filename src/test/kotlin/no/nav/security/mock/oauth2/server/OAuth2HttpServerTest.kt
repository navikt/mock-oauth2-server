package no.nav.security.mock.oauth2.server

import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import no.nav.security.mock.oauth2.http.MockWebServerWrapper
import no.nav.security.mock.oauth2.http.NettyWrapper
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.http.OAuth2HttpServer
import no.nav.security.mock.oauth2.http.RequestHandler
import no.nav.security.mock.oauth2.http.redirect
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.post
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

private val log = KotlinLogging.logger { }

internal class OAuth2HttpServerTest {

    val client: OkHttpClient = OkHttpClient().newBuilder()
        .followRedirects(false)
        .build()

    val requestHandler: RequestHandler = {
        log.debug("received request on url=${it.url}")
        when {
            it.headers.contains("header1" to "headervalue1") -> ok("headermatch")
            it.url.pathSegments == listOf("1", "2") -> ok("pathmatch")
            it.url.query == "param1=value1&param2=value2" -> ok("querymatch")
            it.body == "formparam=formvalue1" -> ok("bodymatch")
            it.url.pathSegments.contains("redirect") ->
                redirect("http://someredirect")
            else -> {
                OAuth2HttpResponse(status = 404)
            }
        }
    }

    @Test
    fun `Netty server should start and serve requests`() {
        NettyWrapper().start(requestHandler).shouldServeRequests().stop()
        NettyWrapper().start(port = 1234, requestHandler).shouldServeRequests().stop()
    }

    @Test
    fun `MockWebServer should start and serve requests`() {
        MockWebServerWrapper().start(requestHandler).shouldServeRequests().stop()
        MockWebServerWrapper().start(port = 1234, requestHandler).shouldServeRequests().stop()
    }

    private fun OAuth2HttpServer.shouldServeRequests() = apply {
        client.get(
            this.url("/header"),
            Headers.headersOf("header1", "headervalue1")
        ).body?.string() shouldBe "headermatch"

        client.get(this.url("/1/2")).body?.string() shouldBe "pathmatch"
        client.get(this.url("path?param1=value1&param2=value2")).body?.string() shouldBe "querymatch"
        client.post(this.url("/form"), mapOf("formparam" to "formvalue1")).body?.string() shouldBe "bodymatch"
        client.get(this.url("/notfound")).code shouldBe 404
        client.get(this.url("/redirect")).apply {
            this.code shouldBe 302
            this.headers["Location"] shouldBe "http://someredirect"
        }
    }

    private fun ok(body: String) = OAuth2HttpResponse(
        status = 200,
        body = body
    )
}
