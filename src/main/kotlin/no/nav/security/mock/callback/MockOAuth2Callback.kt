package no.nav.security.mock.callback

import com.nimbusds.oauth2.sdk.TokenRequest
import java.util.UUID

data class MockOAuth2Callback(val issuerId: String, val jwtCallback: JwtCallback = DefaultJwtCallback()) {
    constructor(issuerId: String) : this(issuerId, DefaultJwtCallback())
}

interface JwtCallback {
    fun subject(tokenRequest: TokenRequest): String
    fun addClaims(tokenRequest: TokenRequest): Map<String, Any>
}

class DefaultJwtCallback : JwtCallback {
    override fun subject(tokenRequest: TokenRequest): String = UUID.randomUUID().toString()
    override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> = mapOf(
        "azp" to "todo",
        "acr" to "Level4",
        "tid" to UUID.randomUUID().toString(),
        "ver" to "2.0"
    )
}