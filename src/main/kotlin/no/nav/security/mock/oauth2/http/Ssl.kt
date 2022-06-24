package no.nav.security.mock.oauth2.http

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
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine

class Ssl @JvmOverloads constructor(
    val sslKeystore: SslKeystore = SslKeystore()
) {
    fun sslEngine(): SSLEngine = sslContext().createSSLEngine().apply {
        useClientMode = false
        needClientAuth = false
    }

    fun sslContext(): SSLContext {
        val keyManager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(sslKeystore.keyStore, sslKeystore.keyPassword.toCharArray())
        }
        return SSLContext.getInstance("TLS").apply {
            init(keyManager.keyManagers, null, null)
        }
    }
}

class SslKeystore @JvmOverloads constructor(
    val keyPassword: String = "",
    val keyStore: KeyStore = generate("localhost", keyPassword)
) {
    @JvmOverloads constructor(
        keyPassword: String,
        keystoreFile: File,
        keystoreType: KeyStoreType = KeyStoreType.PKCS12,
        keystorePassword: String = ""
    ) : this(keyPassword, keyStore(keystoreFile, keystoreType, keystorePassword))

    enum class KeyStoreType {
        PKCS12,
        JKS
    }

    companion object {
        private const val CERT_SIGNATURE_ALG = "SHA256withRSA"
        private const val KEY_ALG = "RSA"
        private const val KEY_SIZE = 2048

        fun generate(hostname: String, keyPassword: String): KeyStore {
            val keyPair = KeyPairGenerator.getInstance(KEY_ALG).apply { initialize(KEY_SIZE) }.generateKeyPair()
            val cert = keyPair.toX509Certificate(hostname)
            return KeyStore.getInstance(KeyStoreType.PKCS12.name).apply {
                this.load(null)
                this.setKeyEntry(hostname, keyPair.private, keyPassword.toCharArray(), arrayOf(cert))
            }
        }

        private fun keyStore(
            keystoreFile: File,
            keystoreType: KeyStoreType = KeyStoreType.PKCS12,
            keystorePassword: String = ""
        ) = KeyStore.getInstance(keystoreType.name).apply {
            keystoreFile.inputStream().use {
                load(it, keystorePassword.toCharArray())
            }
        }

        private fun KeyPair.toX509Certificate(cn: String, expiry: Duration = Duration.ofDays(365)): X509Certificate {
            val now = Instant.now()
            val x500Name = X500Name("CN=$cn")
            val contentSigner: ContentSigner = JcaContentSignerBuilder(CERT_SIGNATURE_ALG).build(this.private)
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

        private fun PublicKey.createSubjectKeyId(): SubjectKeyIdentifier =
            X509ExtensionUtils(digestCalculator()).createSubjectKeyIdentifier(SubjectPublicKeyInfo.getInstance(encoded))

        private fun PublicKey.createAuthorityKeyId(): AuthorityKeyIdentifier =
            X509ExtensionUtils(digestCalculator()).createAuthorityKeyIdentifier(SubjectPublicKeyInfo.getInstance(encoded))

        private fun digestCalculator() = BcDigestCalculatorProvider().get(AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1))
    }
}
