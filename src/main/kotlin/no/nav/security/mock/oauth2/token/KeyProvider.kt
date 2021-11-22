package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.token.KeyGenerator.Companion.EC_KEY_TYPE
import no.nav.security.mock.oauth2.token.KeyGenerator.Companion.RSA_KEY_TYPE
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingDeque

open class KeyProvider @JvmOverloads constructor(
    private val initialKeys: List<JWK> = keysFromFile(INITIAL_KEYS_FILE),
) {
    private val signingKeys: ConcurrentHashMap<String, JWK> = ConcurrentHashMap()

    private var generator: KeyGenerator = KeyGenerator()

    private val keyDeque = LinkedBlockingDeque<JWK>().apply {
        initialKeys.forEach {
            put(it)
        }
    }

    fun regenerate(algorithm: String) {
        generator = KeyGenerator.generate(algorithm)
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
                throw OAuth2Exception("Unsupported key type: ${polledJwk.keyType.value}")
            }
        }
    } ?: generator.generateKey(keyId)

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
