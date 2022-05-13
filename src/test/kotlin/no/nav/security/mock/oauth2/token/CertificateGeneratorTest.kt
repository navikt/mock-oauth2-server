package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.KeyType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.OAuth2Exception
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.security.KeyPairGenerator

class CertificateGeneratorTest {

    @Test
    fun `using an unknown signature should throw error for not supported`() {
        assertThrows<OAuth2Exception> {
            CertificateConfig().findSignature(JWSAlgorithm.parse("RS1010"))
        }
    }

    @Test
    fun `generate certificate and validate metadata`() {
        val keypair = KeyPairGenerator.getInstance(KeyType.RSA.value).generateKeyPair()
        val certConfig = CertificateConfig()
        val signature = certConfig.findSignature(JWSAlgorithm.RS256)
        val cert = CertificateGenerator.make(keypair, signature, "test", 10)

        cert shouldNotBe null
        cert?.let {
            assertDoesNotThrow {
                it.checkValidity()
            }
            it.subjectX500Principal.name shouldBe "CN=test"
            it.sigAlgName shouldBe "SHA256withRSA"
        }
    }
}
