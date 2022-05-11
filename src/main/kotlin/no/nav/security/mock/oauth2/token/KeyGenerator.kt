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
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509ExtensionUtils
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.OperatorCreationException
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.time.Instant
import java.util.Date

data class KeyGenerator(
    val algorithm: JWSAlgorithm = JWSAlgorithm.RS256,
    var keyGenerator: KeyPairGenerator = generate(algorithm.name)
) {
    fun generateKey(keyId: String, x5cCertificateChain: Boolean): JWK {
        if (keyGenerator.algorithm != KeyType.RSA.value) {
            return keyGenerator.generateECKey(keyId, algorithm, x5cCertificateChain)
        }
        return keyGenerator.generateRSAKey(keyId, algorithm, x5cCertificateChain)
    }

    private fun KeyPairGenerator.generateECKey(keyId: String, algorithm: JWSAlgorithm, x5cCertificate: Boolean): JWK =
        generateKeyPair()
            .let {
                ECKey.Builder(toCurve(algorithm), it.public as ECPublicKey)
                    .privateKey(it.private as ECPrivateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(keyId)
                    .algorithm(algorithm)
                    .apply {
                        if (x5cCertificate) {
                            this.x509CertChain(generateX509Certificate(it, "SHA256WITHECDSA"))
                        }
                    }.build()
            }

    private fun toCurve(algorithm: JWSAlgorithm): Curve {
        return requireNotNull(
            Curve.forJWSAlgorithm(algorithm).single()
        ) {
            throw OAuth2Exception("Unsupported: $algorithm")
        }
    }

    private fun KeyPairGenerator.generateRSAKey(keyId: String, algorithm: JWSAlgorithm, x5cCertificate: Boolean): JWK =
        generateKeyPair().let {
            RSAKey.Builder(it.public as RSAPublicKey)
                .privateKey(it.private as RSAPrivateKey)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(keyId)
                .algorithm(algorithm)
                .apply {
                    if (x5cCertificate) {
                        this.x509CertChain(generateX509Certificate(it, "SHA256withRSA"))
                    }
                }.build()
        }

    private fun generateX509Certificate(keyPair: KeyPair, hashAlgorithm: String) = mutableListOf(
        Base64.encode(
            certificate(keyPair, hashAlgorithm)?.encoded ?: throw OAuth2Exception("encoding certificate ${keyPair.public.algorithm}")
        )
    )

    private fun certificate(
        keyPair: KeyPair,
        hashAlgorithm: String,
        cn: String = MOCK_OAUTH2_SERVER_NAME,
        days: Int = DAYS_TO_EXPIRE
    ): X509Certificate? {
        val now = Instant.now()
        val notBefore = Date.from(now)
        val notAfter = Date.from(now.plus(Duration.ofDays(days.toLong())))
        val contentSigner: ContentSigner = JcaContentSignerBuilder(hashAlgorithm).build(keyPair.private)
        val x500Name = X500Name("CN=$cn")
        val certificateBuilder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
            x500Name,
            BigInteger.valueOf(now.toEpochMilli()),
            notBefore,
            notAfter,
            x500Name,
            keyPair.public
        )
            .addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyId(keyPair.public))
            .addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyId(keyPair.public))
            .addExtension(Extension.basicConstraints, true, BasicConstraints(true))
        return JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider()).getCertificate(certificateBuilder.build(contentSigner))
    }

    @Throws(OperatorCreationException::class)
    private fun createSubjectKeyId(publicKey: PublicKey): SubjectKeyIdentifier {
        val publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.encoded)
        val digCalc = BcDigestCalculatorProvider().get(AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1))
        return X509ExtensionUtils(digCalc).createSubjectKeyIdentifier(publicKeyInfo)
    }

    @Throws(OperatorCreationException::class)
    private fun createAuthorityKeyId(publicKey: PublicKey): AuthorityKeyIdentifier {
        val publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.encoded)
        val digCalc = BcDigestCalculatorProvider().get(AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1))

        return X509ExtensionUtils(digCalc).createAuthorityKeyIdentifier(publicKeyInfo)
    }

    companion object {
        const val MOCK_OAUTH2_SERVER_NAME = "mock-oauth2-server"
        const val DAYS_TO_EXPIRE = 730

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
