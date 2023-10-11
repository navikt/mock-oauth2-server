package no.nav.security.mock.oauth2.examples.securedapi

import com.nimbusds.jwt.SignedJWT
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ExampleAppWithSecuredApiTest {
    private lateinit var client: OkHttpClient
    private lateinit var oAuth2Server: MockOAuth2Server
    private lateinit var exampleApp: ExampleAppWithSecuredApi

    private val issuerId = "test"

    @BeforeEach
    fun before() {
        oAuth2Server = MockOAuth2Server()
        oAuth2Server.start()
        exampleApp = ExampleAppWithSecuredApi(oAuth2Server.wellKnownUrl(issuerId).toString())
        exampleApp.start()
        client = OkHttpClient().newBuilder().build()
    }

    @AfterEach
    fun shutdown() {
        oAuth2Server.shutdown()
        exampleApp.shutdown()
    }

    @Test
    fun apiShouldDenyAccessWithoutValidToken() {
        val response: Response =
            client.newCall(
                Request.Builder()
                    .url(exampleApp.url("/api"))
                    .get()
                    .build(),
            ).execute()
        assertThat(response.code).isEqualTo(401)
    }

    @Test
    fun apiShouldAllowAccessWhenTokenIsValid() {
        val token: SignedJWT = oAuth2Server.issueToken(issuerId, "myclient", DefaultOAuth2TokenCallback())
        val response: Response =
            client.newCall(
                Request.Builder()
                    .url(exampleApp.url("/api"))
                    .addHeader("Authorization", "Bearer " + token.serialize())
                    .get()
                    .build(),
            ).execute()
        assertThat(response.code).isEqualTo(200)
        assertThat(response.body.string()).contains(token.jwtClaimsSet.subject)
    }
}
