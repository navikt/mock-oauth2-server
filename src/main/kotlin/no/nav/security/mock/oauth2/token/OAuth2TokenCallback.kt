package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.oauth2.extensions.clientIdAsString
import no.nav.security.mock.oauth2.extensions.grantType
import no.nav.security.mock.oauth2.extensions.replaceValues
import no.nav.security.mock.oauth2.extensions.scopesWithoutOidcScopes
import no.nav.security.mock.oauth2.grant.audienceOrEmpty
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

        override fun subject(tokenRequest: TokenRequest): String =
            when (GrantType.CLIENT_CREDENTIALS) {
                tokenRequest.grantType() -> tokenRequest.clientIdAsString()
                else -> subject
            }

        override fun typeHeader(tokenRequest: TokenRequest): String = typeHeader

        override fun audience(tokenRequest: TokenRequest): List<String> {
            val audienceParam = tokenRequest.audienceOrEmpty()
            return when {
                audience != null -> audience
                audienceParam.isNotEmpty() -> audienceParam
                tokenRequest.scope != null -> tokenRequest.scopesWithoutOidcScopes()
                else -> listOf("default")
            }
        }

        override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> =
            mutableMapOf<String, Any>(
                "tid" to issuerId,
            ).apply {
                putAll(claims)
                if (tokenRequest.grantType() == GrantType.AUTHORIZATION_CODE) {
                    put("azp", tokenRequest.clientIdAsString())
                }
            }

        override fun tokenExpiry(): Long = expiry
    }

data class RequestMappingTokenCallback(
    val issuerId: String,
    val requestMappings: List<RequestMapping>,
    val tokenExpiry: Long = Duration.ofHours(1).toSeconds(),
    // Populated at runtime from the original auth request (e.g. login_hint, acr_values).
    // Not part of the JSON config — defaults to empty on deserialization.
    val extraParams: Map<String, String> = emptyMap(),
) : OAuth2TokenCallback {
    override fun issuerId(): String = issuerId

    override fun subject(tokenRequest: TokenRequest): String? = requestMappings.getClaimOrNull(tokenRequest, "sub")

    override fun typeHeader(tokenRequest: TokenRequest): String = requestMappings.getTypeHeader(tokenRequest)

    override fun audience(tokenRequest: TokenRequest): List<String> = requestMappings.getClaimOrNull(tokenRequest, "aud") ?: emptyList()

    override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> = requestMappings.getClaims(tokenRequest)

    override fun tokenExpiry(): Long = tokenExpiry

    private fun List<RequestMapping>.getClaims(tokenRequest: TokenRequest): Map<String, Any> {
        // Convert extraParams to List-values for isMatch() compatibility
        val extraParamsList = extraParams.mapValues { listOf(it.value) }
        val claims = firstOrNull { it.isMatch(tokenRequest, extraParamsList) }?.claims ?: emptyMap()

        // Merge token body params with auth-request params so ${login_hint} etc. resolve in claim templates
        val templateParams =
            tokenRequest.toHTTPRequest().bodyAsFormParameters
                .mapValues { it.value.joinToString(separator = " ") } + extraParams

        // in case client_id is not set as form param but as basic auth, we add it to the template params in two different formats for backwards compatibility
        return claims.replaceValues(
            templateParams +
                mapOf("clientId" to tokenRequest.clientIdAsString()) +
                mapOf("client_id" to tokenRequest.clientIdAsString()),
        )
    }

    private inline fun <reified T> List<RequestMapping>.getClaimOrNull(
        tokenRequest: TokenRequest,
        key: String,
    ): T? = getClaims(tokenRequest)[key] as? T

    private fun List<RequestMapping>.getTypeHeader(tokenRequest: TokenRequest) = firstOrNull { it.isMatch(tokenRequest) }?.typeHeader ?: JOSEObjectType.JWT.type
}

data class RequestMapping(
    private val requestParam: String,
    private val match: String,
    val claims: Map<String, Any> = emptyMap(),
    val typeHeader: String = JOSEObjectType.JWT.type,
) {
    /**
     * Checks whether this mapping matches the given token request.
     *
     * @param extraParams Additional params (e.g. from the original auth request such as login_hint)
     *                    merged on top of the token request body before matching.
     */
    fun isMatch(
        tokenRequest: TokenRequest,
        extraParams: Map<String, List<String>> = emptyMap(),
    ): Boolean {
        val allParams = tokenRequest.toHTTPRequest().bodyAsFormParameters + extraParams
        return allParams[requestParam]?.any {
            match == "*" || match == it || match.toRegex().matchEntire(it) != null
        } ?: false
    }
}
