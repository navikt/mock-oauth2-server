package no.nav.security.mock.oauth2.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
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
import no.nav.security.mock.oauth2.testutils.withTrustStore
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.Test
import java.io.File
import java.net.InetAddress
import java.net.Socket

private val log = KotlinLogging.logger { }

internal class OAuth2HttpServerTest {
    val httpClient = OkHttpClient().newBuilder().followRedirects(false).build()

    val requestHandler: RequestHandler = {
        log.debug { "received request on url=${it.url}" }
        when {
            it.method == "HEAD" -> {
                OAuth2HttpResponse(headers = Headers.headersOf("Content-Length", "9"), status = 200)
            }

            it.headers.contains("header1" to "headervalue1") -> {
                ok("headermatch")
            }

            it.url.pathSegments == listOf("1", "2") -> {
                ok("pathmatch")
            }

            it.url.query == "param1=value1&param2=value2" -> {
                ok("querymatch")
            }

            it.body == "formparam=formvalue1" -> {
                ok("bodymatch")
            }

            it.url.pathSegments.contains("redirect") -> {
                redirect("http://someredirect")
            }

            it.url.pathSegments == listOf("nocontent") -> {
                OAuth2HttpResponse(status = 204)
            }

            it.url.pathSegments == listOf("host") -> {
                ok("${it.originalUrl.host}:${it.originalUrl.port} ${it.url.host}")
            }

            else -> {
                OAuth2HttpResponse(status = 404)
            }
        }
    }

    @Test
    fun `Netty server should start and serve requests`() {
        NettyWrapper().start(requestHandler).shouldServeRequests().stop()
        NettyWrapper().start(port = 0, requestHandler).shouldServeRequests().stop()
    }

    @Test
    fun `Netty server url should use the host name it was started with`() {
        val loopbackWithCustomHostName = InetAddress.getByAddress("custom-host", byteArrayOf(127, 0, 0, 1))
        val server = NettyWrapper().start(loopbackWithCustomHostName, 0, requestHandler)
        try {
            server.url("/").host shouldBe "custom-host"
        } finally {
            server.stop()
        }
    }

    @Test
    fun `Netty server should start and serve requests with generated keystore and HTTPS enabled`() {
        val ssl = Ssl()
        NettyWrapper(ssl).start(requestHandler).shouldServeRequests(ssl).stop()
        NettyWrapper(ssl).start(port = 0, requestHandler).shouldServeRequests(ssl).stop()
    }

    @Test
    fun `Netty server should start and serve requests with provided keystore and HTTPS enabled`() {
        val ssl =
            Ssl(
                SslKeystore(
                    keyPassword = "",
                    keystoreFile = File("src/test/resources/localhost.p12"),
                    keystorePassword = "",
                    keystoreType = SslKeystore.KeyStoreType.PKCS12,
                ),
            )
        NettyWrapper(ssl).start(requestHandler).shouldServeRequests(ssl).stop()
    }

    @Test
    fun `Netty server should serve requests with an IPv6 Host header instead of dropping the connection`() {
        val server = NettyWrapper().start(requestHandler)
        try {
            listOf("[::1]:8080", "[::1]", "[::1", "[::1]:99999").forEach { hostHeader ->
                httpClient
                    .get(server.url("/1/2"), Headers.headersOf("Host", hostHeader))
                    .use {
                        it.code shouldBe 200
                        it.body.string() shouldBe "pathmatch"
                    }
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `Netty server should reject request bodies exceeding the max content length`() {
        val server = NettyWrapper().start(requestHandler)
        try {
            val oversizedBody = "a".repeat(1024 * 1024 + 1)
            httpClient
                .newCall(
                    Request
                        .Builder()
                        .url(server.url("/form"))
                        .post(oversizedBody.toRequestBody("text/plain".toMediaType()))
                        .build(),
                ).execute()
                .use { it.code shouldBe 413 }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `Netty server should keep the connection open and send Content-Length and a reason phrase`() {
        val server = NettyWrapper().start(requestHandler)
        try {
            server.socketWithReadTimeout().use { socket ->
                repeat(2) {
                    socket.sendAndReadResponse("GET /1/2 HTTP/1.1\r\nHost: localhost\r\n\r\n").asClue { (head, body) ->
                        head.first() shouldBe "HTTP/1.1 200 OK"
                        head.map { it.lowercase() } shouldContain "content-length: 9"
                        head.map { it.lowercase() } shouldNotContain "connection: close"
                        body shouldBe "pathmatch"
                    }
                }
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `Netty server should close the connection when the client asks for it`() {
        val server = NettyWrapper().start(requestHandler)
        try {
            server.socketWithReadTimeout().use { socket ->
                val (head, body) = socket.sendAndReadResponse("GET /1/2 HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n")
                head.map { it.lowercase() } shouldContain "connection: close"
                body shouldBe "pathmatch"
                socket.getInputStream().read() shouldBe -1
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `Netty server should send no Content-Length with a 204 and keep the connection open`() {
        val server = NettyWrapper().start(requestHandler)
        try {
            server.socketWithReadTimeout().use { socket ->
                val (head, _) = socket.sendAndReadResponse("GET /nocontent HTTP/1.1\r\nHost: localhost\r\n\r\n", expectBody = false)
                head.first() shouldBe "HTTP/1.1 204 No Content"
                head.none { it.lowercase().startsWith("content-length:") } shouldBe true
                socket.sendAndReadResponse("GET /1/2 HTTP/1.1\r\nHost: localhost\r\n\r\n").second shouldBe "pathmatch"
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `Netty server should take the request host from the Host header and the default port when it has none`() {
        val server = NettyWrapper().start(requestHandler)
        try {
            httpClient.get(server.url("/host"), Headers.headersOf("Host", "oauth.example.com")).use {
                it.body.string() shouldBe "oauth.example.com:80 oauth.example.com"
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `Netty server should use its own address, not the client's, when there is no Host header`() {
        val server = NettyWrapper().start(requestHandler)
        try {
            server.socketWithReadTimeout().use { socket ->
                val serverHost =
                    HttpUrl
                        .Builder()
                        .scheme("http")
                        .host(socket.inetAddress.hostAddress)
                        .build()
                        .host
                socket.sendAndReadResponse("GET /host HTTP/1.0\r\n\r\n").second shouldBe "$serverHost:${server.port()} $serverHost"
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `Netty server should serve request lines of at least 8000 octets`() {
        val server = NettyWrapper().start(requestHandler)
        try {
            httpClient.get(server.url("/1/2?state=${"a".repeat(12_000)}")).use {
                it.code shouldBe 200
                it.body.string() shouldBe "pathmatch"
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `Netty server should answer 414 and close the connection when the request line is too long`() {
        val server = NettyWrapper().start(requestHandler)
        try {
            server.socketWithReadTimeout().use { socket ->
                val (head, _) =
                    socket.sendAndReadResponse("GET /1/2?state=${"a".repeat(20_000)} HTTP/1.1\r\nHost: localhost\r\n\r\n")
                head.first() shouldBe "HTTP/1.1 414 Request-URI Too Long"
                head.map { it.lowercase() } shouldContain "connection: close"
                socket.getInputStream().read() shouldBe -1
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `MockWebServer should start and serve requests`() {
        MockWebServerWrapper().start(requestHandler).shouldServeRequests().stop()
        MockWebServerWrapper().start(port = 0, requestHandler).shouldServeRequests().stop()
    }

    @Test
    fun `MockWebServer should start and serve requests with generated keystore and HTTPS enabled`() {
        val ssl = Ssl()
        MockWebServerWrapper(ssl).start(requestHandler).shouldServeRequests(ssl).stop()
        MockWebServerWrapper(ssl).start(port = 0, requestHandler).shouldServeRequests(ssl).stop()
    }

    private fun OAuth2HttpServer.shouldServeRequests(ssl: Ssl? = null) =
        apply {
            val client =
                if (ssl != null) {
                    httpClient.withTrustStore(ssl.sslKeystore.keyStore)
                } else {
                    httpClient
                }

            client
                .get(
                    this.url("/header"),
                    Headers.headersOf("header1", "headervalue1"),
                ).body
                .string() shouldBe "headermatch"

            client.get(this.url("/1/2")).body.string() shouldBe "pathmatch"
            client.get(this.url("path?param1=value1&param2=value2")).body.string() shouldBe "querymatch"
            client.post(this.url("/form"), mapOf("formparam" to "formvalue1")).body.string() shouldBe "bodymatch"
            client.get(this.url("/notfound")).code shouldBe 404
            client.get(this.url("/redirect")).apply {
                this.code shouldBe 302
                this.headers["Location"] shouldBe "http://someredirect"
            }
            client
                .newCall(
                    Request
                        .Builder()
                        .url(this.url("/1/2"))
                        .head()
                        .build(),
                ).execute()
                .use {
                    it.code shouldBe 200
                    it.headers["Content-Length"] shouldBe "9"
                }
            client.get(this.url("/1/2")).body.string() shouldBe "pathmatch"
        }

    private fun OAuth2HttpServer.socketWithReadTimeout() = Socket(url("/").host, port()).apply { soTimeout = 5_000 }

    private fun Socket.sendAndReadResponse(
        request: String,
        expectBody: Boolean = true,
    ): Pair<List<String>, String> {
        getOutputStream().apply {
            write(request.toByteArray())
            flush()
        }
        val input = getInputStream()
        val head = StringBuilder()
        while (!head.endsWith("\r\n\r\n")) {
            val next = input.read()
            if (next == -1) break
            head.append(next.toChar())
        }
        val lines = head.trim().lines().map { it.trimEnd('\r') }
        val length =
            lines
                .firstOrNull { it.lowercase().startsWith("content-length:") }
                ?.substringAfter(':')
                ?.trim()
                ?.toInt()
        val body =
            when {
                length != null -> String(input.readNBytes(length))
                expectBody -> String(input.readAllBytes())
                else -> ""
            }
        return lines to body
    }

    private fun ok(body: String) =
        OAuth2HttpResponse(
            status = 200,
            body = body,
        )
}
