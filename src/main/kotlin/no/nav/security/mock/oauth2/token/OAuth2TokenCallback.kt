package no.nav.security.mock.oauth2.token

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.TokenRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.security.mock.oauth2.extensions.clientIdAsString
import no.nav.security.mock.oauth2.extensions.grantType
import no.nav.security.mock.oauth2.extensions.scopesWithoutOidcScopes
import no.nav.security.mock.oauth2.extensions.tokenExchangeGrantOrNull
import no.nav.security.mock.oauth2.http.objectMapper
import java.time.Duration
import java.util.*

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

        // TODO: hack choose first element. Rewrite to support multiple elements and custom objects
        val params = (tokenRequest.toHTTPRequest().bodyAsFormParameters.map {
            it.key to it.value.first()
        }).toMap() + mapOf("clientId" to tokenRequest.clientIdAsString())

        return claims.mapValues { (_, value) ->
            val v = objectMapper.writeValueAsString(value)
            val jsonElement = Json.parseToJsonElement(v)
            when (jsonElement) {
                is JsonPrimitive ->
                    if (jsonElement.isString) {
                        replaceVariables(jsonElement.content, params)
                    } else {
                        jsonElement.content
                    }

                is JsonObject -> {
                    jsonElement.mapValues { (_, value) ->
                        if (value is JsonPrimitive) {
                            replaceVariables(value.content, params)
                        } else if (value is JsonArray)
                            value.map { element ->
                                if (element is JsonPrimitive) {
                                    replaceVariables(element.content, params)
                                } else {
                                    element
                                }
                            }
                        else {
                            value
                        }
                    }
                }

                is JsonArray -> {
                    jsonElement.map { element ->
                        if (element is JsonPrimitive) {
                            replaceVariables(element.content, params)
                        } else {
                            element
                        }
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
        return replacements.entries.fold(input) { acc, (key, value) ->
            acc.replace("\${$key}", value)
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
