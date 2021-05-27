package no.nav.security.mock.oauth2.testutils

import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509ExtensionUtils
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.IOException
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import java.util.Date

class Certificates {

    fun KeyPair.toX509v3Certificate(cn: String, expiry: Duration = Duration.ofDays(365)): X509Certificate {
        val now = Instant.now()
        val x500Name = X500Name("CN=$cn")
        val contentSigner: ContentSigner = JcaContentSignerBuilder("SHA256withRSA").build(this.private)
        val certificateHolder = JcaX509v3CertificateBuilder(
            x500Name,
            BigInteger.valueOf(now.toEpochMilli()),
            Date.from(now),
            Date.from(now.plus(expiry)),
            x500Name,
            this.public
        ).addExtensions(cn, this.public).build(contentSigner)
        return JcaX509CertificateConverter().setProvider(BouncyCastleProvider()).getCertificate(certificateHolder)
    }

    private fun X509v3CertificateBuilder.addExtensions(cn: String, publicKey: PublicKey) = apply {
        addExtension(Extension.subjectKeyIdentifier, false, publicKey.createSubjectKeyId())
            .addExtension(Extension.authorityKeyIdentifier, false, publicKey.createAuthorityKeyId())
            .addExtension(Extension.basicConstraints, true, BasicConstraints(true))
            .addExtension(Extension.subjectAlternativeName, false, GeneralNames(GeneralName(GeneralName.dNSName, cn)))
            .addExtension(Extension.keyUsage, false, KeyUsage(KeyUsage.digitalSignature))
            .addExtension(Extension.extendedKeyUsage, false, ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth))
    }

    fun main() {
        val keyPair = KeyPairGenerator.getInstance("RSA")
            .apply { initialize(2048) }.generateKeyPair()

        println(x509CertificateToPem(keyPair.toX509v3Certificate("yolo")))
    }

    @Throws(IOException::class)
    fun x509CertificateToPem(cert: X509Certificate?): String? {
        val writer = StringWriter()
        val pemWriter = JcaPEMWriter(writer)
        pemWriter.writeObject(cert)
        pemWriter.flush()
        pemWriter.close()
        return writer.toString()
    }

    private fun PublicKey.createSubjectKeyId(): SubjectKeyIdentifier =
        X509ExtensionUtils(digestCalculator()).createSubjectKeyIdentifier(SubjectPublicKeyInfo.getInstance(encoded))

    private fun PublicKey.createAuthorityKeyId(): AuthorityKeyIdentifier =
        X509ExtensionUtils(digestCalculator()).createAuthorityKeyIdentifier(SubjectPublicKeyInfo.getInstance(encoded))

    private fun digestCalculator() = BcDigestCalculatorProvider().get(AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1))
}
