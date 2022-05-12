package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyType
import com.nimbusds.jose.util.X509CertUtils
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.token.KeyProvider.Companion.keysFromFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.File
import java.security.interfaces.RSAPublicKey

class KeyProviderx5cTest {

    private val initialRSAX5cKeysFile = File("src/main/resources/mock-oauth2-server-x5c-keys.json")

    @Test
    fun `signingKey should return a RSA key from initial x5c certificate until deque is empty`() {
        val keys = keysFromFile(filename = "/mock-oauth2-server-x5c-keys.json")
        val provider = KeyProvider(certificate = Certificate(x5cChain = true), initialKeys = keys)
        val initialPublicKeys = initialRsaX5cPublicKeys()

        provider.signingKey(MOCK_OAUTH2_SERVER_NAME).asClue { jwk ->
            jwk.toRSAKey().toRSAPublicKey() shouldBeIn initialPublicKeys
            jwk.keyID shouldBe "mock-oauth2-server"
            jwk.keyType shouldBe KeyType.RSA
            jwk.isPrivate shouldBe true
            jwk.x509CertChain shouldNotBe null
            X509CertUtils.parse("-----BEGIN CERTIFICATE-----${jwk.x509CertChain}-----END CERTIFICATE-----").asClue {
                assertDoesNotThrow {
                    it.checkValidity()
                }
                it.subjectX500Principal.name shouldBe "CN=mock-oauth2-server"
                it.sigAlgName.toString() shouldBe "SHA256withRSA"
                it.notAfter.time shouldBe 1715375571000L
                it.notBefore.time shouldBe 1652303571000L
                it.sigAlgOID.toString() shouldBe "1.2.840.113549.1.1.11"
            }
        }

        provider.signingKey("shouldBeGeneratedOnTheFly").asClue { jwk ->
            jwk.toRSAKey().toRSAPublicKey() shouldNotBeIn initialPublicKeys
            jwk.keyType shouldBe KeyType.RSA
            jwk.x509CertChain shouldNotBe null
            X509CertUtils.parse("-----BEGIN CERTIFICATE-----${jwk.x509CertChain}-----END CERTIFICATE-----").asClue {
                assertDoesNotThrow {
                    it.checkValidity()
                }
                it.subjectX500Principal.name shouldBe "CN=mock-oauth2-server"
            }
            jwk.keyID shouldBe "shouldBeGeneratedOnTheFly"
            jwk.isPrivate shouldBe true
        }
    }

    private fun initialRsaX5cPublicKeys(): List<RSAPublicKey> =
        initialRSAX5cKeysFile.readText().let {
            JWKSet.parse(it).keys
        }.map {
            it.toRSAKey().toRSAPublicKey()
        }
}
