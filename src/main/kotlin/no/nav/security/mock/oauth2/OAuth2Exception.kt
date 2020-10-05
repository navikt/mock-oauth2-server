package no.nav.security.mock.oauth2

import com.nimbusds.oauth2.sdk.ErrorObject
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.OAuth2Error

class OAuth2Exception(val errorObject: ErrorObject?, msg: String, throwable: Throwable?) :
    RuntimeException(msg, throwable) {
    constructor(msg: String) : this(null, msg, null)
    constructor(msg: String, throwable: Throwable?) : this(null, msg, throwable)
    constructor(errorObject: ErrorObject?, msg: String) : this(errorObject, msg, null)
}

fun badRequest(message: String) = OAuth2Exception(OAuth2Error.INVALID_REQUEST, message)
fun missingParameter(name: String) = OAuth2Exception(OAuth2Error.INVALID_REQUEST, "missing required parameter $name")
fun invalidGrant(grantType: GrantType) = OAuth2Exception(OAuth2Error.INVALID_GRANT, "grant_type $grantType not supported.")

fun invalidRequest(message: String): Nothing = throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, message)
