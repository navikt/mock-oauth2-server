package no.nav.security.mock.oauth2.http

import io.kotest.matchers.shouldBe
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

internal class OAuth2HttpRequestTest {
    @Test
    fun `proxy aware urls all usecases`() {
        // no hostheader
        "http://localhost:8080/mypath?query=1".get().url shouldBe "http://localhost:8080/mypath?query=1".toHttpUrl()

        // no host header, x-forwarded-proto set
        "http://localhost:8080/mypath?query=1"
            .get(
                "x-forwarded-proto",
                "https",
            ).url shouldBe "https://localhost/mypath?query=1".toHttpUrl()

        // host header overrides host and port in url
        "http://localhost:8080/mypath?query=1"
            .get(
                "host",
                "localhost:8080",
            ).url shouldBe "http://localhost:8080/mypath?query=1".toHttpUrl()

        // host header overrides host in url, port from original url should be used
        "http://localhost:8080/mypath?query=1"
            .get(
                "host",
                "hostonly",
            ).url shouldBe "http://hostonly:8080/mypath?query=1".toHttpUrl()

        // host header overrides host in url, port from original url should be used
        "http://localhost:8080/mypath?query=1"
            .get(
                "host",
                "hostonly:-1",
            ).url shouldBe "http://hostonly:8080/mypath?query=1".toHttpUrl()

        // host header present, x-forwarded-port overrides port in url
        "http://localhost:8080/mypath?query=1"
            .get(
                "host",
                "host:8080",
                "x-forwarded-port",
                "9090",
            ).url shouldBe "http://host:9090/mypath?query=1".toHttpUrl()

        // host header and x-forwarded- headers present
        "http://localhost:8080/mypath?query=1"
            .get(
                "host",
                "hostheader:8080",
                "x-forwarded-port",
                "9090",
                "x-forwarded-proto",
                "https",
            ).url shouldBe "https://hostheader:9090/mypath?query=1".toHttpUrl()
    }

    private fun String.get(vararg headers: String) =
        OAuth2HttpRequest(
            originalUrl = this.toHttpUrl(),
            headers = Headers.headersOf(*headers),
            method = "GET",
        )

    @Test
    fun `proxyAwareUrl should use host header and x-forwarded-for- `() {
        val req1 =
            OAuth2HttpRequest(
                headers = Headers.headersOf(),
                method = "GET",
                originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl(),
            )
        req1.proxyAwareUrl().toString() shouldBe "http://localhost:8080/mypath?query=1"
        val req2 =
            OAuth2HttpRequest(
                headers =
                    Headers.headersOf(
                        "host",
                        "fakedings.nais.io",
                        "x-forwarded-proto",
                        "https",
                        "x-forwarded-port",
                        "444",
                    ),
                method = "GET",
                originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl(),
            )
        req2.proxyAwareUrl().toString() shouldBe "https://fakedings.nais.io:444/mypath?query=1"

        // host header has host:port and x-forwarded-port is set
        val req3 =
            OAuth2HttpRequest(
                headers =
                    Headers.headersOf(
                        "host",
                        "fakedings.nais.io:666",
                        "x-forwarded-proto",
                        "https",
                        "x-forwarded-port",
                        "444",
                    ),
                method = "GET",
                originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl(),
            )
        req3.proxyAwareUrl().toString() shouldBe "https://fakedings.nais.io:444/mypath?query=1"

        // host header has host:port and no x-forwarded-port
        val req4 =
            OAuth2HttpRequest(
                headers =
                    Headers.headersOf(
                        "host",
                        "fakedings.nais.io:666",
                        "x-forwarded-proto",
                        "https",
                    ),
                method = "GET",
                originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl(),
            )
        req4.proxyAwareUrl().toString() shouldBe "https://fakedings.nais.io:666/mypath?query=1"

        // Host header has only host and no x-forwarded-port
        val req5 =
            OAuth2HttpRequest(
                headers =
                    Headers.headersOf(
                        "host",
                        "fakedings.nais.io",
                        "x-forwarded-proto",
                        "https",
                    ),
                method = "GET",
                originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl(),
            )
        req5.proxyAwareUrl().toString() shouldBe "https://fakedings.nais.io/mypath?query=1"

        val req6 =
            OAuth2HttpRequest(
                headers =
                    Headers.headersOf(
                        "host",
                        "oauth2",
                    ),
                method = "GET",
                originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl(),
            )
        req6.proxyAwareUrl().toString() shouldBe "http://oauth2:8080/mypath?query=1"

        val req7 =
            OAuth2HttpRequest(
                headers =
                    Headers.headersOf(
                        "host",
                        "oauth2:8080",
                    ),
                method = "GET",
                originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl(),
            )
        req7.proxyAwareUrl().toString() shouldBe "http://oauth2:8080/mypath?query=1"

        val req8 =
            OAuth2HttpRequest(
                headers =
                    Headers.headersOf(
                        "host",
                        "oauth2",
                    ),
                method = "GET",
                originalUrl = "https://somehost/mypath?query=1".toHttpUrl(),
            )
        req8.proxyAwareUrl().toString() shouldBe "https://oauth2/mypath?query=1"

        // IPv6 host header with port
        val req9 =
            OAuth2HttpRequest(
                headers =
                    Headers.headersOf(
                        "host",
                        "[::1]:8080",
                    ),
                method = "GET",
                originalUrl = "http://[::1]:8080/mypath?query=1".toHttpUrl(),
            )
        req9.proxyAwareUrl().toString() shouldBe "http://[::1]:8080/mypath?query=1"

        // IPv6 host header without port
        val req10 =
            OAuth2HttpRequest(
                headers =
                    Headers.headersOf(
                        "host",
                        "[::1]",
                        "x-forwarded-proto",
                        "https",
                    ),
                method = "GET",
                originalUrl = "http://[::1]:8080/mypath?query=1".toHttpUrl(),
            )
        req10.proxyAwareUrl().toString() shouldBe "https://[::1]/mypath?query=1"

        // malformed IPv6 host header must not throw - fall back to the original url (issue #1022)
        val req11 =
            OAuth2HttpRequest(
                headers =
                    Headers.headersOf(
                        "host",
                        "[::1",
                    ),
                method = "GET",
                originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl(),
            )
        req11.proxyAwareUrl().toString() shouldBe "http://localhost:8080/mypath?query=1"

        // IPv6 host header with an out-of-range port must not throw either (issue #1022)
        val req12 =
            OAuth2HttpRequest(
                headers =
                    Headers.headersOf(
                        "host",
                        "[::1]:99999",
                    ),
                method = "GET",
                originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl(),
            )
        req12.proxyAwareUrl().toString() shouldBe "http://[::1]:8080/mypath?query=1"
    }

    @Test
    fun `hostAndPortFromHostHeader splits host and port for all Host header shapes`() {
        hostAndPortFromHostHeader(null) shouldBe null
        hostAndPortFromHostHeader("") shouldBe null
        hostAndPortFromHostHeader("   ") shouldBe null

        hostAndPortFromHostHeader("localhost") shouldBe ("localhost" to -1)
        hostAndPortFromHostHeader("fakedings.nais.io:666") shouldBe ("fakedings.nais.io" to 666)

        hostAndPortFromHostHeader("[::1]") shouldBe ("[::1]" to -1)
        hostAndPortFromHostHeader("[::1]:8080") shouldBe ("[::1]" to 8080)
        hostAndPortFromHostHeader("[2001:db8::1]:443") shouldBe ("[2001:db8::1]" to 443)

        // unparseable input must not throw
        hostAndPortFromHostHeader("[::1") shouldBe null
        hostAndPortFromHostHeader("[::1]:notaport") shouldBe null
        hostAndPortFromHostHeader("oauth2:notaport") shouldBe ("oauth2" to -1)

        // out-of-range ports collapse to the "no port" sentinel so HttpUrl.Builder never rejects them
        hostAndPortFromHostHeader("[::1]:99999") shouldBe ("[::1]" to -1)
        hostAndPortFromHostHeader("[::1]:0") shouldBe ("[::1]" to -1)
        hostAndPortFromHostHeader("oauth2:99999") shouldBe ("oauth2" to -1)

        // registry-style host java.net.URI refuses still works via the colon split
        hostAndPortFromHostHeader("mock_oauth2_server:8080") shouldBe ("mock_oauth2_server" to 8080)
    }

    @Test
    fun `wellKnown should use proxyAwareUrl when headers are set`() {
        val req1 =
            OAuth2HttpRequest(
                headers = Headers.headersOf(),
                method = "GET",
                originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl(),
            )
        req1.toWellKnown().issuer shouldBe "http://localhost:8080/mypath"
        val req2 =
            OAuth2HttpRequest(
                headers =
                    Headers.headersOf(
                        "host",
                        "fakedings.nais.io",
                        "x-forwarded-proto",
                        "https",
                        "x-forwarded-port",
                        "444",
                    ),
                method = "GET",
                originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl(),
            )
        req2.toWellKnown().issuer shouldBe "https://fakedings.nais.io:444/mypath"
    }

    @Test
    fun `wellKnown should contain urls exposed by mock service`() {
        val req1 =
            OAuth2HttpRequest(
                headers = Headers.headersOf(),
                method = "GET",
                originalUrl = "http://localhost:8080/mypath?query=1".toHttpUrl(),
            )

        req1.toWellKnown().issuer shouldBe "http://localhost:8080/mypath"
        req1.toWellKnown().userInfoEndpoint shouldBe "http://localhost:8080/mypath/userinfo"
        req1.toWellKnown().authorizationEndpoint shouldBe "http://localhost:8080/mypath/authorize"
        req1.toWellKnown().endSessionEndpoint shouldBe "http://localhost:8080/mypath/endsession"
        req1.toWellKnown().tokenEndpoint shouldBe "http://localhost:8080/mypath/token"
        req1.toWellKnown().jwksUri shouldBe "http://localhost:8080/mypath/jwks"
    }
}
