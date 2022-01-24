package no.nav.security.mock.oauth2.http

import io.kotest.matchers.shouldBe
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

internal class OAuth2HttpRequestTest {

    @Test
    fun `proxyAwareUrl should use host header and x-forwarded-for- `() {
        val req1 = OAuth2HttpRequest(
            headers = Headers.headersOf(),
            method = "GET",
            originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl()
        )
        req1.proxyAwareUrl().toString() shouldBe "http://localhost:8080/mypath?query=1"
        val req2 = OAuth2HttpRequest(
            headers = Headers.headersOf(
                "host",
                "fakedings.nais.io",
                "x-forwarded-proto",
                "https",
                "x-forwarded-port",
                "444"
            ),
            method = "GET",
            originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl()
        )
        req2.proxyAwareUrl().toString() shouldBe "https://fakedings.nais.io:444/mypath?query=1"

        // host header has host:port and x-forwarded-port is set
        val req3 = OAuth2HttpRequest(
            headers = Headers.headersOf(
                "host",
                "fakedings.nais.io:666",
                "x-forwarded-proto",
                "https",
                "x-forwarded-port",
                "444"
            ),
            method = "GET",
            originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl()
        )
        req3.proxyAwareUrl().toString() shouldBe "https://fakedings.nais.io:444/mypath?query=1"

        // host header has host:port and no x-forwarded-port
        val req4 = OAuth2HttpRequest(
            headers = Headers.headersOf(
                "host",
                "fakedings.nais.io:666",
                "x-forwarded-proto",
                "https"
            ),
            method = "GET",
            originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl()
        )
        req4.proxyAwareUrl().toString() shouldBe "https://fakedings.nais.io:666/mypath?query=1"
    }

    @Test
    fun `wellKnown should use proxyAwareUrl when headers are set`() {
        val req1 = OAuth2HttpRequest(
            headers = Headers.headersOf(),
            method = "GET",
            originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl()
        )
        req1.toWellKnown().issuer shouldBe "http://localhost:8080/mypath"
        val req2 = OAuth2HttpRequest(
            headers = Headers.headersOf(
                "host",
                "fakedings.nais.io",
                "x-forwarded-proto",
                "https",
                "x-forwarded-port",
                "444"
            ),
            method = "GET",
            originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl()
        )
        req2.toWellKnown().issuer shouldBe "https://fakedings.nais.io:444/mypath"
    }
}
