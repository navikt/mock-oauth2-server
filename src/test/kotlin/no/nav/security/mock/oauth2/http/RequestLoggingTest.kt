package no.nav.security.mock.oauth2.http

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.read.ListAppender
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.TokenRequest
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class RequestLoggingTest {
    @Test
    fun `client errors are logged at info with the request url and error description but without a stack trace`() {
        val events =
            captureLogs(Level.INFO) {
                tokenRequest(OAuth2HttpRequestHandler(OAuth2Config()), "grant_type=unknown&client_id=client1").status shouldBe 400
            }

        val errorEvents = events.filter { it.formattedMessage.contains("failed with") }
        errorEvents shouldHaveSize 1
        errorEvents.single().level shouldBe Level.INFO
        errorEvents.single().formattedMessage shouldBe
            "POST http://localhost/default/token?trace=1 failed with unsupported_grant_type: grant_type unknown not supported."
        errorEvents.single().throwableProxy.shouldBeNull()
        events.filter { it.level == Level.ERROR }.shouldBeEmpty()
    }

    @Test
    fun `client errors include the stack trace when debug logging is enabled`() {
        val events =
            captureLogs(Level.DEBUG) {
                tokenRequest(OAuth2HttpRequestHandler(OAuth2Config()), "grant_type=unknown&client_id=client1").status shouldBe 400
            }

        val errorEvents = events.filter { it.formattedMessage.contains("failed with") }
        errorEvents shouldHaveSize 1
        errorEvents.single().level shouldBe Level.INFO
        ThrowableProxyUtil.asString(errorEvents.single().throwableProxy.shouldNotBeNull()) shouldContain "at no.nav.security.mock.oauth2"
    }

    @Test
    fun `client error logs stay on one line when the exception message contains line breaks`() {
        val events =
            captureLogs {
                val handler = OAuth2HttpRequestHandler(OAuth2Config())
                handler.enqueueTokenCallback(
                    failingCallback(OAuth2Exception(OAuth2Error.INVALID_REQUEST, "first line\r\nsecond line")),
                )
                tokenRequest(handler, "grant_type=client_credentials&client_id=client1&client_secret=secret").status shouldBe 400
            }

        val message = events.single { it.formattedMessage.contains("failed with") }.formattedMessage
        message shouldNotContain "\n"
        message shouldNotContain "\r"
        message shouldBe "POST http://localhost/default/token?trace=1 failed with invalid_request: Invalid request (first line\\r\\nsecond line)"
    }

    @Test
    fun `client error logs include the exception message when it differs from the description`() {
        val events =
            captureLogs {
                tokenRequest(OAuth2HttpRequestHandler(OAuth2Config()), "grant_type=urn:ietf:params:oauth:grant-type:token-exchange").status shouldBe 400
            }

        events.single { it.formattedMessage.contains("failed with") }.formattedMessage shouldBe
            "POST http://localhost/default/token?trace=1 failed with invalid_request: Invalid request (request must contain some form of ClientAuthentication.)"
    }

    @Test
    fun `server errors are logged at error with the request url and stack trace`() {
        val events =
            captureLogs {
                val handler = OAuth2HttpRequestHandler(OAuth2Config())
                handler.enqueueTokenCallback(failingCallback(IllegalStateException("callback-failure")))
                tokenRequest(handler, "grant_type=client_credentials&client_id=client1&client_secret=secret").status shouldBe 500
            }

        val error = events.single { it.level == Level.ERROR }
        error.formattedMessage shouldBe "received exception when handling POST http://localhost/default/token?trace=1"
        val stackTrace = ThrowableProxyUtil.asString(error.throwableProxy.shouldNotBeNull())
        stackTrace shouldContain "java.lang.IllegalStateException: callback-failure"
        stackTrace shouldContain "at no.nav.security.mock.oauth2"
    }

    @Test
    fun `server errors without a message are logged and return server_error`() {
        val events =
            captureLogs {
                val handler = OAuth2HttpRequestHandler(OAuth2Config())
                handler.enqueueTokenCallback(failingCallback(IllegalStateException()))
                val response = tokenRequest(handler, "grant_type=client_credentials&client_id=client1&client_secret=secret")
                response.status shouldBe 500
                response.body.shouldNotBeNull() shouldContain "server_error"
            }

        ThrowableProxyUtil.asString(events.single { it.level == Level.ERROR }.throwableProxy.shouldNotBeNull()) shouldContain
            "java.lang.IllegalStateException"
    }

    @Test
    fun `debug logging includes the request values`() {
        val events =
            captureLogs {
                tokenRequest(OAuth2HttpRequestHandler(OAuth2Config()), "grant_type=client_credentials&client_id=client1&client_secret=secret").status shouldBe
                    200
            }

        val tokenRequestLog = events.single { it.level == Level.DEBUG && it.formattedMessage.startsWith("handle token request") }.formattedMessage
        tokenRequestLog shouldContain "http://localhost/default/token?trace=1"
        tokenRequestLog shouldContain "grant_type=client_credentials&client_id=client1"
    }

    private fun tokenRequest(
        handler: OAuth2HttpRequestHandler,
        body: String,
    ): OAuth2HttpResponse =
        handler.authorizationServer.invoke(
            OAuth2HttpRequest(
                headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
                method = "POST",
                originalUrl = "http://localhost/default/token?trace=1".toHttpUrl(),
                body = body,
            ),
        )

    private fun failingCallback(exception: Exception): OAuth2TokenCallback =
        object : OAuth2TokenCallback by DefaultOAuth2TokenCallback() {
            override fun subject(tokenRequest: TokenRequest): String = throw exception
        }

    private fun captureLogs(
        level: Level = Level.DEBUG,
        block: () -> Unit,
    ): List<ILoggingEvent> {
        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val originalLevel = rootLogger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        rootLogger.addAppender(appender)
        rootLogger.level = level

        try {
            block()
            return appender.list.toList()
        } finally {
            rootLogger.detachAppender(appender)
            rootLogger.level = originalLevel
            appender.stop()
        }
    }
}
