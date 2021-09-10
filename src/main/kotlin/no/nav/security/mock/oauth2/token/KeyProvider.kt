package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingDeque

open class KeyProvider @JvmOverloads constructor(
    private val initialKeys: List<RSAKey> = keysFromFile(INITIAL_KEYS_FILE)
) {
    private val signingKeys: ConcurrentHashMap<String, RSAKey> = ConcurrentHashMap()

    private val generator = KeyPairGenerator.getInstance("RSA").apply { this.initialize(2048) }

    private val keyDeque = LinkedBlockingDeque<RSAKey>().apply {
        initialKeys.forEach {
            put(it)
        }
    }

    fun signingKey(keyId: String): RSAKey = signingKeys.computeIfAbsent(keyId) { keyFromDequeOrNew(keyId) }

    private fun keyFromDequeOrNew(keyId: String): RSAKey = keyDeque.poll()?.let {
        RSAKey.Builder(it).keyID(keyId).build()
    } ?: generator.generateRSAKey(keyId)

    private fun KeyPairGenerator.generateRSAKey(keyId: String): RSAKey =
        generateKeyPair()
            .let {
                RSAKey.Builder(it.public as RSAPublicKey)
                    .privateKey(it.private as RSAPrivateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(keyId)
                    .build()
            }

    companion object {
        const val INITIAL_KEYS_FILE = "/mock-oauth2-server-keys.json"

        fun keysFromFile(filename: String): List<RSAKey> {
            val keysFromFile = KeyProvider::class.java.getResource(filename)
            if (keysFromFile != null) {
                return JWKSet.parse(keysFromFile.readText()).keys.map { it as RSAKey }
            }
            return emptyList()
        }
    }
}
