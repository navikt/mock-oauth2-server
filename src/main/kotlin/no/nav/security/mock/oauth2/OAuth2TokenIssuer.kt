package no.nav.security.mock.oauth2

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse
import mu.KotlinLogging
import no.nav.security.mock.callback.JwtCallback
import no.nav.security.mock.extensions.authorizationCodeResponse
import no.nav.security.mock.extensions.clientIdAsString
import okhttp3.HttpUrl
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

private val log = KotlinLogging.logger {}

open class OAuth2TokenIssuer {
    private val jwtTokenProvider: JwtTokenProvider = JwtTokenProvider()
    private val codeToAuthRequestCache: MutableMap<AuthorizationCode, AuthenticationRequest> = HashMap()

    fun jwks(): JWKSet = jwtTokenProvider.publicJwkSet()

    fun authorizationCodeResponse(authenticationRequest: AuthenticationRequest): AuthenticationSuccessResponse {
        val code = AuthorizationCode()
        codeToAuthRequestCache[code] = authenticationRequest
        return AuthenticationSuccessResponse(
            authenticationRequest.redirectionURI,
            code,
            null,
            null,
            authenticationRequest.state,
            null,
            authenticationRequest.responseMode
        )
    }

    fun authorizationCodeTokenResponse(
        issuerUrl: HttpUrl,
        tokenRequest: TokenRequest,
        jwtCallback: JwtCallback
    ): OAuth2TokenResponse {
        val authenticationRequest: AuthenticationRequest =
            codeToAuthRequestCache[tokenRequest.authorizationCodeResponse()]
            ?: throw OAuth2Exception(
                OAuth2Error.INVALID_GRANT,
                "code '${tokenRequest.authorizationCodeResponse()}' is expired or incorrect"
            )
        val clientId: String = tokenRequest.clientIdAsString()
        val scope: String = tokenRequest.scope.toString()
        val subject: String = jwtCallback.subject(tokenRequest)
        val nonce: String? = authenticationRequest.nonce?.value
        val additionalClaims: Map<String, Any> = jwtCallback.addClaims(tokenRequest)

        val idToken: SignedJWT = createJWTClaimsSetBuilder(
            issuerUrl = issuerUrl,
            subject = subject,
            audience = clientId,
            nonce = nonce,
            additionalClaims = additionalClaims
        ).let {
            jwtTokenProvider.createSignedJWT(it.build())
        }
        val accessToken: SignedJWT = createJWTClaimsSetBuilder(
            issuerUrl = issuerUrl,
            subject = subject,
            audience = "todo",
            nonce = nonce,
            additionalClaims = additionalClaims
        ).let {
            jwtTokenProvider.createSignedJWT(it.build())
        }

        return OAuth2TokenResponse(
            tokenType = "Bearer",
            idToken = idToken.serialize(),
            accessToken = accessToken.serialize(),
            refreshToken = UUID.randomUUID().toString(),
            expiresIn = expiresIn(idToken),
            scope = scope
        )
    }

    private fun expiresIn(token: SignedJWT): Long =
        Duration.between(Instant.now(), token.jwtClaimsSet.expirationTime.toInstant()).seconds

    private fun createJWTClaimsSetBuilder(
        issuerUrl: HttpUrl,
        subject: String,
        audience: String,
        nonce: String?,
        additionalClaims: Map<String, Any>
    ): JWTClaimsSet.Builder {
        val now = Instant.now()
        val jwtClaimsSetBuilder = JWTClaimsSet.Builder()
            .subject(subject)
            .audience(audience)
            .issuer(issuerUrl.toString())
            .issueTime(Date.from(now))
            .notBeforeTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(3600)))
            .jwtID(UUID.randomUUID().toString())

        if (nonce != null) {
            jwtClaimsSetBuilder.claim("nonce", nonce)
        }

        additionalClaims.forEach {
            jwtClaimsSetBuilder.claim(it.key, it.value)
        }
        return jwtClaimsSetBuilder
    }
}

internal class JwtTokenProvider {
    private val jwkSet: JWKSet
    private val rsaKey: RSAKey
    fun publicJwkSet(): JWKSet {
        return jwkSet.toPublicJWKSet()
    }

    fun createSignedJWT(claimsSet: JWTClaimsSet): SignedJWT {
            val header = JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.keyID)
                .type(JOSEObjectType.JWT)
            val signedJWT = SignedJWT(header.build(), claimsSet)
            val signer = RSASSASigner(rsaKey.toPrivateKey())
            signedJWT.sign(signer)
            return signedJWT
    }

    companion object {
        private const val DEFAULT_KEYID = "mock-oauth2-server-key"
        private fun generateJWKSet(keyId: String): JWKSet {
            return JWKSet(createJWK(keyId, generateKeyPair()))
        }

        private fun generateKeyPair(): KeyPair {
            return try {
                val gen = KeyPairGenerator.getInstance("RSA")
                gen.initialize(2048)
                gen.generateKeyPair()
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }
        }

        private fun createJWK(keyID: String, keyPair: KeyPair): RSAKey {
            return RSAKey.Builder(keyPair.public as RSAPublicKey)
                .privateKey(keyPair.private as RSAPrivateKey)
                .keyID(keyID)
                .build()
        }
    }

    init {
        jwkSet = generateJWKSet(DEFAULT_KEYID)
        rsaKey = jwkSet.getKeyByKeyId(DEFAULT_KEYID) as RSAKey
    }
}