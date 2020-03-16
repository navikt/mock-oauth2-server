package no.nav.security.mock.oauth2.extensions

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.Prompt
import no.nav.security.mock.oauth2.OAuth2Exception
import java.time.Duration
import java.time.Instant

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
