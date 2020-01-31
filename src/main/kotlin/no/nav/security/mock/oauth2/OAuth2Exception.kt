package no.nav.security.mock.oauth2

import com.nimbusds.oauth2.sdk.ErrorObject

class OAuth2Exception(val errorObject: ErrorObject?, msg: String, throwable: Throwable?) :
    RuntimeException(msg, throwable) {
    constructor(msg: String) : this(null, msg, null)
    constructor(msg: String, throwable: Throwable?) : this(null, msg, throwable)
    constructor(errorObject: ErrorObject?, msg: String) : this(errorObject, msg, null)
}