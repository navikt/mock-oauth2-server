package no.nav.security.mock.oauth2.examples.openidconnect

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExampleAppWithOpenIdConnectTest {
    private lateinit var client: OkHttpClient
    private lateinit var oAuth2Server: MockOAuth2Server
    private lateinit var exampleApp: ExampleAppWithOpenIdConnect

    private val issuerId = "test"

    @BeforeEach
    fun before() {
        oAuth2Server = MockOAuth2Server()
        oAuth2Server.start()
        exampleApp = ExampleAppWithOpenIdConnect(oAuth2Server.wellKnownUrl(issuerId).toString())
        exampleApp.start()
        client =
            OkHttpClient()
                .newBuilder()
                .followRedirects(true)
                .cookieJar(InmemoryCookieJar())
                .build()
    }

    @AfterEach
    fun shutdown() {
        oAuth2Server.shutdown()
        exampleApp.shutdown()
    }

    @Test
    fun loginWithOpenIdConnect() {
        val loginResponse = client.newCall(Request.Builder().url(exampleApp.url("/login")).build()).execute()
        assertThat(loginResponse.headers["Set-Cookie"]).contains("id_token=")
    }

    @Test
    fun loginAndAccessSecuredPathWithIdTokenForSubjectFoo() {
        oAuth2Server.enqueueCallback(
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = "foo",
            ),
        )
        val loginResponse = client.newCall(Request.Builder().url(exampleApp.url("/login")).build()).execute()
        assertThat(loginResponse.headers["Set-Cookie"]).contains("id_token=")
        val securedResponse = client.newCall(Request.Builder().url(exampleApp.url("/secured")).build()).execute()
        assertThat(securedResponse.code).isEqualTo(200)
        val body = securedResponse.body?.string()
        assertThat(body).isEqualTo("welcome foo")
    }

    @Test
    fun requestToSecuredPathShouldRedirectToLogin() {
        val loginResponse =
            OkHttpClient()
                .newBuilder()
                .followRedirects(false)
                .build()
                .newCall(Request.Builder().url(exampleApp.url("/secured")).build())
                .execute()
        assertThat(loginResponse.code).isEqualTo(302)
        assertThat(loginResponse.headers["Location"]).isEqualTo(exampleApp.url("/login").toString())
    }

    internal class InmemoryCookieJar : CookieJar {
        private val cookieList: MutableList<Cookie> = mutableListOf()

        override fun loadForRequest(url: HttpUrl): List<Cookie> = cookieList

        override fun saveFromResponse(
            url: HttpUrl,
            cookies: List<Cookie>,
        ) {
            cookieList.addAll(cookies)
        }
    }
}
