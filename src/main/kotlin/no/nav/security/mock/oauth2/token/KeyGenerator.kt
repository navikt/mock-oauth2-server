package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import no.nav.security.mock.oauth2.OAuth2Exception
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

data class KeyGenerator(
    var keyGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(RSA_KEY_TYPE).apply {
        this.initialize(RSAKeyGenerator.MIN_KEY_SIZE_BITS)
    },
    var currentAlgorithm: JWSAlgorithm = JWSAlgorithm.RS256
) {
    fun generateKey(keyId: String): JWK {
        if (keyGenerator.algorithm == RSA_KEY_TYPE) {
            return keyGenerator.generateRSAKey(keyId)
        }
        return keyGenerator.generateECKey(keyId, currentAlgorithm)
    }

    private fun KeyPairGenerator.generateECKey(keyId: String, algorithm: JWSAlgorithm): JWK =
        generateKeyPair()
            .let {
                ECKey.Builder(toCurve(algorithm), it.public as ECPublicKey)
                    .privateKey(it.private as ECPrivateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(keyId)
                    .build()
            }

    private fun toCurve(algorithm: JWSAlgorithm): Curve {
        return requireNotNull(
            Curve.forJWSAlgorithm(algorithm).single()
        ) {
            throw OAuth2Exception("Unsupported: $algorithm")
        }
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

        const val RSA_KEY_TYPE = "RSA"
        const val EC_KEY_TYPE = "EC"

        private val supportedAlgorithms =
            mapOf(
                JWSAlgorithm.RS256 to Requirement(RSA_KEY_TYPE, RSAKeyGenerator.MIN_KEY_SIZE_BITS),
                JWSAlgorithm.RS512 to Requirement(RSA_KEY_TYPE, 512),
                JWSAlgorithm.ES256 to Requirement(EC_KEY_TYPE, 256)
            )

        fun generate(algorithm: String): KeyGenerator {
            val parsedAlgo = JWSAlgorithm.parse(algorithm)

            val algo = requireNotNull(supportedAlgorithms[parsedAlgo]?.type) {
                throw OAuth2Exception("Unsupported: $algorithm")
            }

            val key = requireNotNull(supportedAlgorithms[parsedAlgo]?.keySize) {
                throw OAuth2Exception("Unsupported: $algorithm")
            }

            return KeyGenerator(
                KeyPairGenerator.getInstance(algo).apply {
                    this.initialize(key)
                }, parsedAlgo
            )
        }

        data class Requirement(
            val type: String,
            val keySize: Int,
        )
    }
}
