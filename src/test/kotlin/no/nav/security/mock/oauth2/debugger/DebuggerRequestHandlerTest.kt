package no.nav.security.mock.oauth2.debugger

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.http.NettyWrapper
import no.nav.security.mock.oauth2.http.Ssl
import no.nav.security.mock.oauth2.http.SslKeystore
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import java.io.File
import java.net.InetAddress

internal class DebuggerRequestHandlerTest {
    private val client = OkHttpClient().newBuilder().followRedirects(false).build()

    @Test
    fun `debugger callback should not make server side requests to a client supplied token_url`() {
        val internalService = MockWebServer()
        internalService.enqueue(MockResponse().setBody("INTERNAL_SECRET"))
        internalService.start()

        val server = MockOAuth2Server()
        server.start()

        try {
            val ssrfTarget = internalService.url("/secret-internal-endpoint")
            val debuggerUrl = server.url("/default/debugger")

            val form =
                FormBody
                    .Builder()
                    .add("authorize_url", server.url("/default/authorize").toString())
                    .add("token_url", ssrfTarget.toString())
                    .add("client_id", "debugger")
                    .add("client_secret", "secret")
                    .add("client_auth_method", "CLIENT_SECRET_BASIC")
                    .add("scope", "openid")
                    .add("redirect_uri", server.url("/default/debugger/callback").toString())
                    .build()

            val cookie =
                client
                    .newCall(
                        Request
                            .Builder()
                            .url(debuggerUrl)
                            // spoofed to match the SSRF target, defeating any same origin check on the request url
                            .header("Host", "${ssrfTarget.host}:${ssrfTarget.port}")
                            .post(form)
                            .build(),
                    ).execute()
                    .use { checkNotNull(it.header("Set-Cookie")) }

            val callbackBody =
                client
                    .newCall(
                        Request
                            .Builder()
                            .url(server.url("/default/debugger/callback?code=some-code"))
                            .header("Host", "${ssrfTarget.host}:${ssrfTarget.port}")
                            .header("Cookie", cookie.substringBefore(";"))
                            .build(),
                    ).execute()
                    .use { it.body.string() }

            internalService.requestCount shouldBe 0
            callbackBody shouldNotContain "INTERNAL_SECRET"
            // the client visible url is still reflected in the request, it just no longer selects the target
            callbackBody shouldContain "Host: ${ssrfTarget.host}:${ssrfTarget.port}"
        } finally {
            server.shutdown()
            internalService.shutdown()
        }
    }

    @Test
    fun `debugger should not redirect to a client supplied authorize_url`() {
        val server = MockOAuth2Server()
        server.start()

        try {
            val form =
                FormBody
                    .Builder()
                    .add("authorize_url", "http://evil.example.com/authorize")
                    .add("client_id", "debugger")
                    .add("scope", "openid")
                    .build()

            client
                .newCall(
                    Request
                        .Builder()
                        .url(server.url("/default/debugger"))
                        .post(form)
                        .build(),
                ).execute()
                .use {
                    it.code shouldBe 302
                    checkNotNull(it.header("Location")) shouldStartWith server.url("/default/authorize").toString()
                }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `debugger callback should reach the token endpoint over https with a provided keystore`() {
        val ssl =
            Ssl(
                SslKeystore(
                    keyPassword = "",
                    keystoreFile = File("src/test/resources/localhost.p12"),
                    keystorePassword = "",
                    keystoreType = SslKeystore.KeyStoreType.PKCS12,
                ),
            )
        val server = MockOAuth2Server(OAuth2Config(httpServer = NettyWrapper(ssl)))
        // the wildcard bind of the standalone server, an address no certificate covers
        server.start(InetAddress.getByName("0.0.0.0"), 0)
        val sslClient = client.withSsl(ssl)

        try {
            val base = "https://localhost:${server.url("/").port}"
            val form =
                FormBody
                    .Builder()
                    .add("client_id", "debugger")
                    .add("client_secret", "secret")
                    .add("client_auth_method", "CLIENT_SECRET_BASIC")
                    .add("scope", "openid")
                    .add("redirect_uri", "$base/default/debugger/callback")
                    .build()

            val cookie =
                sslClient
                    .newCall(
                        Request
                            .Builder()
                            .url("$base/default/debugger")
                            .post(form)
                            .build(),
                    ).execute()
                    .use { checkNotNull(it.header("Set-Cookie")) }

            sslClient
                .newCall(
                    Request
                        .Builder()
                        .url("$base/default/debugger/callback?code=some-code")
                        .header("Cookie", cookie.substringBefore(";"))
                        .build(),
                ).execute()
                .use { it.body.string() } shouldContain "invalid_grant"
        } finally {
            server.shutdown()
        }
    }
}
