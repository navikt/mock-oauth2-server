package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JWSAlgorithm
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.token.KeyGenerator.Companion.ecAlgorithmFamily
import no.nav.security.mock.oauth2.token.KeyGenerator.Companion.rsaAlgorithmFamily

const val MOCK_OAUTH2_SERVER_NAME = "mock-oauth2-server"
const val DAYS_TO_EXPIRE = 730
const val PREFIX_SHA = "SHA"
const val SUFFIX_RSA = "withRSA"
const val SUFFIX_ECDSA = "withECDSA"

class CertificateConfig(
    val x509CertChain: Boolean = false,
    val expiresInDays: Int = DAYS_TO_EXPIRE,
    val cn: String = MOCK_OAUTH2_SERVER_NAME
) {
    fun findSignature(algo: JWSAlgorithm): String {
        return when {
            rsaAlgorithmFamily.contains(algo) -> {
                val encryption = algo.name.filter { it.isDigit() }
                "$PREFIX_SHA${encryption}$SUFFIX_RSA"
            }
            ecAlgorithmFamily.contains(algo) -> {
                val encryption = algo.name.filter { it.isDigit() }
                "$PREFIX_SHA${encryption}$SUFFIX_ECDSA"
            }
            else -> {
                throw OAuth2Exception("unsupported certificate algorithm: $algo")
            }
        }
    }
}
