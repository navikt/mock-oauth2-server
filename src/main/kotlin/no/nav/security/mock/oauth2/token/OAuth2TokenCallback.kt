package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JOSEObjectType
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

    fun typeHeader(tokenRequest: TokenRequest): String

    fun audience(tokenRequest: TokenRequest): List<String>

    fun addClaims(tokenRequest: TokenRequest): Map<String, Any>

    fun tokenExpiry(): Long
}

// TODO: for JwtBearerGrant and TokenExchange should be able to ovverride sub, make sub nullable and return some default
open class DefaultOAuth2TokenCallback
    @JvmOverloads
    constructor(
        private val issuerId: String = "default",
        private val subject: String = UUID.randomUUID().toString(),
        private val typeHeader: String = JOSEObjectType.JWT.type,
        // needs to be nullable in order to know if a list has explicitly been set, empty list should be a allowable value
        private val audience: List<String>? = null,
        private val claims: Map<String, Any> = emptyMap(),
        private val expiry: Long = 3600,
    ) : OAuth2TokenCallback {
        override fun issuerId(): String = issuerId

        override fun subject(tokenRequest: TokenRequest): String {
            return when (GrantType.CLIENT_CREDENTIALS) {
                tokenRequest.grantType() -> tokenRequest.clientIdAsString()
                else -> subject
            }
        }

        override fun typeHeader(tokenRequest: TokenRequest): String {
            return typeHeader
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
            mutableMapOf<String, Any>(
                "tid" to issuerId,
            ).apply {
                putAll(claims)
                put("azp", tokenRequest.clientIdAsString())
            }

        override fun tokenExpiry(): Long = expiry
    }

data class RequestMappingTokenCallback(
    val issuerId: String,
    val requestMappings: List<RequestMapping>,
    val tokenExpiry: Long = Duration.ofHours(1).toSeconds(),
) : OAuth2TokenCallback {
    override fun issuerId(): String = issuerId

    override fun subject(tokenRequest: TokenRequest): String? = requestMappings.getClaimOrNull(tokenRequest, "sub")

    override fun typeHeader(tokenRequest: TokenRequest): String = requestMappings.getTypeHeader(tokenRequest)

    override fun audience(tokenRequest: TokenRequest): List<String> = requestMappings.getClaimOrNull(tokenRequest, "aud") ?: emptyList()

    override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> = requestMappings.getClaims(tokenRequest)

    override fun tokenExpiry(): Long = tokenExpiry

    private fun List<RequestMapping>.getClaims(tokenRequest: TokenRequest): Map<String, Any> {
        val claims = firstOrNull { it.isMatch(tokenRequest) }?.claims ?: emptyMap()
        val customParameters = tokenRequest.customParameters.mapValues { (_, value) -> value.first() }
        val variables =
            if (tokenRequest.grantType() == GrantType.CLIENT_CREDENTIALS) {
                customParameters + ("clientId" to tokenRequest.clientIdAsString())
            } else {
                customParameters
            }
        return claims.mapValues { (_, value) ->
            when (value) {
                is String -> replaceVariables(value, variables)
                is List<*> ->
                    value.map { v ->
                        if (v is String) {
                            replaceVariables(v, variables)
                        } else {
                            v
                        }
                    }
                else -> value
            }
        }
    }

    private inline fun <reified T> List<RequestMapping>.getClaimOrNull(
        tokenRequest: TokenRequest,
        key: String,
    ): T? = getClaims(tokenRequest)[key] as? T

    private fun List<RequestMapping>.getTypeHeader(tokenRequest: TokenRequest) = firstOrNull { it.isMatch(tokenRequest) }?.typeHeader ?: JOSEObjectType.JWT.type

    private fun replaceVariables(
        input: String,
        replacements: Map<String, String>,
    ): String {
        val pattern = Regex("""\$\{(\w+)}""")
        return pattern.replace(input) { result ->
            val variableName = result.groupValues[1]
            val replacement = replacements[variableName]
            replacement ?: result.value
        }
    }
}

data class RequestMapping(
    private val requestParam: String,
    private val match: String,
    val claims: Map<String, Any> = emptyMap(),
    val typeHeader: String = JOSEObjectType.JWT.type,
) {
    fun isMatch(tokenRequest: TokenRequest): Boolean {
        return tokenRequest.toHTTPRequest().bodyAsFormParameters[requestParam]?.any {
            match == "*" || match == it || match.toRegex().matchEntire(it) != null
        } ?: false
    }
}
