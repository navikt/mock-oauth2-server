package no.nav.security.mock.oauth2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// Runs on the kotlin-stdlib version declared for consumers. Every endpoint here serializes JSON,
// where a stdlib too old for our Kotlin-compiled dependencies fails.
// A missing stdlib class kills the handler thread, not this one, so the calls need timeouts.
class MinimumStdlibSmokeTest {
    @Test
    @Timeout(30)
    fun `server serves metadata and issues a token on the declared kotlin-stdlib`() {
        val server = MockOAuth2Server()
        server.start()
        try {
            val client = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build()

            val wellKnown = client.get(server.wellKnownUrl("default").toString())
            assertEquals(200, wellKnown.statusCode())
            assertTrue(wellKnown.body().contains("token_endpoint"))

            val jwks = client.get(server.jwksUrl("default").toString())
            assertEquals(200, jwks.statusCode())
            assertTrue(jwks.body().contains("keys"))

            val token =
                client.post(
                    server.tokenEndpointUrl("default").toString(),
                    "grant_type=client_credentials&client_id=client&client_secret=secret&scope=scope",
                )
            assertEquals(200, token.statusCode())
            assertTrue(token.body().contains("access_token"))
        } finally {
            server.shutdown()
        }
    }

    private fun HttpClient.get(url: String): HttpResponse<String> =
        send(
            HttpRequest
                .newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    private fun HttpClient.post(
        url: String,
        form: String,
    ): HttpResponse<String> =
        send(
            HttpRequest
                .newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    private companion object {
        val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
