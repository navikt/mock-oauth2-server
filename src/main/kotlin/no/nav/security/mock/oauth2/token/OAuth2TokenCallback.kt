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

/**
 * Extension point for callbacks that need access to params from the original authorization request.
 *
 * Lifecycle/source of [authRequestParams]:
 * - For `authorization_code` token exchange, values come from the original `/authorize` query params.
 * - For `refresh_token` token exchange, values come from server-side persisted params captured during the
 *   original authorization-code exchange.
 * - For flows where no authorization request exists (for example `client_credentials`), the map may be empty.
 *
 * Precedence:
 * - If a key exists both in token request body params and in [authRequestParams], [authRequestParams] wins.
 * - `client_id` is the exception: it always resolves from the authenticated token request.
 *
 * Constraints for refresh-token reuse:
 * - Persisted params are sanitized and bounded before storage.
 * - By default, keys `claims`, `request`, and `client_assertion` are excluded from persisted storage.
 * - Value length is truncated, and count/total-size limits are enforced (see `AuthRequestParamsStoragePolicy`).
 *
 * Implementers should treat [authRequestParams] as optional context and handle missing keys defensively.
 */
interface AuthRequestAwareOAuth2TokenCallback : OAuth2TokenCallback {
    override fun subject(tokenRequest: TokenRequest): String? = subject(tokenRequest, emptyMap())

    override fun typeHeader(tokenRequest: TokenRequest): String = typeHeader(tokenRequest, emptyMap())

    override fun audience(tokenRequest: TokenRequest): List<String> = audience(tokenRequest, emptyMap())

    override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> = addClaims(tokenRequest, emptyMap())

    fun subject(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): String?

    fun typeHeader(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): String

    fun audience(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): List<String>

    fun addClaims(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): Map<String, Any>
}

internal fun OAuth2TokenCallback.resolveSubject(
    tokenRequest: TokenRequest,
    authRequestParams: Map<String, String>,
): String? =
    when (this) {
        is AuthRequestAwareOAuth2TokenCallback -> this.subject(tokenRequest, authRequestParams)
        else -> this.subject(tokenRequest)
    }

internal fun OAuth2TokenCallback.resolveTypeHeader(
    tokenRequest: TokenRequest,
    authRequestParams: Map<String, String>,
): String =
    when (this) {
        is AuthRequestAwareOAuth2TokenCallback -> this.typeHeader(tokenRequest, authRequestParams)
        else -> this.typeHeader(tokenRequest)
    }

internal fun OAuth2TokenCallback.resolveAudience(
    tokenRequest: TokenRequest,
    authRequestParams: Map<String, String>,
): List<String> =
    when (this) {
        is AuthRequestAwareOAuth2TokenCallback -> this.audience(tokenRequest, authRequestParams)
        else -> this.audience(tokenRequest)
    }

internal fun OAuth2TokenCallback.resolveClaims(
    tokenRequest: TokenRequest,
    authRequestParams: Map<String, String>,
): Map<String, Any> =
    when (this) {
        is AuthRequestAwareOAuth2TokenCallback -> this.addClaims(tokenRequest, authRequestParams)
        else -> this.addClaims(tokenRequest)
    }

internal fun OAuth2TokenCallback.withSubject(subject: String?): OAuth2TokenCallback =
    if (subject == null) {
        this
    } else {
        SubjectOAuth2TokenCallback(this, subject)
    }

private class SubjectOAuth2TokenCallback(
    private val delegate: OAuth2TokenCallback,
    private val subject: String,
) : AuthRequestAwareOAuth2TokenCallback {
    override fun issuerId(): String = delegate.issuerId()

    override fun subject(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): String? = delegate.resolveClaims(tokenRequest, context(authRequestParams))["sub"] as? String ?: subject

    override fun typeHeader(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): String = delegate.resolveTypeHeader(tokenRequest, context(authRequestParams))

    override fun audience(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): List<String> = delegate.resolveAudience(tokenRequest, context(authRequestParams))

    override fun addClaims(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): Map<String, Any> = delegate.resolveClaims(tokenRequest, context(authRequestParams))

    override fun tokenExpiry(): Long = delegate.tokenExpiry()

    private fun context(authRequestParams: Map<String, String>) = authRequestParams + (RequestMappingTokenCallback.SUBJECT_PARAM to subject)
}

private data class TokenRequestContext(
    val formParameters: Map<String, List<String>>,
    val authRequestParamsList: Map<String, List<String>>,
    val templateParams: Map<String, String>,
)

private fun TokenRequest.requestContext(authRequestParams: Map<String, String>): TokenRequestContext {
    val formParameters = toHTTPRequest().bodyAsFormParameters
    return TokenRequestContext(
        formParameters = formParameters,
        authRequestParamsList = authRequestParams.mapValues { listOf(it.value) },
        templateParams = formParameters.mapValues { it.value.joinToString(separator = " ") } + authRequestParams,
    )
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
) : AuthRequestAwareOAuth2TokenCallback {
    companion object {
        const val SUBJECT_PARAM = "subject"
    }

    override fun issuerId(): String = issuerId

    override fun subject(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): String? = requestMappings.getClaimOrNull(tokenRequest, "sub", authRequestParams)

    override fun typeHeader(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): String = requestMappings.getTypeHeader(tokenRequest, authRequestParams)

    override fun audience(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): List<String> =
        when (val aud = requestMappings.getClaimOrNull<Any>(tokenRequest, "aud", authRequestParams)) {
            is String -> listOf(aud)
            is List<*> -> aud.filterIsInstance<String>()
            else -> emptyList()
        }

    override fun addClaims(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): Map<String, Any> = requestMappings.getClaims(tokenRequest, authRequestParams)

    override fun tokenExpiry(): Long = tokenExpiry

    /**
     * Adds fallback values used when neither the token request nor preserved authorization-request context
     * contains a key. Prefer [AuthRequestAwareOAuth2TokenCallback] for new integrations.
     */
    @Deprecated(
        message = "Implement AuthRequestAwareOAuth2TokenCallback to receive authorization-request context directly.",
        replaceWith = ReplaceWith("this"),
    )
    fun withExtraMatchParams(extraMatchParams: Map<String, String>): OAuth2TokenCallback =
        object : AuthRequestAwareOAuth2TokenCallback {
            override fun issuerId(): String = this@RequestMappingTokenCallback.issuerId()

            override fun subject(
                tokenRequest: TokenRequest,
                authRequestParams: Map<String, String>,
            ): String? = this@RequestMappingTokenCallback.subject(tokenRequest, mergedContext(tokenRequest, authRequestParams))

            override fun typeHeader(
                tokenRequest: TokenRequest,
                authRequestParams: Map<String, String>,
            ): String = this@RequestMappingTokenCallback.typeHeader(tokenRequest, mergedContext(tokenRequest, authRequestParams))

            override fun audience(
                tokenRequest: TokenRequest,
                authRequestParams: Map<String, String>,
            ): List<String> = this@RequestMappingTokenCallback.audience(tokenRequest, mergedContext(tokenRequest, authRequestParams))

            override fun addClaims(
                tokenRequest: TokenRequest,
                authRequestParams: Map<String, String>,
            ): Map<String, Any> = this@RequestMappingTokenCallback.addClaims(tokenRequest, mergedContext(tokenRequest, authRequestParams))

            override fun tokenExpiry(): Long = this@RequestMappingTokenCallback.tokenExpiry()

            private fun mergedContext(
                tokenRequest: TokenRequest,
                authRequestParams: Map<String, String>,
            ): Map<String, String> {
                val formParameters = tokenRequest.toHTTPRequest().bodyAsFormParameters
                val fallbackParams =
                    extraMatchParams.filterKeys {
                        it != "client_id" && it !in authRequestParams && formParameters[it].isNullOrEmpty()
                    }
                return fallbackParams + authRequestParams
            }
        }

    private fun List<RequestMapping>.getClaims(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): Map<String, Any> {
        val requestContext = tokenRequest.requestContext(authRequestParams)
        val claims =
            firstOrNull { it.isMatchWithAuthRequestParams(tokenRequest, requestContext.formParameters, requestContext.authRequestParamsList) }?.claims
                ?: emptyMap()
        val clientId = tokenRequest.clientIdAsString()

        // Merge token body params with auth-request params so ${login_hint} etc. resolve in claim templates
        // in case client_id is not set as form param but as basic auth, we add it to the template params in two different formats for backwards compatibility
        return claims.replaceValues(
            requestContext.templateParams +
                mapOf("clientId" to clientId) +
                mapOf("client_id" to clientId),
        )
    }

    private inline fun <reified T> List<RequestMapping>.getClaimOrNull(
        tokenRequest: TokenRequest,
        key: String,
        authRequestParams: Map<String, String>,
    ): T? = getClaims(tokenRequest, authRequestParams)[key] as? T

    private fun List<RequestMapping>.getTypeHeader(
        tokenRequest: TokenRequest,
        authRequestParams: Map<String, String>,
    ): String {
        val requestContext = tokenRequest.requestContext(authRequestParams)
        return firstOrNull { it.isMatchWithAuthRequestParams(tokenRequest, requestContext.formParameters, requestContext.authRequestParamsList) }?.typeHeader
            ?: JOSEObjectType.JWT.type
    }
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
            runCatching { match.toRegex() }.getOrNull()
        }

    @Deprecated(
        message = "Authorization-request context is supplied by AuthRequestAwareOAuth2TokenCallback.",
        replaceWith = ReplaceWith("isMatchWithAuthRequestParams(tokenRequest, tokenRequest.toHTTPRequest().bodyAsFormParameters, emptyMap())"),
    )
    fun isMatch(
        tokenRequest: TokenRequest,
        extraMatchParams: Map<String, String> = emptyMap(),
    ): Boolean = isMatch(tokenRequest.toHTTPRequest().bodyAsFormParameters, tokenRequest, extraMatchParams)

    @Deprecated(
        message = "Authorization-request context is supplied by AuthRequestAwareOAuth2TokenCallback.",
        replaceWith = ReplaceWith("isMatchWithAuthRequestParams(tokenRequest, formParameters, emptyMap())"),
    )
    fun isMatch(
        formParameters: Map<String, List<String>>,
        tokenRequest: TokenRequest,
        extraMatchParams: Map<String, String> = emptyMap(),
    ): Boolean {
        val effectiveValues =
            when {
                formParameters[requestParam].isNullOrEmpty().not() -> {
                    formParameters[requestParam]
                }

                requestParam == "client_id" -> {
                    tokenRequest.clientAuthentication
                        ?.clientID
                        ?.value
                        ?.let { listOf(it) }
                        ?: tokenRequest.clientID?.value?.let { listOf(it) }
                }

                else -> {
                    extraMatchParams[requestParam]?.let { listOf(it) }
                }
            }
        return effectiveValues?.any {
            match == "*" || match == it || matchRegex?.matchEntire(it) != null
        } ?: false
    }

    internal fun isMatchWithAuthRequestParams(
        tokenRequest: TokenRequest,
        formParameters: Map<String, List<String>>,
        authRequestParams: Map<String, List<String>> = emptyMap(),
    ): Boolean {
        val effectiveValues: List<String> =
            if (requestParam == "client_id") {
                tokenRequest.clientAuthentication
                    ?.clientID
                    ?.value
                    ?.let { listOf(it) }
                    ?: tokenRequest.clientID?.value?.let { listOf(it) }
                    ?: emptyList()
            } else {
                authRequestParams[requestParam]
                    ?: formParameters[requestParam]
                    ?: emptyList()
            }
        return effectiveValues.any {
            match == "*" || match == it || matchRegex?.matchEntire(it) != null
        }
    }
}
