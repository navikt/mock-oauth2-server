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
import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.clientIdAsString
import no.nav.security.mock.oauth2.extensions.issuerId
import okhttp3.HttpUrl
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

typealias TimeProvider = () -> Instant?

class OAuth2TokenProvider
    @JvmOverloads
    constructor(
        private val keyProvider: KeyProvider = KeyProvider(),
        private val timeProvider: TimeProvider,
    ) {
        val systemTime
            get() = timeProvider()

        @JvmOverloads
        constructor(
            keyProvider: KeyProvider = KeyProvider(),
            systemTime: Instant? = null,
        ) : this(keyProvider, { systemTime })

        @JvmOverloads
        fun publicJwkSet(issuerId: String = "default"): JWKSet {
            return JWKSet(keyProvider.signingKey(issuerId)).toPublicJWKSet()
        }

        fun getAlgorithm(): JWSAlgorithm {
            return keyProvider.algorithm()
        }

        fun idToken(
            tokenRequest: TokenRequest,
            issuerUrl: HttpUrl,
            oAuth2TokenCallback: OAuth2TokenCallback,
            nonce: String? = null,
        ) = defaultClaims(
            issuerUrl,
            oAuth2TokenCallback.subject(tokenRequest),
            listOf(tokenRequest.clientIdAsString()),
            nonce,
            oAuth2TokenCallback.addClaims(tokenRequest),
            oAuth2TokenCallback.tokenExpiry(),
        ).sign(issuerUrl.issuerId(), oAuth2TokenCallback.typeHeader(tokenRequest))

        fun accessToken(
            tokenRequest: TokenRequest,
            issuerUrl: HttpUrl,
            oAuth2TokenCallback: OAuth2TokenCallback,
            nonce: String? = null,
        ) = defaultClaims(
            issuerUrl,
            oAuth2TokenCallback.subject(tokenRequest),
            oAuth2TokenCallback.audience(tokenRequest),
            nonce,
            oAuth2TokenCallback.addClaims(tokenRequest),
            oAuth2TokenCallback.tokenExpiry(),
        ).sign(issuerUrl.issuerId(), oAuth2TokenCallback.typeHeader(tokenRequest))

        fun exchangeAccessToken(
            tokenRequest: TokenRequest,
            issuerUrl: HttpUrl,
            claimsSet: JWTClaimsSet,
            oAuth2TokenCallback: OAuth2TokenCallback,
        ) = systemTime.orNow().let { now ->
            JWTClaimsSet.Builder(claimsSet)
                .issuer(issuerUrl.toString())
                .expirationTime(Date.from(now.plusSeconds(oAuth2TokenCallback.tokenExpiry())))
                .notBeforeTime(Date.from(now))
                .issueTime(Date.from(now))
                .jwtID(UUID.randomUUID().toString())
                .audience(oAuth2TokenCallback.audience(tokenRequest))
                .addClaims(oAuth2TokenCallback.addClaims(tokenRequest))
                .build()
                .sign(issuerUrl.issuerId(), oAuth2TokenCallback.typeHeader(tokenRequest))
        }

        @JvmOverloads
        fun jwt(
            claims: Map<String, Any>,
            expiry: Duration = Duration.ofHours(1),
            issuerId: String = "default",
        ): SignedJWT =
            JWTClaimsSet.Builder().let { builder ->
                val now = systemTime.orNow()
                builder
                    .issueTime(Date.from(now))
                    .notBeforeTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(expiry.toSeconds())))
                builder.addClaims(claims)
                builder.build()
            }.sign(issuerId, JOSEObjectType.JWT.type)

        private fun JWTClaimsSet.sign(
            issuerId: String,
            type: String,
        ): SignedJWT {
            val key = keyProvider.signingKey(issuerId)
            val algorithm = keyProvider.algorithm()
            val keyType = keyProvider.keyType()
            val supported = KeyGenerator.isSupported(algorithm)

            return when {
                supported && keyType == KeyType.RSA.value -> {
                    SignedJWT(
                        jwsHeader(key.keyID, type, algorithm),
                        this,
                    ).apply {
                        sign(RSASSASigner(key.toRSAKey().toPrivateKey()))
                    }
                }
                supported && keyType == KeyType.EC.value -> {
                    SignedJWT(
                        jwsHeader(key.keyID, type, algorithm),
                        this,
                    ).apply {
                        sign(ECDSASigner(key.toECKey().toECPrivateKey()))
                    }
                }
                else -> {
                    throw OAuth2Exception("Unsupported algorithm: ${algorithm.name}")
                }
            }
        }

        private fun jwsHeader(
            keyId: String,
            type: String,
            algorithm: JWSAlgorithm,
        ): JWSHeader {
            return JWSHeader.Builder(algorithm)
                .keyID(keyId)
                .type(JOSEObjectType(type)).build()
        }

        private fun JWTClaimsSet.Builder.addClaims(claims: Map<String, Any> = emptyMap()) =
            apply {
                claims.forEach { this.claim(it.key, it.value) }
            }

        private fun defaultClaims(
            issuerUrl: HttpUrl,
            subject: String?,
            audience: List<String>,
            nonce: String?,
            additionalClaims: Map<String, Any>,
            expiry: Long,
        ) = JWTClaimsSet.Builder().let { builder ->
            val now = systemTime.orNow()
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

        private fun Instant?.orNow(): Instant = this ?: Instant.now()
    }
