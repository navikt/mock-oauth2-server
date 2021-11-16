package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.token.KeyProvider.Companion.INITIAL_KEYS_FILE
import org.junit.jupiter.api.Test
import java.io.File
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

internal class KeyProviderTest {

    private val initialKeysFile = File("src/main/resources$INITIAL_KEYS_FILE")

    @Test
    fun `signingKey should return a key from initial keys file until deque is empty`() {
        val provider = KeyProvider()
        val initialPublicKeys = initialPublicKeys()

        for (i in initialPublicKeys.indices) {
            provider.signingKey("issuer$i").asClue {
                it.toRSAPublicKey() shouldBeIn initialPublicKeys
                it.keyID shouldBe "issuer$i"
            }
        }

        provider.signingKey("shouldBeGeneratedOnTheFly").asClue {
            it.toRSAPublicKey() shouldNotBeIn initialPublicKeys
            it.keyID shouldBe "shouldBeGeneratedOnTheFly"
        }
    }

    @Test
    fun `signingKey should return a key from provided constructor arg until deque is empty`() {
        val initialKeys = generateKeys(2)
        val provider = KeyProvider(initialKeys)
        val initialPublicKeys = initialKeys.map { it.toRSAPublicKey() }

        for (i in initialPublicKeys.indices) {
            provider.signingKey("issuer$i").asClue {
                it.toRSAPublicKey() shouldBeIn initialPublicKeys
                it.keyID shouldBe "issuer$i"
            }
        }

        provider.signingKey("shouldBeGeneratedOnTheFly").asClue {
            it.toRSAPublicKey() shouldNotBeIn initialPublicKeys
            it.keyID shouldBe "shouldBeGeneratedOnTheFly"
        }
    }

    private fun initialPublicKeys(): List<RSAPublicKey> =
        initialKeysFile.readText().let {
            JWKSet.parse(it).keys
        }.map {
            it.toRSAKey().toRSAPublicKey()
        }

    private fun writeInitialKeysFile() {
        val list = generateKeys(5)
        initialKeysFile.writeText(JWKSet(list).toString(false))
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
