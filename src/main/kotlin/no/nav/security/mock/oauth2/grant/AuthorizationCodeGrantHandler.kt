package no.nav.security.mock.oauth2.grant

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse
import mu.KotlinLogging
import no.nav.security.mock.callback.TokenCallback
import no.nav.security.mock.extensions.authorizationCode
import no.nav.security.mock.extensions.expiresIn
import no.nav.security.mock.oauth2.OAuth2TokenProvider
import no.nav.security.mock.oauth2.OAuth2TokenResponse
import okhttp3.HttpUrl
import java.util.UUID

private val log = KotlinLogging.logger {}

class AuthorizationCodeHandler(
    private val tokenProvider: OAuth2TokenProvider
) : GrantHandler {

    private val codeToAuthRequestCache: MutableMap<AuthorizationCode, AuthenticationRequest> = HashMap()

    fun authorizationCodeResponse(authenticationRequest: AuthenticationRequest): AuthenticationSuccessResponse {
        val code = AuthorizationCode()
        log.debug("issuing authorization code $code")
        codeToAuthRequestCache[code] = authenticationRequest
        return AuthenticationSuccessResponse(
            authenticationRequest.redirectionURI,
            code,
            null,
            null,
            authenticationRequest.state,
            null,
            authenticationRequest.responseMode
        )
    }

    override fun tokenResponse(
        tokenRequest: TokenRequest,
        issuerUrl: HttpUrl,
        tokenCallback: TokenCallback
    ): OAuth2TokenResponse {
        val authenticationRequest = getAuthenticationRequest(tokenRequest.authorizationCode())
        val scope: String = tokenRequest.scope.toString()
        val nonce: String? = authenticationRequest?.nonce?.value
        val idToken: SignedJWT = tokenProvider.idToken(tokenRequest, issuerUrl, nonce, tokenCallback)
        val accessToken: SignedJWT = tokenProvider.accessToken(tokenRequest, issuerUrl, nonce, tokenCallback)

        return OAuth2TokenResponse(
            tokenType = "Bearer",
            idToken = idToken.serialize(),
            accessToken = accessToken.serialize(),
            refreshToken = UUID.randomUUID().toString(),
            expiresIn = idToken.expiresIn(),
            scope = scope
        )
    }

    private fun getAuthenticationRequest(code: AuthorizationCode): AuthenticationRequest? = codeToAuthRequestCache[code]
}