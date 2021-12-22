package no.nav.security.mock.oauth2.token

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.token.KeyProvider.Companion.INITIAL_KEYS_FILE
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.IllegalStateException
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import kotlin.test.assertEquals

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

    @Test
    fun `premapped Issuer JWKs are stored`() {
        val initialMappedKeys = initialPremappedKeys()
        val provider = KeyProvider(initialMappedKeys = initialMappedKeys)
        initialMappedKeys.keys.forEach {
            assert(provider.signingKey(it).equals(initialMappedKeys[it]))
        }
    }

    @Test
    fun `reading premapped issuer jwks config file behaves as expected`() {
        val jsonFile = "./src/test/resources/" + "premapped_issuer_jwks_testfile.json"
        System.setProperty("PREDEFINED_ISSUER_JWKS", jsonFile)

        val jsonObj = jacksonObjectMapper().readValue(File(jsonFile), ObjectNode::class.java)

        val keyProvider = KeyProvider()
        val issuers = listOf("aad", "other3")
        for (iss in issuers) {
            val actual = keyProvider.signingKey(iss)
            val expected = JWK.parse(jsonObj.get(iss).toString()).toRSAKey()
            assertEquals(expected, actual)
        }
        System.clearProperty("PREDEFINED_ISSUER_JWKS")
    }

    @Test
    fun `reading invalid premapped issuer jwks config file fails`() {
        val jsonFile = "./src/test/resources/" + "premapped_issuer_jwks_testfile_invalid.json"
        System.setProperty("PREDEFINED_ISSUER_JWKS", jsonFile)

        shouldThrow<IllegalStateException> {
            KeyProvider()
        }
        System.clearProperty("PREDEFINED_ISSUER_JWKS")
    }

    private fun initialPublicKeys(): List<RSAPublicKey> =
        initialKeysFile.readText().let {
            JWKSet.parse(it).keys
        }.map {
            it.toRSAKey().toRSAPublicKey()
        }

    private fun initialPremappedKeys(): Map<String, RSAKey> {
        val out = mutableMapOf<String, RSAKey>()
        val issuerNames = listOf("iss1", "iss2", "iss3")
        val keys = generateKeys(3).map { it.toRSAKey() }
        issuerNames.indices.forEach {
            out[issuerNames[it]] = keys[it]
        }
        return out.toMap()
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
