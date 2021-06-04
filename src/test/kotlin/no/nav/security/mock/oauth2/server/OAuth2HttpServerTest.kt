package no.nav.security.mock.oauth2.server

import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import no.nav.security.mock.oauth2.http.MockWebServerWrapper
import no.nav.security.mock.oauth2.http.NettyWrapper
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.http.OAuth2HttpServer
import no.nav.security.mock.oauth2.http.RequestHandler
import no.nav.security.mock.oauth2.http.Ssl
import no.nav.security.mock.oauth2.http.SslKeystore
import no.nav.security.mock.oauth2.http.redirect
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.testutils.post
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import java.io.File
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private val log = KotlinLogging.logger { }

internal class OAuth2HttpServerTest {

    val httpClient = OkHttpClient().newBuilder().followRedirects(false).build()

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
    fun `Netty server should start and serve requests with generated keystore and HTTPS enabled`() {
        val ssl = Ssl()
        NettyWrapper(ssl).start(requestHandler).shouldServeRequests(ssl).stop()
        NettyWrapper(ssl).start(port = 1234, requestHandler).shouldServeRequests(ssl).stop()
    }

    @Test
    fun `Netty server should start and serve requests with provided keystore and HTTPS enabled`() {
        val ssl = Ssl(
            SslKeystore(
                keyPassword = "",
                keystoreFile = File("src/test/resources/localhost.p12"),
                keystorePassword = "",
                keystoreType = SslKeystore.KeyStoreType.PKCS12
            )
        )
        NettyWrapper(ssl).start(requestHandler).shouldServeRequests(ssl).stop()
    }

    @Test
    fun `MockWebServer should start and serve requests`() {
        MockWebServerWrapper().start(requestHandler).shouldServeRequests().stop()
        MockWebServerWrapper().start(port = 1234, requestHandler).shouldServeRequests().stop()
    }

    @Test
    fun `MockWebServer should start and serve requests with generated keystore and HTTPS enabled`() {
        val ssl = Ssl()
        MockWebServerWrapper(ssl).start(requestHandler).shouldServeRequests(ssl).stop()
        MockWebServerWrapper(ssl).start(port = 1234, requestHandler).shouldServeRequests(ssl).stop()
    }

    private fun OAuth2HttpServer.shouldServeRequests(ssl: Ssl? = null) = apply {
        val client = if (ssl != null) {
            httpClient.withTrustStore(ssl.sslKeystore.keyStore)
        } else {
            httpClient
        }

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

    private fun OkHttpClient.withTrustStore(keyStore: KeyStore): OkHttpClient =
        newBuilder().apply {
            followRedirects(false)
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(keyStore) }
            val sslContext = SSLContext.getInstance("TLS").apply { init(null, trustManagerFactory.trustManagers, null) }
            sslSocketFactory(sslContext.socketFactory, trustManagerFactory.trustManagers[0] as X509TrustManager)
        }.build()

    private fun ok(body: String) = OAuth2HttpResponse(
        status = 200,
        body = body
    )
}
