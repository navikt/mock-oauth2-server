package no.nav.security.mock.oauth2.login

import com.nimbusds.oauth2.sdk.OAuth2Error
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.templateMapper
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

internal class LoginRequestHandlerTest {

    private val handler = LoginRequestHandler(templateMapper, OAuth2Config())

    @Test
    fun `loginSubmit should return login with username and claims from form params`() {
        handler.loginSubmit(request("username=foo&claims=someJsonString")).asClue {
            it shouldBe Login("foo", claims = "someJsonString")
        }
    }

    @Test
    fun `loginSubmit should fail with OAuth2Error invalid_request when missing required params`() {
        shouldThrow<OAuth2Exception> {
            handler.loginSubmit(request("param=value"))
        }.asClue {
            it.errorObject?.code shouldBe OAuth2Error.INVALID_REQUEST.code
        }
    }

    private fun request(body: String) =
        OAuth2HttpRequest(
            originalUrl = "http://localhost/issuer1/login".toHttpUrl(),
            headers = Headers.headersOf(),
            method = "POST",
            body = body
        )
}
