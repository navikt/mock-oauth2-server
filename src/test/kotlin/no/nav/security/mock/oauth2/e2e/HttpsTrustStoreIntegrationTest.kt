package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.http.MockWebServerWrapper
import no.nav.security.mock.oauth2.http.Ssl
import no.nav.security.mock.oauth2.http.SslKeystore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.io.File
import java.security.KeyStore
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class HttpsTrustStoreIntegrationTest {
    private val defaultSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory()

    @AfterEach
    fun restoreDefaultSocketFactory() {
        HttpsURLConnection.setDefaultSSLSocketFactory(defaultSocketFactory)
    }

    @Test
    fun `spring mvc jwt decoder should verify tokens over https once the generated keystore is trusted`() {
        val ssl = Ssl()
        withHttpsServer(ssl) { jwksUrl, token ->
            shouldThrow<JwtException> { NimbusJwtDecoder.withJwkSetUri(jwksUrl).build().decode(token) }
                .message shouldContain "PKIX path building failed"

            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(ssl.sslKeystore.keyStore)
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustManagerFactory.trustManagers, null)
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)

            NimbusJwtDecoder
                .withJwkSetUri(jwksUrl)
                .build()
                .decode(token)
                .subject shouldBe "foo"
        }
    }

    @Test
    fun `truststore made with keytool from the server keystore should trust the server`() {
        val ssl =
            Ssl(
                SslKeystore(
                    keyPassword = "",
                    keystoreFile = File("src/test/resources/localhost.p12"),
                    keystorePassword = "",
                    keystoreType = SslKeystore.KeyStoreType.PKCS12,
                ),
            )
        withHttpsServer(ssl) { jwksUrl, token ->
            val trustStore = KeyStore.getInstance("PKCS12")
            File("src/test/resources/truststore.p12").inputStream().use { trustStore.load(it, "changeit".toCharArray()) }
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(trustStore)
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustManagerFactory.trustManagers, null)
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)

            NimbusJwtDecoder
                .withJwkSetUri(jwksUrl)
                .build()
                .decode(token)
                .subject shouldBe "foo"
        }
    }

    private fun withHttpsServer(
        ssl: Ssl,
        test: (jwksUrl: String, token: String) -> Unit,
    ) {
        val server = MockOAuth2Server(OAuth2Config(httpServer = MockWebServerWrapper(ssl))).apply { start() }
        try {
            test(server.jwksUrl("default").toString(), server.issueToken("default", subject = "foo").serialize())
        } finally {
            server.shutdown()
        }
    }
}
