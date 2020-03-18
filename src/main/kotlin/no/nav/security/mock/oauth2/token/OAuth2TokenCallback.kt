package no.nav.security.mock.oauth2.token

import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.openid.connect.sdk.OIDCScopeValue
import no.nav.security.mock.oauth2.extensions.clientIdAsString
import no.nav.security.mock.oauth2.extensions.grantType
import java.util.UUID

interface OAuth2TokenCallback {
    fun issuerId(): String
    fun subject(tokenRequest: TokenRequest): String
    fun audience(tokenRequest: TokenRequest): String
    fun addClaims(tokenRequest: TokenRequest): Map<String, Any>
    fun tokenExpiry(): Long
}

open class DefaultOAuth2TokenCallback(
    private val issuerId: String = "default",
    private val subject: String = UUID.randomUUID().toString(),
    private val audience: String? = null,
    private val claims: Map<String, Any> = emptyMap(),
    private val expiry: Long = 3600
) : OAuth2TokenCallback {

    override fun issuerId(): String = issuerId

    override fun subject(tokenRequest: TokenRequest): String {
        return when (GrantType.CLIENT_CREDENTIALS) {
            tokenRequest.grantType() -> tokenRequest.clientID.value
            else -> subject
        }
    }

    override fun audience(tokenRequest: TokenRequest): String {
        val oidcScopeList = OIDCScopeValue.values().map { it.toString() }
        return audience
            ?: let {
                tokenRequest.scope?.toStringList()
                    ?.filterNot { oidcScopeList.contains(it) }?.firstOrNull()
            } ?: "default"
    }

    override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> =
        claims.toMutableMap().apply {
            putAll(
                mapOf(
                    "azp" to tokenRequest.clientIdAsString(),
                    "tid" to issuerId
                )
            )
        }

    override fun tokenExpiry(): Long = expiry
}
