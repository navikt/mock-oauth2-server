package no.nav.security.mock.oauth2.http

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.security.mock.oauth2.OAuth2Config
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class SensitiveLoggingTest {
    @Test
    fun `request logging excludes headers body and query values`() {
        val messages =
            captureLogs {
                val route = OAuth2HttpRequestHandler(OAuth2Config()).authorizationServer
                val response =
                    route.invoke(
                        OAuth2HttpRequest(
                            headers =
                                Headers.headersOf(
                                    "Authorization",
                                    "Bearer header-sentinel",
                                    "X-Api-Key",
                                    "other-header-sentinel",
                                    "Content-Type",
                                    "application/x-www-form-urlencoded",
                                ),
                            method = "POST",
                            originalUrl = "http://localhost/default/token?trace=query-sentinel".toHttpUrl(),
                            body = "grant_type=client_credentials&client_id=client&client_secret=body-sentinel",
                        ),
                    )

                response.status shouldBe 200
            }

        messages shouldContain "handle token request"
        messages shouldNotContain "Authorization"
        messages shouldNotContain "X-Api-Key"
        messages shouldNotContain "header-sentinel"
        messages shouldNotContain "other-header-sentinel"
        messages shouldNotContain "body-sentinel"
        messages shouldNotContain "query-sentinel"
    }

    @Test
    fun `authorization and refresh token values are not logged`() {
        lateinit var issuedCode: String
        val messages =
            captureLogs {
                val route = OAuth2HttpRequestHandler(OAuth2Config(interactiveLogin = true)).authorizationServer
                val authResponse =
                    route.invoke(
                        OAuth2HttpRequest(
                            headers = Headers.headersOf(),
                            method = "POST",
                            originalUrl =
                                "http://localhost/default/authorize"
                                    .toHttpUrl()
                                    .newBuilder()
                                    .addQueryParameter("client_id", "client")
                                    .addQueryParameter("response_type", "code")
                                    .addQueryParameter("redirect_uri", "http://callback")
                                    .addQueryParameter("scope", "openid")
                                    .addQueryParameter("state", "query-state-sentinel")
                                    .build(),
                            body = "username=user-sentinel&claims=not-json-sentinel",
                        ),
                    )
                authResponse.status shouldBe 302
                val code = checkNotNull(authResponse.headers["Location"]).toHttpUrl().queryParameter("code")
                issuedCode = checkNotNull(code)

                route.invoke(
                    OAuth2HttpRequest(
                        headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
                        method = "POST",
                        originalUrl = "http://localhost/default/token".toHttpUrl(),
                        body =
                            "grant_type=authorization_code&code=$issuedCode&redirect_uri=http%3A%2F%2Fcallback" +
                                "&client_id=client&client_secret=client-secret-sentinel",
                    ),
                )
                route.invoke(
                    OAuth2HttpRequest(
                        headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
                        method = "POST",
                        originalUrl = "http://localhost/default/token".toHttpUrl(),
                        body = "grant_type=refresh_token&refresh_token=refresh-sentinel&client_id=client",
                    ),
                )
                route.invoke(
                    OAuth2HttpRequest(
                        headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
                        method = "POST",
                        originalUrl = "http://localhost/default/debugger".toHttpUrl(),
                        body = "client_id=client&client_secret=debugger-sentinel",
                    ),
                )
                route.invoke(
                    OAuth2HttpRequest(
                        headers = Headers.headersOf(),
                        method = "GET",
                        originalUrl = "http://localhost/no-route?token=unmatched-sentinel".toHttpUrl(),
                    ),
                )
            }

        messages shouldContain "issuing authorization code"
        messages shouldNotContain issuedCode
        messages shouldNotContain "user-sentinel"
        messages shouldNotContain "not-json-sentinel"
        messages shouldNotContain "query-state-sentinel"
        messages shouldNotContain "client-secret-sentinel"
        messages shouldNotContain "refresh-sentinel"
        messages shouldNotContain "debugger-sentinel"
        messages shouldNotContain "unmatched-sentinel"
    }

    private fun captureLogs(block: () -> Unit): String {
        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val originalLevel = rootLogger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        rootLogger.addAppender(appender)
        rootLogger.level = Level.DEBUG

        try {
            block()
            return appender.list.joinToString("\n") {
                it.formattedMessage +
                    (it.throwableProxy?.let { proxy -> ch.qos.logback.classic.spi.ThrowableProxyUtil.asString(proxy) } ?: "")
            }
        } finally {
            rootLogger.detachAppender(appender)
            rootLogger.level = originalLevel
            appender.stop()
        }
    }
}
