package no.nav.security.mock.extensions

import OAuth2Exception
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.TokenRequest

fun TokenRequest.grantType(): GrantType =
    this.authorizationGrant?.type ?: throw OAuth2Exception("missing required parameter grant_type")

fun TokenRequest.authorizationCodeResponse(): AuthorizationCode =
    this.authorizationGrant
        ?.let { it as? AuthorizationCodeGrant }
        ?.authorizationCode
        ?: throw OAuth2Exception(OAuth2Error.INVALID_GRANT, "code cannot be null")

fun TokenRequest.clientIdAsString(): String =
    this.clientAuthentication?.clientID?.value ?: this.clientID?.value
    ?: throw OAuth2Exception(OAuth2Error.INVALID_CLIENT, "client_id cannot be null")