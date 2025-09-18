package no.nav.security.mock.oauth2.extensions

import io.kotest.matchers.shouldBe
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

internal class HttpUrlExtensionsTest {
    @Test
    fun `urls with no segments, one segment and multiple segments`() {
        "http://localhost".toHttpUrl().issuerId() shouldBe ""
        `verify oauth2 endpoint urls`("http://localhost")

        "http://localhost/path1".toHttpUrl().issuerId() shouldBe "path1"
        `verify oauth2 endpoint urls`("http://localhost/path1")

        "http://localhost/path1/path2".toHttpUrl().issuerId() shouldBe "path1/path2"
        `verify oauth2 endpoint urls`("http://localhost/path1/path2")
    }

    private fun `verify oauth2 endpoint urls`(baseUrl: String) {
        val httpUrl = baseUrl.toHttpUrl()
        httpUrl.toIssuerUrl() shouldBe baseUrl.toHttpUrl()
        httpUrl.toWellKnownUrl() shouldBe "$baseUrl/.well-known/openid-configuration".toHttpUrl()
        httpUrl.toOAuth2AuthorizationServerMetadataUrl() shouldBe "$baseUrl/.well-known/oauth-authorization-server".toHttpUrl()
        httpUrl.toTokenEndpointUrl() shouldBe "$baseUrl/token".toHttpUrl()
        httpUrl.toAuthorizationEndpointUrl() shouldBe "$baseUrl/authorize".toHttpUrl()
        httpUrl.toDebuggerCallbackUrl() shouldBe "$baseUrl/debugger/callback".toHttpUrl()
        httpUrl.toDebuggerUrl() shouldBe "$baseUrl/debugger".toHttpUrl()
        httpUrl.toEndSessionEndpointUrl() shouldBe "$baseUrl/endsession".toHttpUrl()
        httpUrl.toRevocationEndpointUrl() shouldBe "$baseUrl/revoke".toHttpUrl()
        httpUrl.toJwksUrl() shouldBe "$baseUrl/jwks".toHttpUrl()
    }
}
