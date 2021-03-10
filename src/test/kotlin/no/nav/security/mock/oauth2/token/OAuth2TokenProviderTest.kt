package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.jwk.KeyType
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.oauth2.sdk.GrantType
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.testutils.nimbusTokenRequest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

internal class OAuth2TokenProviderTest {
    private val tokenProvider = OAuth2TokenProvider()
    private val jwkSet = tokenProvider.publicJwkSet()

    @Test
    fun `public jwks returns public part of JWKs`() =
        jwkSet.keys.any { it.isPrivate } shouldNotBe true

    @Test
    fun `all keys in public jwks should contain kty, use and kid`() {
        jwkSet.keys.forEach {
            it.keyID shouldNotBe null
            it.keyType shouldBe KeyType.RSA
            it.keyUse shouldBe KeyUse.SIGNATURE
        }
    }

    @Test
    fun `claims from tokencallback should be added to token in tokenExchange`() {
        val initialToken = tokenProvider.jwt(
            mapOf(
                "iss" to "http://initialissuer",
                "sub" to "initialsubject",
                "aud" to "initialaudience",
                "initialclaim" to "initialclaim"
            )
        )

        tokenProvider.exchangeAccessToken(
            tokenRequest = nimbusTokenRequest(
                "myclient",
                "grant_type" to GrantType.JWT_BEARER.value,
                "scope" to "scope1",
                "assertion" to initialToken.serialize()
            ),
            "http://default_if_not_overridden".toHttpUrl(),
            initialToken.jwtClaimsSet,
            DefaultOAuth2TokenCallback(
                claims = mapOf(
                    "extraclaim" to "extra",
                    "iss" to "http://overrideissuer"
                )
            )
        ).jwtClaimsSet.asClue {
            it.issuer shouldBe "http://overrideissuer"
            it.subject shouldBe "initialsubject"
            it.audience shouldBe listOf("scope1")
            it.claims["initialclaim"] shouldBe "initialclaim"
            it.claims["extraclaim"] shouldBe "extra"
        }
    }
}
