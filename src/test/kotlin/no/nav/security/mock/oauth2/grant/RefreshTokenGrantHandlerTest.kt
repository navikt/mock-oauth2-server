package no.nav.security.mock.oauth2.grant

import com.nimbusds.oauth2.sdk.OAuth2Error
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap

internal class RefreshTokenGrantHandlerTest {
    @Test
    fun `refresh request that loses a concurrent rotation should not consume the enqueued callback`() {
        val refreshTokenManager = RefreshTokenManager(RotatedByConcurrentRequestCache())
        val refreshToken = refreshTokenManager.refreshToken(DefaultOAuth2TokenCallback(issuerId = "default"))
        val enqueuedCallbacks = ArrayDeque<OAuth2TokenCallback>(listOf(DefaultOAuth2TokenCallback(issuerId = "default", subject = "enqueued")))
        val handler =
            RefreshTokenGrantHandler(OAuth2TokenProvider(), refreshTokenManager, rotateRefreshToken = true) { enqueuedCallbacks.removeFirstOrNull() }

        shouldThrow<OAuth2Exception> {
            handler.tokenResponse(refreshTokenRequest(refreshToken), "http://localhost/default".toHttpUrl(), DefaultOAuth2TokenCallback())
        }.errorObject?.code shouldBe OAuth2Error.INVALID_GRANT.code

        enqueuedCallbacks.size shouldBe 1
    }

    private class RotatedByConcurrentRequestCache : ConcurrentHashMap<RefreshToken, RefreshTokenState>() {
        override fun remove(key: RefreshToken): RefreshTokenState? = null
    }

    private fun refreshTokenRequest(refreshToken: String): OAuth2HttpRequest =
        OAuth2HttpRequest(
            headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
            method = "POST",
            originalUrl = "http://localhost/default/token".toHttpUrl(),
            body =
                "grant_type=refresh_token&" +
                    "client_id=client1&" +
                    "client_secret=secret&" +
                    "refresh_token=$refreshToken",
        )
}
