package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.TokenRequest
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Date
import java.util.UUID
import no.nav.security.mock.oauth2.extensions.clientIdAsString
import okhttp3.HttpUrl

class OAuth2TokenProvider {
    private val jwkSet: JWKSet = generateJWKSet(DEFAULT_KEYID)
    private val rsaKey: RSAKey = jwkSet.getKeyByKeyId(DEFAULT_KEYID) as RSAKey

    fun publicJwkSet(): JWKSet {
        return jwkSet.toPublicJWKSet()
    }

    fun idToken(
        tokenRequest: TokenRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback,
        nonce: String? = null
    ) = createSignedJWT(
        defaultClaims(
            issuerUrl,
            oAuth2TokenCallback.subject(tokenRequest),
            listOf(tokenRequest.clientIdAsString()),
            nonce,
            oAuth2TokenCallback.addClaims(tokenRequest),
            oAuth2TokenCallback.tokenExpiry()
        )
    )

    fun accessToken(
        tokenRequest: TokenRequest,
        issuerUrl: HttpUrl,
        oAuth2TokenCallback: OAuth2TokenCallback,
        nonce: String? = null
    ) = createSignedJWT(
        defaultClaims(
            issuerUrl,
            oAuth2TokenCallback.subject(tokenRequest),
            oAuth2TokenCallback.audience(tokenRequest),
            nonce,
            oAuth2TokenCallback.addClaims(tokenRequest),
            oAuth2TokenCallback.tokenExpiry()
        )
    )

    fun exchangeAccessToken(
        tokenRequest: TokenRequest,
        issuerUrl: HttpUrl,
        claimsSet: JWTClaimsSet,
        oAuth2TokenCallback: OAuth2TokenCallback
    ) = Instant.now().let { now ->
        createSignedJWT(
            JWTClaimsSet.Builder(claimsSet)
                .issuer(oAuth2TokenCallback.issuerUrl(tokenRequest)?:issuerUrl.toString())
                .expirationTime(Date.from(now.plusSeconds(oAuth2TokenCallback.tokenExpiry())))
                .notBeforeTime(Date.from(now))
                .issueTime(Date.from(now))
                .jwtID(UUID.randomUUID().toString())
                .audience(oAuth2TokenCallback.audience(tokenRequest))
                .build()
        )
    }

    private fun createSignedJWT(claimsSet: JWTClaimsSet): SignedJWT {
        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.keyID)
            .type(JOSEObjectType.JWT)
        val signedJWT = SignedJWT(header.build(), claimsSet)
        val signer = RSASSASigner(rsaKey.toPrivateKey())
        signedJWT.sign(signer)
        return signedJWT
    }

    private fun defaultClaims(
        issuerUrl: HttpUrl,
        subject: String,
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

        additionalClaims.forEach { builder.claim(it.key, it.value) }
        builder.build()
    }

    companion object {
        private const val DEFAULT_KEYID = "mock-oauth2-server-key"
        private fun generateJWKSet(keyId: String) =
            JWKSet(createRSAKey(keyId, generateKeyPair()))

        private fun generateKeyPair(): KeyPair =
            KeyPairGenerator.getInstance("RSA").let {
                it.initialize(2048)
                it.generateKeyPair()
            }

        private fun createRSAKey(keyID: String, keyPair: KeyPair) =
            RSAKey.Builder(keyPair.public as RSAPublicKey)
                .privateKey(keyPair.private as RSAPrivateKey)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(keyID)
                .build()
    }
}
