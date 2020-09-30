package no.nav.security.mock.oauth2.grant

import com.nimbusds.oauth2.sdk.AuthorizationGrant
import com.nimbusds.oauth2.sdk.GrantType
import no.nav.security.mock.oauth2.badRequest

val TOKEN_EXCHANGE = GrantType("urn:ietf:params:oauth:grant-type:token-exchange")

class TokenExchangeGrant(
    val subjectTokenType: String,
    val subjectToken: String,
    val audience: String
) : AuthorizationGrant(TOKEN_EXCHANGE) {

    override fun toParameters(): MutableMap<String, MutableList<String>> =
        mutableMapOf(
            "grant_type" to mutableListOf(TOKEN_EXCHANGE.value),
            "subject_token_type" to mutableListOf(subjectTokenType),
            "subject_token" to mutableListOf(subjectToken),
            "audience" to mutableListOf(audience)
        )

    companion object {
        fun parse(parameters: Map<String, String>): TokenExchangeGrant =
            TokenExchangeGrant(
                parameters.require("subject_token_type"),
                parameters.require("subject_token"),
                parameters.require("audience")
            )
    }
}

private inline fun <reified T> Map<String, T>.require(name: String): T =
    this[name] ?: throw badRequest("missing required parameter $name")