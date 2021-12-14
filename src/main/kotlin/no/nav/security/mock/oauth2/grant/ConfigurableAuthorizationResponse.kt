package no.nav.security.mock.oauth2.grant

import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse
import com.nimbusds.oauth2.sdk.token.AccessToken

class ConfigurableAuthorizationResponse(
    private val authorizationRequest: AuthorizationRequest,
    accessToken: AccessToken,
    private val expiresIn: Int,
) : AuthorizationSuccessResponse(
    authorizationRequest.redirectionURI,
    null,
    accessToken,
    authorizationRequest.state,
    authorizationRequest.impliedResponseMode()
) {
    override fun toParameters(): MutableMap<String, MutableList<String>> {
        return mutableMapOf<String, MutableList<String>>()
            .with("access_token", accessToken.value)
            .with("state", authorizationRequest.state.value)
            .with("token_type", accessToken.type.value)
            .with("expires_in", expiresIn.toString()).also { params ->
                authorizationRequest.scope?.let {
                    params.with("scope", it.toString())
                }
            }
    }

    private fun MutableMap<String, MutableList<String>>.with(key: String, value: String): MutableMap<String, MutableList<String>> {
        this[key] = mutableListOf(value)
        return this
    }
}
