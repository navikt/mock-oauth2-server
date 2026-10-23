package no.nav.security.mock.oauth2.http

import com.nimbusds.jose.util.JSONObjectUtils
import com.nimbusds.oauth2.sdk.ErrorObject
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class OAuth2HttpResponseTest {
    @Test
    fun `oauth2Error should keep the casing of error_description`() {
        val response = oauth2Error(ErrorObject("unsupported_token_type", "unsupported token type: MyHint", 400))

        response.status shouldBe 400
        val body = JSONObjectUtils.parse(checkNotNull(response.body))
        body["error"] shouldBe "unsupported_token_type"
        body["error_description"] shouldBe "unsupported token type: MyHint"
    }
}
