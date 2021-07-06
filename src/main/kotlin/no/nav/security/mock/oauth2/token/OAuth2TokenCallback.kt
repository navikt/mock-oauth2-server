package no.nav.security.mock.oauth2.token

import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.oauth2.extensions.clientIdAsString
import no.nav.security.mock.oauth2.extensions.grantType
import no.nav.security.mock.oauth2.extensions.scopesWithoutOidcScopes
import no.nav.security.mock.oauth2.extensions.tokenExchangeGrantOrNull
import java.time.Duration
import java.util.UUID

interface OAuth2TokenCallback {
    fun issuerId(): String
    fun subject(tokenRequest: TokenRequest): String?
    fun audience(tokenRequest: TokenRequest): List<String>
    fun addClaims(tokenRequest: TokenRequest): Map<String, Any>
    fun tokenExpiry(): Long
}

// TODO: for JwtBearerGrant and TokenExchange should be able to ovverride sub, make sub nullable and return some default
open class DefaultOAuth2TokenCallback @JvmOverloads constructor(
    private val issuerId: String = "default",
    private val subject: String = UUID.randomUUID().toString(),
    // needs to be nullable in order to know if a list has explicitly been set, empty list should be a allowable value
    private val audience: List<String>? = null,
    private val claims: Map<String, Any> = emptyMap(),
    private val expiry: Long = 3600
) : OAuth2TokenCallback {

    override fun issuerId(): String = issuerId

    override fun subject(tokenRequest: TokenRequest): String {
        return when (GrantType.CLIENT_CREDENTIALS) {
            tokenRequest.grantType() -> tokenRequest.clientIdAsString()
            else -> subject
        }
    }

    override fun audience(tokenRequest: TokenRequest): List<String> {
        val audienceParam = tokenRequest.tokenExchangeGrantOrNull()?.audience
        return when {
            audience != null -> audience
            audienceParam != null -> audienceParam
            tokenRequest.scope != null -> tokenRequest.scopesWithoutOidcScopes()
            else -> listOf("default")
        }
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

data class RequestMappingTokenCallback(
    val issuerId: String,
    val requestMappings: Set<RequestMapping>,
    val tokenExpiry: Long = Duration.ofHours(1).toSeconds()
) : OAuth2TokenCallback {
    override fun issuerId(): String = issuerId
    override fun subject(tokenRequest: TokenRequest): String? =
        requestMappings.getClaimOrNull(tokenRequest, "sub")

    override fun audience(tokenRequest: TokenRequest): List<String> =
        requestMappings.getClaimOrNull(tokenRequest, "aud") ?: emptyList()

    override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> =
        requestMappings.getClaims(tokenRequest)

    override fun tokenExpiry(): Long = tokenExpiry

    private fun Set<RequestMapping>.getClaims(tokenRequest: TokenRequest) =
        firstOrNull { it.isMatch(tokenRequest) }?.claims ?: emptyMap()

    private inline fun <reified T> Set<RequestMapping>.getClaimOrNull(tokenRequest: TokenRequest, key: String): T? =
        getClaims(tokenRequest)[key] as? T
}

data class RequestMapping(
    private val requestParam: String,
    private val match: String = "*",
    val claims: Map<String, Any> = emptyMap()
) {
    fun isMatch(tokenRequest: TokenRequest): Boolean =
        tokenRequest.toHTTPRequest().queryParameters[requestParam]?.any {
            if (match != "*") it == match else true
        } ?: false
}
