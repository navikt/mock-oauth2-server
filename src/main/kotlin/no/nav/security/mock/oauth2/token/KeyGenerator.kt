package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyType
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.util.Base64
import no.nav.security.mock.oauth2.OAuth2Exception
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

data class KeyGenerator(
    val algorithm: JWSAlgorithm = JWSAlgorithm.RS256,
    var keyGenerator: KeyPairGenerator = generate(algorithm.name)
) {
    fun generateKey(keyId: String, certificate: Certificate? = Certificate()): JWK {
        if (keyGenerator.algorithm != KeyType.RSA.value) {
            return keyGenerator.generateECKey(keyId, algorithm, certificate)
        }
        return keyGenerator.generateRSAKey(keyId, algorithm, certificate)
    }

    private fun KeyPairGenerator.generateECKey(keyId: String, algorithm: JWSAlgorithm, certificate: Certificate?): JWK =
        generateKeyPair()
            .let { keyPair ->
                ECKey.Builder(toCurve(algorithm), keyPair.public as ECPublicKey)
                    .privateKey(keyPair.private as ECPrivateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(keyId)
                    .algorithm(algorithm)
                    .apply {
                        this.addEcCertificate(keyPair, certificate)
                    }.build()
            }

    private fun toCurve(algorithm: JWSAlgorithm): Curve {
        return requireNotNull(
            Curve.forJWSAlgorithm(algorithm).single()
        ) {
            throw OAuth2Exception("Unsupported: $algorithm")
        }
    }

    private fun KeyPairGenerator.generateRSAKey(keyId: String, algorithm: JWSAlgorithm, certificate: Certificate?): JWK =
        generateKeyPair().let { keyPair ->
            RSAKey.Builder(keyPair.public as RSAPublicKey)
                .privateKey(keyPair.private as RSAPrivateKey)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(keyId)
                .algorithm(algorithm)
                .apply {
                    this.addRsaCertificate(keyPair, certificate)
                }.build()
        }

    private fun generateX509Certificate(keyPair: KeyPair, signatureAlgorithm: String, commonName: String, expire: Int) = mutableListOf(
        Base64.encode(
            try {
                CertificateGenerator.make(keyPair, signatureAlgorithm, commonName, expire)?.encoded
            } catch (e: Throwable) {
                throw OAuth2Exception("creating certificate ${e.message}")
            }
        )
    )

    private fun RSAKey.Builder.addRsaCertificate(keyPair: KeyPair, certificate: Certificate?): RSAKey.Builder? {
        certificate?.let {
            if (!certificate.x5cChain) {
                return null
            }

            return this.x509CertChain(
                generateX509Certificate(
                    keyPair,
                    certificate.findSignature(algorithm),
                    certificate.cn,
                    certificate.expiresInDays
                )
            )
        } ?: return null
    }

    private fun ECKey.Builder.addEcCertificate(keyPair: KeyPair, certificate: Certificate?): ECKey.Builder? {
        certificate?.let {
            if (!certificate.x5cChain) {
                return null
            }

            return this.x509CertChain(
                generateX509Certificate(
                    keyPair,
                    certificate.findSignature(algorithm),
                    certificate.cn,
                    certificate.expiresInDays
                )
            )
        } ?: return null
    }

    companion object {
        val rsaAlgorithmFamily = JWSAlgorithm.Family.RSA.toList()
        val ecAlgorithmFamily = JWSAlgorithm.Family.EC.filterNot {
            // ES256K is not a public used algorithm
            // ES512 is counted as "legacy" and is not supported
            it.name == "ES256K" || it.name == "ES512"
        }

        private val supportedAlgorithms = listOf(
            Algorithm(rsaAlgorithmFamily, KeyType.RSA),
            Algorithm(ecAlgorithmFamily, KeyType.EC)
        )

        fun isSupported(algorithm: JWSAlgorithm) = supportedAlgorithms.flatMap { it.family }.contains(algorithm)

        fun generate(algorithm: String): KeyPairGenerator {
            val parsedAlgo = JWSAlgorithm.parse(algorithm)
            return supportedAlgorithms.mapNotNull {
                if (it.family.contains(parsedAlgo)) {
                    KeyGenerator(
                        parsedAlgo,
                        KeyPairGenerator.getInstance(it.keyType.value).apply {
                            if (it.keyType.value != KeyType.RSA.value) {
                                this.initialize(parsedAlgo.name.subSequence(2, 5).toString().toInt())
                            } else {
                                this.initialize(RSAKeyGenerator.MIN_KEY_SIZE_BITS)
                            }
                        }
                    ).keyGenerator
                } else null
            }.singleOrNull() ?: throw OAuth2Exception("Unsupported algorithm: $algorithm")
        }

        data class Algorithm(
            val family: List<JWSAlgorithm>,
            val keyType: KeyType
        )
    }
}
