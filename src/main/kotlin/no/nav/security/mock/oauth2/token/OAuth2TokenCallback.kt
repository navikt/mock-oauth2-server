package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.TokenRequest
import mu.KotlinLogging
import no.nav.security.mock.oauth2.extensions.clientIdAsString
import no.nav.security.mock.oauth2.extensions.grantType
import no.nav.security.mock.oauth2.extensions.replaceValues
import no.nav.security.mock.oauth2.extensions.scopesWithoutOidcScopes
import no.nav.security.mock.oauth2.grant.audienceOrEmpty
import java.time.Duration
import java.util.UUID

private val log = KotlinLogging.logger {}

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
) : OAuth2TokenCallback {
    companion object {
        const val SUBJECT_PARAM = "subject"
    }
    override fun issuerId(): String = issuerId

    override fun subject(tokenRequest: TokenRequest): String? = resolve(tokenRequest).claims["sub"] as? String

    override fun typeHeader(tokenRequest: TokenRequest): String = resolve(tokenRequest).typeHeader

    override fun audience(tokenRequest: TokenRequest): List<String> =
        resolve(tokenRequest).claims["aud"].toAudienceList()

    override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> = resolve(tokenRequest).claims

    override fun tokenExpiry(): Long = tokenExpiry

    /**
     * Returns a view of this callback that supplements matching with [extraMatchParams] —
     * key/value pairs that are considered when no matching form parameter is found in the token request body.
     *
     * Intended for use during interactive login, where the login username is injected under the
     * [SUBJECT_PARAM] key so that a [RequestMapping] with `requestParam = "subject"` can match on it:
     *
     * ```json
     * { "requestParam": "subject", "match": "alice", "claims": { "role": "admin" } }
     * ```
     *
     * **Precedence (highest to lowest):**
     * 1. `client_id` / `clientId` — always authoritative
     * 2. Token request form parameters — override extra params on the same key
     * 3. [extraMatchParams] — used only when no form parameter exists for the key
     */
    fun withExtraMatchParams(extraMatchParams: Map<String, String>): OAuth2TokenCallback =
        ExtraMatchParamsWrapper(extraMatchParams)

    private inner class ExtraMatchParamsWrapper(
        private val extraMatchParams: Map<String, String>,
    ) : OAuth2TokenCallback by this@RequestMappingTokenCallback {
        override fun subject(tokenRequest: TokenRequest): String? =
            resolve(tokenRequest, extraMatchParams).claims["sub"] as? String

        override fun typeHeader(tokenRequest: TokenRequest): String =
            resolve(tokenRequest, extraMatchParams).typeHeader

        override fun audience(tokenRequest: TokenRequest): List<String> =
            resolve(tokenRequest, extraMatchParams).claims["aud"].toAudienceList()

        override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> =
            resolve(tokenRequest, extraMatchParams).claims
    }

    private fun resolve(
        tokenRequest: TokenRequest,
        extraMatchParams: Map<String, String> = emptyMap(),
    ): ResolvedMapping {
        val rawFormParams: Map<String, List<String>> = tokenRequest.toHTTPRequest().bodyAsFormParameters
        val matched = requestMappings.firstOrNull { it.isMatch(rawFormParams, tokenRequest, extraMatchParams) }
        val rawClaims = matched?.claims ?: emptyMap()
        // Template variable precedence (highest to lowest):
        //   1. client_id / clientId  — always authoritative
        //   2. form params           — token POST body
        //   3. extraMatchParams      — e.g. login subject
        val templateParams =
            buildMap {
                putAll(extraMatchParams)
                putAll(rawFormParams.mapValues { it.value.joinToString(separator = " ") })
                put("clientId", tokenRequest.clientIdAsString())
                put("client_id", tokenRequest.clientIdAsString())
            }
        return ResolvedMapping(
            claims = rawClaims.replaceValues(templateParams),
            typeHeader = matched?.typeHeader ?: JOSEObjectType.JWT.type,
        )
    }

    private data class ResolvedMapping(
        val claims: Map<String, Any>,
        val typeHeader: String,
    )
}

data class RequestMapping(
    private val requestParam: String,
    private val match: String,
    val claims: Map<String, Any> = emptyMap(),
    val typeHeader: String = JOSEObjectType.JWT.type,
) {
    private val matchRegex: Regex? =
        if (match == "*") {
            null
        } else {
            runCatching { match.toRegex() }.getOrElse {
                log.warn("RequestMapping match value '{}' is not a valid regex — only exact-string matching will apply", match)
                null
            }
        }

    fun isMatch(
        formParams: Map<String, List<String>>,
        tokenRequest: TokenRequest,
        extraMatchParams: Map<String, String> = emptyMap(),
    ): Boolean {
        val effectiveValues =
            when {
                formParams[requestParam].isNullOrEmpty().not() -> formParams[requestParam]
                requestParam == "client_id" ->
                    tokenRequest.clientAuthentication?.clientID?.value?.let { listOf(it) }
                        ?: tokenRequest.clientID?.value?.let { listOf(it) }
                else -> extraMatchParams[requestParam]?.let { listOf(it) }
            }
        return effectiveValues?.any {
            match == "*" || match == it || matchRegex?.matchEntire(it) != null
        } ?: false
    }

    fun isMatch(
        tokenRequest: TokenRequest,
        extraMatchParams: Map<String, String> = emptyMap(),
    ): Boolean =
        isMatch(
            tokenRequest.toHTTPRequest().bodyAsFormParameters,
            tokenRequest,
            extraMatchParams,
        )
}

private fun Any?.toAudienceList(): List<String> =
    when (this) {
        is String -> listOf(this)
        is List<*> -> filterIsInstance<String>()
        else -> emptyList()
    }
