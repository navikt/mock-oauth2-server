package no.nav.security.mock.oauth2.token

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
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import java.util.Date

object CertificateGenerator {

    fun make(
        keyPair: KeyPair,
        signatureAlgorithm: String,
        commonName: String,
        daysToExpire: Int
    ): X509Certificate? {
        val now = Instant.now()
        val notBefore = Date.from(now)
        val notAfter = Date.from(now.plus(Duration.ofDays(daysToExpire.toLong())))
        val contentSigner: ContentSigner = JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.private)
        val x500Name = X500Name("CN=$commonName")
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
}
