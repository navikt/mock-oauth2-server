package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.token.KeyProvider.Companion.INITIAL_KEYS_FILE
import org.junit.jupiter.api.Test
import java.io.File
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

internal class KeyProviderTest {

    private val initialRSAKeysFile = File("src/main/resources$INITIAL_KEYS_FILE")

    private val initialECKeysFile = File("src/main/resources/mock-oauth2-server-keys-ec.json")

    @Test
    fun `signingKey should return a RSA key from initial keys file until deque is empty`() {
        val provider = KeyProvider()
        val initialPublicKeys = initialRsaPublicKeys()

        for (i in initialPublicKeys.indices) {
            provider.signingKey("issuer$i").asClue {
                it.toRSAKey().toRSAPublicKey() shouldBeIn initialPublicKeys
                it.keyID shouldBe "issuer$i"
            }
        }

        provider.signingKey("shouldBeGeneratedOnTheFly").asClue {
            it.toRSAKey().toRSAPublicKey() shouldNotBeIn initialPublicKeys
            it.keyID shouldBe "shouldBeGeneratedOnTheFly"
        }
    }

    @Test
    fun `signingKey should return a EC key from initial keys file until deque is empty`() {
        val provider = KeyProvider(
            initialKeys = KeyProvider.keysFromFile("/mock-oauth2-server-keys-ec.json"),
            algorithm = "ES256",
        )
        val initialPublicKeys = initialEcPublicKeys()

        for (i in initialPublicKeys.indices) {
            provider.signingKey("issuer$i").asClue {
                it.toECKey().toECPublicKey() shouldBeIn initialPublicKeys
                it.keyID shouldBe "issuer$i"
            }
        }

        provider.signingKey("shouldBeGeneratedOnTheFly").asClue {
            it.toECKey().toECPublicKey() shouldNotBeIn initialPublicKeys
            it.keyID shouldBe "shouldBeGeneratedOnTheFly"
        }
    }

    @Test
    fun `unsupported signingKey algorithm should throw an error message`() {
        val provider = KeyProvider(KeyProvider.keysFromFile("/mock-oauth2-server-keys-ec.json"))
        shouldThrow<OAuth2Exception> {
            provider.generate("ET256")
        }.message shouldBe "Unsupported algorithm: ET256"
    }

    @Test
    fun `signingKey should return a RSA key from provided constructor arg until deque is empty`() {
        val initialKeys = generateKeys(2)
        val provider = KeyProvider(initialKeys)
        val initialPublicKeys = initialKeys.map { it.toRSAPublicKey() }

        for (i in initialPublicKeys.indices) {
            provider.signingKey("issuer$i").asClue {
                it.toRSAKey().toRSAPublicKey() shouldBeIn initialPublicKeys
                it.keyID shouldBe "issuer$i"
            }
        }

        provider.signingKey("shouldBeGeneratedOnTheFly").asClue {
            it.toRSAKey().toRSAPublicKey() shouldNotBeIn initialPublicKeys
            it.keyID shouldBe "shouldBeGeneratedOnTheFly"
        }
    }

    private fun initialRsaPublicKeys(): List<RSAPublicKey> =
        initialRSAKeysFile.readText().let {
            JWKSet.parse(it).keys
        }.map {
            it.toRSAKey().toRSAPublicKey()
        }

    private fun initialEcPublicKeys(): List<ECPublicKey> =
        initialECKeysFile.readText().let {
            JWKSet.parse(it).keys
        }.map {
            it.toECKey().toECPublicKey()
        }

    private fun writeInitialKeysFile() {
        val list = generateKeys(5)
        initialRSAKeysFile.writeText(JWKSet(list).toString(false))
    }

    private fun generateKeys(numKeys: Int): List<RSAKey> {
        val list = mutableListOf<RSAKey>()
        for (i in 1..numKeys) {
            val key = KeyPairGenerator.getInstance("RSA").apply { this.initialize(2048) }
                .generateKeyPair()
                .let {
                    RSAKey.Builder(it.public as RSAPublicKey)
                        .privateKey(it.private as RSAPrivateKey)
                        .keyUse(KeyUse.SIGNATURE)
                        .keyID("initialkey-$i")
                        .build()
                }
            list.add(key)
        }
        return list
    }
}
