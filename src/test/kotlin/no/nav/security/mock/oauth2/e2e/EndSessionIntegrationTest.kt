package no.nav.security.mock.oauth2.e2e

import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import no.nav.security.mock.oauth2.withMockOAuth2Server
import okhttp3.HttpUrl
import org.junit.jupiter.api.Test

class EndSessionIntegrationTest {
    private val client = client()

    @Test
    fun `end session request should redirect to post_logout_redirect_uri with state`() {
        withMockOAuth2Server {
            client.get(endSessionUrl("http://localhost/bye", "s1")).use {
                it.code shouldBe 302
                it.headers["location"] shouldBe "http://localhost/bye?state=s1"
            }
        }
    }

    @Test
    fun `end session request should add state to post_logout_redirect_uri that already has a query`() {
        withMockOAuth2Server {
            client.get(endSessionUrl("http://localhost/bye?a=1", "a b")).use {
                it.code shouldBe 302
                it.headers["location"] shouldBe "http://localhost/bye?a=1&state=a%20b"
            }
        }
    }

    @Test
    fun `end session request should add state to post_logout_redirect_uri with a custom scheme`() {
        withMockOAuth2Server {
            client.get(endSessionUrl("myapp://logout?a=1", "s1")).use {
                it.code shouldBe 302
                it.headers["location"] shouldBe "myapp://logout?a=1&state=s1"
            }
        }
    }

    @Test
    fun `end session request without state should redirect to post_logout_redirect_uri unchanged`() {
        withMockOAuth2Server {
            client.get(endSessionUrl("http://localhost/bye?a=1", null)).use {
                it.code shouldBe 302
                it.headers["location"] shouldBe "http://localhost/bye?a=1"
            }
        }
    }

    private fun MockOAuth2Server.endSessionUrl(
        postLogoutRedirectUri: String,
        state: String?,
    ): HttpUrl =
        endSessionEndpointUrl("default")
            .newBuilder()
            .addQueryParameter("post_logout_redirect_uri", postLogoutRedirectUri)
            .apply { if (state != null) addQueryParameter("state", state) }
            .build()
}
