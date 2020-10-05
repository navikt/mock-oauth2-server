package no.nav.security.mock.oauth2.extensions

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier
import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT
import com.nimbusds.oauth2.sdk.id.Issuer
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.Prompt
import java.time.Duration
import java.time.Instant
import java.util.HashSet
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.invalidRequest

fun AuthenticationRequest.isPrompt(): Boolean =
    this.prompt?.any {
        it == Prompt.Type.LOGIN || it == Prompt.Type.CONSENT || it == Prompt.Type.SELECT_ACCOUNT
    } ?: false

fun TokenRequest.grantType(): GrantType =
    this.authorizationGrant?.type
        ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "missing required parameter grant_type")

fun TokenRequest.authorizationCode(): AuthorizationCode =
    this.authorizationGrant
        ?.let { it as? AuthorizationCodeGrant }
        ?.authorizationCode
        ?: throw OAuth2Exception(OAuth2Error.INVALID_GRANT, "code cannot be null")

fun TokenRequest.clientIdAsString(): String =
    this.clientAuthentication?.clientID?.value ?: this.clientID?.value
        ?: throw OAuth2Exception(OAuth2Error.INVALID_CLIENT, "client_id cannot be null")

fun SignedJWT.expiresIn(): Int =
    Duration.between(Instant.now(), this.jwtClaimsSet.expirationTime.toInstant()).seconds.toInt()

fun SignedJWT.verifySignatureAndIssuer(issuer: Issuer, jwkSet: JWKSet): JWTClaimsSet {
    val jwtProcessor: ConfigurableJWTProcessor<SecurityContext?> = DefaultJWTProcessor()
    jwtProcessor.jwsTypeVerifier = DefaultJOSEObjectTypeVerifier(JOSEObjectType("JWT"))
    val keySelector: JWSKeySelector<SecurityContext?> = JWSVerificationKeySelector(
        JWSAlgorithm.RS256,
        ImmutableJWKSet(jwkSet)
    )
    jwtProcessor.jwsKeySelector = keySelector
    jwtProcessor.jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(
        JWTClaimsSet.Builder().issuer(issuer.toString()).build(),
        HashSet(listOf("sub", "iat", "exp", "aud"))
    )
    return try {
        jwtProcessor.process(this, null)
    } catch (e: Exception) {
        throw OAuth2Exception("invalid signed JWT.", e)
    }
}

fun ClientAuthentication.requirePrivateKeyJwt(requiredAudience: String): PrivateKeyJWT =
    (this as? PrivateKeyJWT)
        ?.let {
            when {
                it.clientAssertion.expiresIn() > 120 -> {
                    invalidRequest("invalid client_assertion: client_assertion expiry is too long( should be < 120s)")
                }
                !it.clientAssertion.jwtClaimsSet.audience.contains(requiredAudience) -> {
                    invalidRequest("invalid client_assertion: client_assertion must contain required audience '$requiredAudience'")
                }
                else -> it
            }
        } ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "request must contain a valid client_assertion.")
