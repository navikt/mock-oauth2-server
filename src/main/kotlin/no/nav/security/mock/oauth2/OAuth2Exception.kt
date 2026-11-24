package no.nav.security.mock.oauth2

import com.nimbusds.oauth2.sdk.ErrorObject
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.http.HTTPResponse

@Suppress("unused")
class OAuth2Exception(
    val errorObject: ErrorObject?,
    msg: String,
    throwable: Throwable?,
) : RuntimeException(msg, throwable) {
    constructor(msg: String) : this(null, msg, null)
    constructor(msg: String, throwable: Throwable?) : this(null, msg, throwable)
    constructor(errorObject: ErrorObject?, msg: String) : this(errorObject, msg, null)
}

fun missingParameter(name: String): Nothing =
    "missing required parameter $name".let {
        throw OAuth2Exception(OAuth2Error.INVALID_REQUEST.setDescription(it), it)
    }

fun invalidGrant(grantType: GrantType): Nothing =
    "grant_type $grantType not supported.".let {
        throw OAuth2Exception(OAuth2Error.INVALID_GRANT.setDescription(it), it)
    }

fun unsupportedGrantType(grantType: GrantType): Nothing =
    "grant_type $grantType not supported.".let {
        throw OAuth2Exception(OAuth2Error.UNSUPPORTED_GRANT_TYPE.setDescription(it), it)
    }

fun invalidRequest(message: String): Nothing =
    message.let {
        throw OAuth2Exception(OAuth2Error.INVALID_REQUEST.setDescription(message), message)
    }

internal fun unsupportedResponseType(message: String): Nothing = throw OAuth2Exception(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE.setDescription(message), message)

internal fun requestNotSupported(): Nothing = throw OAuth2Exception(OAuth2Error.REQUEST_NOT_SUPPORTED, "the request parameter is not supported")

internal fun requestUriNotSupported(): Nothing = throw OAuth2Exception(OAuth2Error.REQUEST_URI_NOT_SUPPORTED, "the request_uri parameter is not supported")

internal fun invalidClient(message: String): Nothing = throw OAuth2Exception(OAuth2Error.INVALID_CLIENT.setDescription(message), message)

fun notFound(message: String): Nothing = throw OAuth2Exception(ErrorObject("not_found", "Resource not found", HTTPResponse.SC_NOT_FOUND), message)
