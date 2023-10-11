package no.nav.security.mock.oauth2.examples.clientcredentials

import com.nimbusds.jwt.SignedJWT
import no.nav.security.mock.oauth2.MockOAuth2Server
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ExampleAppWithClientCredentialsClientTest {
    private lateinit var client: OkHttpClient
    private lateinit var oAuth2Server: MockOAuth2Server
    private lateinit var exampleApp: ExampleAppWithClientCredentialsClient

    private val issuerId = "test"

    @BeforeEach
    fun before() {
        oAuth2Server = MockOAuth2Server()
        oAuth2Server.start()
        exampleApp = ExampleAppWithClientCredentialsClient(oAuth2Server.wellKnownUrl(issuerId).toString())
        exampleApp.start()
        client = OkHttpClient().newBuilder().build()
    }

    @AfterEach
    fun shutdown() {
        oAuth2Server.shutdown()
        exampleApp.shutdown()
    }

    @Test
    fun appShouldReturnClientCredentialsAccessTokenWhenInvoked() {
        val response: Response =
            client.newCall(
                Request.Builder()
                    .url(exampleApp.url("/clientcredentials"))
                    .get()
                    .build(),
            ).execute()
        assertThat(response.code).isEqualTo(200)

        val token: SignedJWT? =
            response.body?.string()
                ?.split("token=")
                ?.let { it[1] }
                ?.let { SignedJWT.parse(it) }

        assertThat(token).isNotNull
        assertThat(token?.jwtClaimsSet?.subject).isEqualTo("ExampleAppWithClientCredentialsClient")
    }
}
