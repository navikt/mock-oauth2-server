package no.nav.security.mock.callback

import com.nimbusds.oauth2.sdk.TokenRequest
import java.util.UUID

interface JwtCallback {
    fun issuerId(): String
    fun subject(tokenRequest: TokenRequest): String
    fun addClaims(tokenRequest: TokenRequest): Map<String, Any>
    fun tokenExpiry(): Int
}

class DefaultJwtCallback(
    private val issuerId: String = "default",
    private val subject: String = UUID.randomUUID().toString(),
    private val claims: Map<String, Any> = mapOf(
        "azp" to "todo",
        "acr" to "Level4",
        "tid" to UUID.randomUUID().toString(),
        "ver" to "2.0"
    ),
    private val expiry: Int = 3600
) : JwtCallback {
    override fun issuerId(): String = issuerId
    override fun subject(tokenRequest: TokenRequest): String = subject
    override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> = claims
    override fun tokenExpiry(): Int = expiry
}