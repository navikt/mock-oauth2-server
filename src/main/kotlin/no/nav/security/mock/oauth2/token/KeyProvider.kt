package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import no.nav.security.mock.oauth2.OAuth2Exception
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingDeque

const val RSA_KEY_TYPE = "RSA"
const val EC_KEY_TYPE = "EC"
const val RSA_KEY_SIZE_256 = 2048

open class KeyProvider @JvmOverloads constructor(
    private val initialKeys: List<JWK> = keysFromFile(INITIAL_KEYS_FILE),
    private val keyTypeAndKeySize: Pair<String, Int> = RSA_KEY_TYPE to RSA_KEY_SIZE_256
) {
    private val signingKeys: ConcurrentHashMap<String, JWK> = ConcurrentHashMap()

    private val generator = KeyPairGenerator.getInstance(keyTypeAndKeySize.first).apply { this.initialize(keyTypeAndKeySize.second) }

    private val keyDeque = LinkedBlockingDeque<JWK>().apply {
        initialKeys.forEach {
            put(it)
        }
    }

    fun signingKey(keyId: String): JWK = signingKeys.computeIfAbsent(keyId) { keyFromDequeOrNew(keyId) }

    private fun keyFromDequeOrNew(keyId: String): JWK = keyDeque.poll()?.let { polledJwk ->
        when (polledJwk.keyType.value) {
            RSA_KEY_TYPE -> {
                RSAKey.Builder(polledJwk.toRSAKey()).keyID(keyId).build()
            }
            EC_KEY_TYPE -> {
                ECKey.Builder(polledJwk.toECKey()).keyID(keyId).build()
            }
            else -> {
                throw OAuth2Exception("Unsupported key type (kty): ${polledJwk.keyType.value}")
            }
        }
    } ?: generator.generateKey(keyId, keyTypeAndKeySize.first)


    private fun KeyPairGenerator.generateKey(keyId: String, keyType: String): JWK =
        when (keyType) {
            RSA_KEY_TYPE -> {
                this.generateRSAKey(keyId)
            }
            EC_KEY_TYPE -> {
                this.generateECKey(keyId)
            }
            else -> {
                throw OAuth2Exception("Unsupported key type (kty): $keyType")
            }
        }

    private fun KeyPairGenerator.generateECKey(keyId: String): JWK =
        generateKeyPair()
            .let {
                ECKey.Builder(Curve.P_256, it.public as ECPublicKey)
                    .privateKey(it.private as ECPrivateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(keyId)
                    .build()
            }

    private fun KeyPairGenerator.generateRSAKey(keyId: String): JWK =
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

        fun keysFromFile(filename: String): List<JWK> {
            val keysFromFile = KeyProvider::class.java.getResource(filename)
            if (keysFromFile != null) {
                return JWKSet.parse(keysFromFile.readText()).keys.map { it as JWK }
            }
            return emptyList()
        }
    }
}
