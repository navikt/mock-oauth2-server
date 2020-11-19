package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.jwk.KeyType
import com.nimbusds.jose.jwk.KeyUse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class OAuth2TokenProviderTest {
    private val jwkSet = OAuth2TokenProvider().publicJwkSet()

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
}
