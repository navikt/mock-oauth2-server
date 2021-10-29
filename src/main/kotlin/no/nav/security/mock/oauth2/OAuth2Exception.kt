package no.nav.security.mock.oauth2

import com.nimbusds.oauth2.sdk.ErrorObject
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.http.HTTPResponse

@Suppress("unused")
class OAuth2Exception(val errorObject: ErrorObject?, msg: String, throwable: Throwable?) :
    RuntimeException(msg, throwable) {
    constructor(msg: String) : this(null, msg, null)
    constructor(msg: String, throwable: Throwable?) : this(null, msg, throwable)
    constructor(errorObject: ErrorObject?, msg: String) : this(errorObject, msg, null)
}

fun missingParameter(name: String): Nothing = throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "missing required parameter $name")
fun invalidGrant(grantType: GrantType): Nothing = throw OAuth2Exception(OAuth2Error.INVALID_GRANT, "grant_type $grantType not supported.")
fun invalidRequest(message: String): Nothing = throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, message)
fun notFound(message: String): Nothing = throw OAuth2Exception(ErrorObject("not_found", "Resource not found", HTTPResponse.SC_NOT_FOUND), message)
