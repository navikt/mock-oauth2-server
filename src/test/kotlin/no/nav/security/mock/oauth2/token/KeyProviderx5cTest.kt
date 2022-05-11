package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyType
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.token.KeyProvider.Companion.keysFromFile
import org.junit.jupiter.api.Test
import java.io.File
import java.security.interfaces.RSAPublicKey

class KeyProviderx5cTest {

    private val initialRSAX5cKeysFile = File("src/main/resources/mock-oauth2-server-x5c-keys.json")

    @Test
    fun `signingKey should return a RSA key from initial x5c certificate until deque is empty`() {
        val keys = keysFromFile(filename = "/mock-oauth2-server-x5c-keys.json")
        val provider = KeyProvider(x5cCertificateChain = true, initialKeys = keys)
        val initialPublicKeys = initialRsaX5cPublicKeys()

        provider.signingKey(KeyGenerator.MOCK_OAUTH2_SERVER_NAME).asClue {
            it.toRSAKey().toRSAPublicKey() shouldBeIn initialPublicKeys
            it.keyID shouldBe "mock-oauth2-server"
            it.keyType shouldBe KeyType.RSA
            it.isPrivate shouldBe true
            it.x509CertChain shouldNotBe null
            it.computeThumbprint().toString() shouldBe "XI_0_o7bgCzz3atyVifjZfEeAztOy-DrIdWFXTiLhng"
        }

        provider.signingKey("shouldBeGeneratedOnTheFly").asClue {
            it.toRSAKey().toRSAPublicKey() shouldNotBeIn initialPublicKeys
            it.keyType shouldBe KeyType.RSA
            it.x509CertChain shouldNotBe null
            it.keyID shouldBe "shouldBeGeneratedOnTheFly"
            it.isPrivate shouldBe true
        }
    }

    private fun initialRsaX5cPublicKeys(): List<RSAPublicKey> =
        initialRSAX5cKeysFile.readText().let {
            JWKSet.parse(it).keys
        }.map {
            it.toRSAKey().toRSAPublicKey()
        }
}
