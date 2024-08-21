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
import com.nimbusds.oauth2.sdk.AuthorizationGrant
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.oauth2.sdk.id.Issuer
import com.nimbusds.oauth2.sdk.pkce.CodeChallenge
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.OIDCScopeValue
import com.nimbusds.openid.connect.sdk.Prompt
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.invalidRequest
import java.time.Duration
import java.time.Instant

private val log = KotlinLogging.logger { }

fun AuthenticationRequest.isPrompt(): Boolean =
    this.prompt?.any {
        it == Prompt.Type.LOGIN || it == Prompt.Type.CONSENT || it == Prompt.Type.SELECT_ACCOUNT
    } ?: false

fun AuthenticationRequest.verifyPkce(tokenRequest: TokenRequest) {
    val verifier: CodeVerifier? = tokenRequest.grant(AuthorizationCodeGrant::class.java).codeVerifier
    if (verifier != null) {
        if (CodeChallenge.compute(this.codeChallengeMethod, verifier) != this.codeChallenge) {
            val msg = "invalid_pkce: code_verifier does not compute to code_challenge from request"
            throw OAuth2Exception(OAuth2Error.INVALID_GRANT.setDescription(msg), msg)
        }
    } else {
        log.debug("no code_verifier found in token request, nothing to compare")
    }
}

fun TokenRequest.grantType(): GrantType =
    this.authorizationGrant?.type
        ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "missing required parameter grant_type")

fun TokenRequest.scopesWithoutOidcScopes() =
    scope?.toStringList()?.filterNot { value ->
        OIDCScopeValue.values().map { it.toString() }.contains(value)
    } ?: emptyList()

fun TokenRequest.tokenExchangeGrantOrNull() = authorizationGrant as? com.nimbusds.oauth2.sdk.tokenexchange.TokenExchangeGrant

fun TokenRequest.authorizationCode(): AuthorizationCode =
    this.authorizationGrant
        ?.let { it as? AuthorizationCodeGrant }
        ?.authorizationCode
        ?: throw OAuth2Exception(OAuth2Error.INVALID_GRANT, "code cannot be null")

inline fun <reified T : AuthorizationGrant> TokenRequest.grant(type: Class<T>): T =
    this.authorizationGrant as? T
        ?: throw OAuth2Exception(OAuth2Error.INVALID_GRANT, "expected grant of type $type")

fun TokenRequest.clientIdAsString(): String =
    this.clientAuthentication?.clientID?.value ?: this.clientID?.value
        ?: throw OAuth2Exception(OAuth2Error.INVALID_CLIENT, "client_id cannot be null")

fun SignedJWT.expiresIn(): Int = Duration.between(Instant.now(), this.jwtClaimsSet.expirationTime.toInstant()).seconds.toInt()

fun SignedJWT.verifySignatureAndIssuer(
    issuer: Issuer,
    jwkSet: JWKSet,
    jwsAlgorithm: JWSAlgorithm = JWSAlgorithm.RS256,
): JWTClaimsSet {
    val jwtProcessor: ConfigurableJWTProcessor<SecurityContext?> = DefaultJWTProcessor()
    jwtProcessor.jwsTypeVerifier = DefaultJOSEObjectTypeVerifier(JOSEObjectType("JWT"))
    val keySelector: JWSKeySelector<SecurityContext?> =
        JWSVerificationKeySelector(
            jwsAlgorithm,
            ImmutableJWKSet(jwkSet),
        )
    jwtProcessor.jwsKeySelector = keySelector
    jwtProcessor.jwtClaimsSetVerifier =
        DefaultJWTClaimsVerifier(
            JWTClaimsSet.Builder().issuer(issuer.toString()).build(),
            HashSet(listOf("sub", "iat", "exp")),
        )
    return jwtProcessor.process(this, null)
}

fun HTTPRequest.clientAuthentication() =
    ClientAuthentication.parse(this)
        ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "request must contain some form of ClientAuthentication.")

/**
 * TODO: We currently accept multiple audiences for backwards compatibility as updates to RFC7523 are pending.
 * Relevant excerpts:
 *  > The JWT MUST contain an aud (audience) claim containing the issuer identifier [RFC8414] of the authorization server as its sole value.
 *
 *  > Unlike the aud value specified in [RFC7523], there MUST be no value other than the issuer identifier of the intended authorization server used as the audience of the JWT;
 *  > this includes that the token endpoint URL of the authorization server MUST NOT be used as an audience value.
 *
 *  > The authorization server MUST reject any JWT that does not contain its issuer identifier as its sole audience value.
 *
 * See [RFC7523bis](https://datatracker.ietf.org/doc/draft-ietf-oauth-rfc7523bis) for details.
 * Compliance with the RFC will require breaking changes.
 **/
fun ClientAuthentication.requirePrivateKeyJwt(
    requiredAudience: String,
    maxLifetimeSeconds: Long,
    additionalAcceptedAudience: String? = null,
): PrivateKeyJWT =
    (this as? PrivateKeyJWT)
        ?.let {
            val acceptedAudiences = setOf(requiredAudience, additionalAcceptedAudience).filterNotNull().toSet()
            when {
                it.clientAssertion.expiresIn() > maxLifetimeSeconds -> {
                    invalidRequest("invalid client_assertion: expiry must be less than $maxLifetimeSeconds seconds")
                }

                it.clientAssertion.jwtClaimsSet.issuer != it.clientID.value -> {
                    invalidRequest("invalid client_assertion: issuer must match client_id '${it.clientID.value}'")
                }

                it.clientAssertion.jwtClaimsSet.subject != it.clientID.value -> {
                    invalidRequest("invalid client_assertion: subject must match client_id '${it.clientID.value}'")
                }

                it.clientAssertion.jwtClaimsSet.audience
                    .isEmpty() -> {
                    invalidRequest("invalid client_assertion: audience cannot be empty")
                }

                it.clientAssertion.jwtClaimsSet.audience.size > 1 -> {
                    invalidRequest("invalid client_assertion: audience must not contain more than one element")
                }

                // audience is a List<String>; it should contain exactly one element as per previous checks
                it.clientAssertion.jwtClaimsSet.audience
                    .first() !in acceptedAudiences -> {
                    invalidRequest("invalid client_assertion: audience should be $requiredAudience")
                }

                // all checks passed
                else -> it
            }
        } ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "request must contain a valid client_assertion.")
