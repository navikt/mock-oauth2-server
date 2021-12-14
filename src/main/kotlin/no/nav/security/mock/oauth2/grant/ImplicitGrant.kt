package no.nav.security.mock.oauth2.grant

import com.nimbusds.oauth2.sdk.AuthorizationGrant
import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.TokenRequest
import okhttp3.HttpUrl

internal class ImplicitGrant : AuthorizationGrant(GrantType.IMPLICIT) {
    override fun toParameters(): MutableMap<String, MutableList<String>> {
        return mutableMapOf()
    }

    companion object {
        fun asTokenRequest(url: HttpUrl, authorizationRequest: AuthorizationRequest): TokenRequest {
            return TokenRequest(
                url.toUri(),
                authorizationRequest.clientID,
                ImplicitGrant(),
                authorizationRequest.scope
            )
        }
    }
}
