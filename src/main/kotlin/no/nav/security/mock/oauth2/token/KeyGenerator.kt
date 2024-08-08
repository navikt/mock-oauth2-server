package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyType
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
    val algorithm: JWSAlgorithm = JWSAlgorithm.RS256,
    var keyGenerator: KeyPairGenerator = generate(algorithm.name),
) {
    fun generateKey(keyId: String): JWK {
        if (keyGenerator.algorithm != KeyType.RSA.value) {
            return keyGenerator.generateECKey(keyId, algorithm)
        }
        return keyGenerator.generateRSAKey(keyId, algorithm)
    }

    private fun KeyPairGenerator.generateECKey(
        keyId: String,
        algorithm: JWSAlgorithm,
    ): JWK =
        generateKeyPair()
            .let {
                ECKey
                    .Builder(toCurve(algorithm), it.public as ECPublicKey)
                    .privateKey(it.private as ECPrivateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(keyId)
                    .algorithm(algorithm)
                    .build()
            }

    private fun toCurve(algorithm: JWSAlgorithm): Curve =
        requireNotNull(
            Curve.forJWSAlgorithm(algorithm).single(),
        ) {
            throw OAuth2Exception("Unsupported: $algorithm")
        }

    private fun KeyPairGenerator.generateRSAKey(
        keyId: String,
        algorithm: JWSAlgorithm,
    ): JWK =
        generateKeyPair()
            .let {
                RSAKey
                    .Builder(it.public as RSAPublicKey)
                    .privateKey(it.private as RSAPrivateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(keyId)
                    .algorithm(algorithm)
                    .build()
            }

    companion object {
        val rsaAlgorithmFamily = JWSAlgorithm.Family.RSA.toList()
        val ecAlgorithmFamily =
            JWSAlgorithm.Family.EC.filterNot {
                // ES256K is not a public used algorithm
                // ES512 is counted as "legacy" and is not supported
                it.name == "ES256K" || it.name == "ES512"
            }

        private val supportedAlgorithms =
            listOf(
                Algorithm(rsaAlgorithmFamily, KeyType.RSA),
                Algorithm(ecAlgorithmFamily, KeyType.EC),
            )

        fun isSupported(algorithm: JWSAlgorithm) = supportedAlgorithms.flatMap { it.family }.contains(algorithm)

        fun generate(algorithm: String): KeyPairGenerator {
            val parsedAlgo = JWSAlgorithm.parse(algorithm)
            return supportedAlgorithms
                .mapNotNull {
                    if (it.family.contains(parsedAlgo)) {
                        KeyGenerator(
                            parsedAlgo,
                            KeyPairGenerator.getInstance(it.keyType.value).apply {
                                if (it.keyType.value != KeyType.RSA.value) {
                                    this.initialize(
                                        parsedAlgo.name
                                            .subSequence(2, 5)
                                            .toString()
                                            .toInt(),
                                    )
                                } else {
                                    this.initialize(RSAKeyGenerator.MIN_KEY_SIZE_BITS)
                                }
                            },
                        ).keyGenerator
                    } else {
                        null
                    }
                }.singleOrNull() ?: throw OAuth2Exception("Unsupported algorithm: $algorithm")
        }

        data class Algorithm(
            val family: List<JWSAlgorithm>,
            val keyType: KeyType,
        )
    }
}
