package no.nav.security.mock.oauth2.token

import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.openid.connect.sdk.OIDCScopeValue
import java.util.UUID
import no.nav.security.mock.oauth2.extensions.clientIdAsString
import no.nav.security.mock.oauth2.extensions.grantType
import no.nav.security.mock.oauth2.grant.TokenExchangeGrant

interface OAuth2TokenCallback {
    fun issuerId(): String
    fun subject(tokenRequest: TokenRequest): String
    fun audience(tokenRequest: TokenRequest): List<String>
    fun addClaims(tokenRequest: TokenRequest): Map<String, Any>
    fun tokenExpiry(): Long
    fun issuerUrl(tokenRequest: TokenRequest): String?
}

// TODO: for JwtBearerGrant and TokenExchange should be able to ovverride sub, make sub nullable and return some default
open class DefaultOAuth2TokenCallback(
    private val issuerId: String = "default",
    private val subject: String = UUID.randomUUID().toString(),
    // needs to be nullable in order to know if a list has explicitly been set, empty list should be a allowable value
    private val audience: List<String>? = null,
    private val claims: Map<String, Any> = emptyMap(),
    private val expiry: Long = 3600,
    private val scopeIssAud: Map<String, Map<String, String>>? = null,
) : OAuth2TokenCallback {

    override fun issuerId(): String = issuerId

    override fun subject(tokenRequest: TokenRequest): String {
        return when (GrantType.CLIENT_CREDENTIALS) {
            tokenRequest.grantType() -> tokenRequest.clientIdAsString()
            else -> subject
        }
    }

    override fun issuerUrl(tokenRequest: TokenRequest): String? {
        return scopeIssAud?.filter {
            tokenRequest.scope?.toStringList()?.contains(it.key) ?: false
        }?.map { it.value["iss"] }?.firstOrNull()
    }

    override fun audience(tokenRequest: TokenRequest): List<String> {
        val oidcScopeList = OIDCScopeValue.values().map { it.toString() }
        return audience
            ?: let {
                scopeIssAud?.filter {
                    tokenRequest.scope?.toStringList()?.contains(it.key) ?: false
                }?.map { it.value["aud"] }?.filterNotNull()?.ifEmpty { null }
            }
            ?: let { tokenRequest.scope?.toStringList() }
            ?: (tokenRequest.authorizationGrant as? TokenExchangeGrant)?.audience
            ?: let {
                tokenRequest.scope?.toStringList()
                    ?.filterNot { oidcScopeList.contains(it) }
            } ?: listOf("default")
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
