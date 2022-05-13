package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyType
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.id.Issuer
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.security.mock.oauth2.extensions.verifySignatureAndIssuer
import no.nav.security.mock.oauth2.testutils.toCertObject
import no.nav.security.mock.oauth2.token.KeyGenerator.Companion.ecAlgorithmFamily
import no.nav.security.mock.oauth2.token.KeyGenerator.Companion.rsaAlgorithmFamily
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant
import java.util.Date

class KeyGeneratorTest {

    @Test
    fun `verify RSA signing keys with the right algorithm is created`() {
        rsaAlgorithmFamily.forEachIndexed { index, jwsAlgorithm ->

            val generator = KeyGenerator(algorithm = jwsAlgorithm)
            generator.algorithm.toString() shouldBe jwsAlgorithm.name

            val keyId = "test$index"
            val keys = generator.generateKey(keyId)

            keys.keyID shouldBe keyId
            keys.keyType.toString() shouldBe KeyType.RSA.value
            keys.keyUse.toString() shouldBe "sig"
            keys.algorithm shouldBeIn rsaAlgorithmFamily
            keys.x509CertChain shouldBe null

            val issuer = Issuer("issuer$index")
            val jwt = jwtWith(issuer.value, keyId, JOSEObjectType.JWT.type, jwsAlgorithm)
            val jwkSet = JWKSet.parse("""{"keys": [$keys]}""".trimIndent())

            shouldNotThrow<Exception> {
                jwt.apply {
                    sign(RSASSASigner(keys.toRSAKey().toRSAPrivateKey()))
                }
                jwt.verifySignatureAndIssuer(issuer, jwkSet, jwsAlgorithm)
            }
        }
    }

    @Test
    fun `verify EC signing keys with the right algorithm is created`() {
        ecAlgorithmFamily.forEachIndexed { index, jwsAlgorithm ->

            val generator = KeyGenerator(algorithm = jwsAlgorithm)
            generator.algorithm.toString() shouldBe jwsAlgorithm.name

            val keyId = "test$index"
            val keys = generator.generateKey(keyId)

            keys.keyID shouldBe keyId
            keys.keyType.toString() shouldBe KeyType.EC.value
            keys.keyUse.toString() shouldBe "sig"
            keys.algorithm shouldBeIn ecAlgorithmFamily
            keys.x509CertChain shouldBe null

            val issuer = Issuer("issuer$index")
            val jwt = jwtWith(issuer.value, keyId, JOSEObjectType.JWT.type, jwsAlgorithm)
            val jwkSet = JWKSet.parse("""{"keys": [$keys]}""".trimIndent())

            shouldNotThrow<Exception> {
                jwt.apply {
                    sign(ECDSASigner(keys.toECKey().toECPrivateKey()))
                }
                jwt.verifySignatureAndIssuer(issuer, jwkSet, jwsAlgorithm)
            }
        }
    }

    @Test
    fun `verify RSA signing keys with x5c certificate chain and with the right algorithm is created`() {
        rsaAlgorithmFamily.forEachIndexed { index, jwsAlgorithm ->

            val generator = KeyGenerator(algorithm = jwsAlgorithm)
            generator.algorithm.toString() shouldBe jwsAlgorithm.name

            val keyId = "test$index"
            val certCfg = CertificateConfig(x509CertChain = true)
            val keys = generator.generateKey(keyId, certCfg)

            keys.keyID shouldBe keyId
            keys.keyType.toString() shouldBe KeyType.RSA.value
            keys.keyUse.toString() shouldBe "sig"
            keys.x509CertChain shouldNotBe null
            keys.x509CertChain.toCertObject().asClue {
                assertDoesNotThrow {
                    it.checkValidity()
                }
                it.subjectX500Principal.name shouldBe "CN=mock-oauth2-server"
                it.sigAlgName.toString() shouldBe certCfg.findSignature(jwsAlgorithm)
            }
            keys.algorithm shouldBeIn rsaAlgorithmFamily

            val issuer = Issuer("issuer$index")
            val jwt = jwtWith(issuer.value, keyId, JOSEObjectType.JWT.type, jwsAlgorithm)
            val jwkSet = JWKSet.parse("""{"keys": [$keys]}""".trimIndent())

            shouldNotThrow<Exception> {
                jwt.apply {
                    sign(RSASSASigner(keys.toRSAKey().toRSAPrivateKey()))
                }
                jwt.verifySignatureAndIssuer(issuer, jwkSet, jwsAlgorithm)
            }
        }
    }

    @Test
    fun `verify EC signing keys with x5c certificate chain and with the right algorithm is created`() {
        ecAlgorithmFamily.forEachIndexed { index, jwsAlgorithm ->

            val generator = KeyGenerator(algorithm = jwsAlgorithm)
            generator.algorithm.toString() shouldBe jwsAlgorithm.name

            val keyId = "test$index"
            val certCfg = CertificateConfig(x509CertChain = true)
            val keys = generator.generateKey(keyId, certCfg)

            keys.keyID shouldBe keyId
            keys.keyType.toString() shouldBe KeyType.EC.value
            keys.keyUse.toString() shouldBe "sig"
            keys.x509CertChain shouldNotBe null
            keys.x509CertChain.toCertObject().asClue {
                assertDoesNotThrow {
                    it.checkValidity()
                }
                it.subjectX500Principal.name shouldBe "CN=mock-oauth2-server"
                it.sigAlgName.toString() shouldBe certCfg.findSignature(jwsAlgorithm)
            }
            keys.algorithm shouldBeIn ecAlgorithmFamily

            val issuer = Issuer("issuer$index")
            val jwt = jwtWith(issuer.value, keyId, JOSEObjectType.JWT.type, jwsAlgorithm)
            val jwkSet = JWKSet.parse("""{"keys": [$keys]}""".trimIndent())

            shouldNotThrow<Exception> {
                jwt.apply {
                    sign(ECDSASigner(keys.toECKey().toECPrivateKey()))
                }
                jwt.verifySignatureAndIssuer(issuer, jwkSet, jwsAlgorithm)
            }
        }
    }

    private fun jwtWith(issuer: String, keyId: String, type: String, algorithm: JWSAlgorithm): SignedJWT =
        SignedJWT(
            JWSHeader.Builder(algorithm)
                .keyID(keyId)
                .type(JOSEObjectType(type)).build(),
            JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("test")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(20)))
                .build()
        )
}
