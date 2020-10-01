package no.nav.security.mock.oauth2.testutils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import java.net.URL
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Date
import java.util.UUID
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import okhttp3.HttpUrl

object ClientAssertionType {
    const val JWT_BEARER = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
}

object SubjectTokenType {
    const val TOKEN_TYPE_JWT = "urn:ietf:params:oauth:token-type:jwt"
}

data class ParsedTokenResponse(
    val status: Int,
    val body: String
) {
    private val tokenResponse: OAuth2TokenResponse = jacksonObjectMapper().readValue(body)
    val tokenType = tokenResponse.tokenType
    val issuedTokenType = tokenResponse.issuedTokenType
    val refreshToken = tokenResponse.refreshToken
    val expiresIn = tokenResponse.expiresIn
    val scope = tokenResponse.scope
    val accessToken: SignedJWT? = tokenResponse.accessToken?.asJwt()
    val idToken: SignedJWT? = tokenResponse.idToken?.asJwt()
}

fun String.asJwt(): SignedJWT = SignedJWT.parse(this)

val SignedJWT.audience: List<String> get() = jwtClaimsSet.audience
val SignedJWT.issuer: String get() = jwtClaimsSet.issuer
val SignedJWT.subject: String get() = jwtClaimsSet.subject
val SignedJWT.claims: Map<String, Any> get() = jwtClaimsSet.claims

fun SignedJWT.verify(issuer: HttpUrl, jwkSetUri: HttpUrl): JWTClaimsSet {
    return DefaultJWTProcessor<SecurityContext?>()
        .apply {
            jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, RemoteJWKSet(jwkSetUri.toUrl()))
            jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(
                JWTClaimsSet.Builder()
                    .issuer(issuer.toString())
                    .build(),
                HashSet(
                    listOf("sub", "iss", "iat", "exp", "aud")
                )
            )
        }.process(this, null)
}

fun clientAssertion(
    clientId: String,
    audience: URL,
    rsaKey: RSAKey = generateRsaKey(),
    lifetime: Long = 119,
    issueTime: Instant = Instant.now()
): SignedJWT =
    JWTClaimsSet.Builder()
        .issuer(clientId)
        .subject(clientId)
        .audience(audience.toString())
        .issueTime(Date.from(issueTime))
        .expirationTime(Date.from(issueTime.plusSeconds(lifetime)))
        .notBeforeTime(Date.from(issueTime))
        .jwtID(UUID.randomUUID().toString())
        .build()
        .sign(rsaKey)


fun JWTClaimsSet.sign(rsaKey: RSAKey = generateRsaKey()): SignedJWT =
    SignedJWT(
        JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.keyID)
            .type(JOSEObjectType.JWT).build(),
        this
    ).apply {
        sign(RSASSASigner(rsaKey.toPrivateKey()))
    }

fun generateRsaKey(keyId: String = UUID.randomUUID().toString(), keySize: Int = 2048): RSAKey =
    KeyPairGenerator.getInstance("RSA")
        .apply { initialize(keySize) }.generateKeyPair()
        .let {
            RSAKey.Builder(it.public as RSAPublicKey)
                .privateKey(it.private as RSAPrivateKey)
                .keyID(keyId)
                .keyUse(KeyUse.SIGNATURE)
                .build()
        }
