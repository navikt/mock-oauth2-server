package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyType
import com.nimbusds.jose.jwk.RSAKey
import no.nav.security.mock.oauth2.OAuth2Exception
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingDeque

open class KeyProvider @JvmOverloads constructor(
    private val initialKeys: List<JWK> = keysFromFile(INITIAL_KEYS_FILE),
    private val algorithm: String = JWSAlgorithm.RS256.name
) {
    private val signingKeys: ConcurrentHashMap<String, JWK> = ConcurrentHashMap()

    private var generator: KeyGenerator = KeyGenerator(JWSAlgorithm.parse(algorithm))

    private val keyDeque = LinkedBlockingDeque<JWK>().apply {
        initialKeys.forEach {
            put(it)
        }
    }

    fun signingKey(keyId: String): JWK = signingKeys.computeIfAbsent(keyId) { keyFromDequeOrNew(keyId) }

    private fun keyFromDequeOrNew(keyId: String): JWK = keyDeque.poll()?.let { polledJwk ->
        when (polledJwk.keyType.value) {
            KeyType.RSA.value -> {
                RSAKey.Builder(polledJwk.toRSAKey()).keyID(keyId).build()
            }
            KeyType.EC.value -> {
                ECKey.Builder(polledJwk.toECKey()).keyID(keyId).build()
            }
            else -> {
                throw OAuth2Exception("Unsupported key type: ${polledJwk.keyType.value}")
            }
        }
    } ?: generator.generateKey(keyId)

    fun algorithm(): JWSAlgorithm = JWSAlgorithm.parse(algorithm)

    fun keyType(): String = generator.keyGenerator.algorithm

    fun generate(algorithm: String) {
        generator = KeyGenerator(JWSAlgorithm.parse(algorithm))
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
