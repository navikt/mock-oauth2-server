package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.oauth2.extensions.clientIdAsString
import no.nav.security.mock.oauth2.extensions.issuerId
import okhttp3.HttpUrl
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

class OAuth2TokenProvider @JvmOverloads constructor(
    private val keyProvider: KeyProvider = KeyProvider()
) {
    @JvmOverloads
    fun publicJwkSet(issuerId: String = "default"): JWKSet {
        return JWKSet(keyProvider.signingKey(issuerId)).toPublicJWKSet()
    }

    fun idToken(
        tokenRequest: TokenRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback,
        nonce: String? = null
    ) = defaultClaims(
        issuerUrl,
        oAuth2TokenCallback.subject(tokenRequest),
        listOf(tokenRequest.clientIdAsString()),
        nonce,
        oAuth2TokenCallback.addClaims(tokenRequest),
        oAuth2TokenCallback.tokenExpiry()
    ).sign(issuerUrl.issuerId(), oAuth2TokenCallback.typeHeader(tokenRequest), oAuth2TokenCallback.algorithm())

    fun accessToken(
        tokenRequest: TokenRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback,
        nonce: String? = null
    ) = defaultClaims(
        issuerUrl,
        oAuth2TokenCallback.subject(tokenRequest),
        oAuth2TokenCallback.audience(tokenRequest),
        nonce,
        oAuth2TokenCallback.addClaims(tokenRequest),
        oAuth2TokenCallback.tokenExpiry()
    ).sign(issuerUrl.issuerId(), oAuth2TokenCallback.typeHeader(tokenRequest), oAuth2TokenCallback.algorithm())

    fun exchangeAccessToken(
        tokenRequest: TokenRequest,
        issuerUrl: HttpUrl,
        claimsSet: JWTClaimsSet,
        oAuth2TokenCallback: OAuth2TokenCallback
    ) = Instant.now().let { now ->
        JWTClaimsSet.Builder(claimsSet)
            .issuer(issuerUrl.toString())
            .expirationTime(Date.from(now.plusSeconds(oAuth2TokenCallback.tokenExpiry())))
            .notBeforeTime(Date.from(now))
            .issueTime(Date.from(now))
            .jwtID(UUID.randomUUID().toString())
            .audience(oAuth2TokenCallback.audience(tokenRequest))
            .addClaims(oAuth2TokenCallback.addClaims(tokenRequest))
            .build()
            .sign(issuerUrl.issuerId(), oAuth2TokenCallback.typeHeader(tokenRequest), oAuth2TokenCallback.algorithm())
    }

    @JvmOverloads
    fun jwt(claims: Map<String, Any>, expiry: Duration = Duration.ofHours(1), issuerId: String = "default"): SignedJWT =
        JWTClaimsSet.Builder().let { builder ->
            val now = Instant.now()
            builder
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(expiry.toSeconds())))
            builder.addClaims(claims)
            builder.build()
        }.sign(issuerId, JOSEObjectType.JWT.type, JWSAlgorithm.RS256.name)

    private fun JWTClaimsSet.sign(issuerId: String, type: String, algorithm: String): SignedJWT {
        val algo = JWSAlgorithm.parse(algorithm)
        when {
            algorithm == "RS256" -> {
                val key = keyProvider.signingKey(issuerId)
                return SignedJWT(
                    jwsHeader(key.keyID, type, algo),
                    this
                ).apply {
                    sign(RSASSASigner(key.toRSAKey().toPrivateKey()))
                }
            }
            algorithm.startsWith("RS") -> {
                keyProvider.regenerate(algorithm)
                val key = keyProvider.signingKey(issuerId)
                return SignedJWT(
                    jwsHeader(key.keyID, type, algo),
                    this
                ).apply {
                    sign(RSASSASigner(key.toRSAKey().toPrivateKey()))
                }
            }
            else -> {
                keyProvider.regenerate(algorithm)
                val key = keyProvider.signingKey(issuerId)
                return SignedJWT(
                    jwsHeader(key.keyID, type, algo),
                    this
                ).apply {
                    sign(ECDSASigner(key.toECKey().toECPrivateKey()))
                }
            }
        }
    }

    private fun jwsHeader(keyId: String, type: String, algorithm: JWSAlgorithm): JWSHeader {
        return JWSHeader.Builder(algorithm)
            .keyID(keyId)
            .type(JOSEObjectType(type)).build()
    }

    private fun JWTClaimsSet.Builder.addClaims(claims: Map<String, Any> = emptyMap()) = apply {
        claims.forEach { this.claim(it.key, it.value) }
    }

    private fun defaultClaims(
        issuerUrl: HttpUrl,
        subject: String?,
        audience: List<String>,
        nonce: String?,
        additionalClaims: Map<String, Any>,
        expiry: Long
    ) = JWTClaimsSet.Builder().let { builder ->
        val now = Instant.now()
        builder.subject(subject)
            .audience(audience)
            .issuer(issuerUrl.toString())
            .issueTime(Date.from(now))
            .notBeforeTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(expiry)))
            .jwtID(UUID.randomUUID().toString())

        nonce?.also { builder.claim("nonce", it) }
        builder.addClaims(additionalClaims)
        builder.build()
    }
}
