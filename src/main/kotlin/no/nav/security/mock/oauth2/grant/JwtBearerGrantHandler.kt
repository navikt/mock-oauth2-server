package no.nav.security.mock.oauth2.grant

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier
import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.nimbusds.oauth2.sdk.JWTBearerGrant
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.extensions.expiresIn
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.HttpUrl
import java.util.HashSet
import java.util.UUID

class JwtBearerGrantHandler(private val tokenProvider: OAuth2TokenProvider) : GrantHandler {

    override fun tokenResponse(
        tokenRequest: TokenRequest,
        issuerUrl: HttpUrl,
        OAuth2TokenCallback: OAuth2TokenCallback
    ): OAuth2TokenResponse {

        val receivedClaimsSet = assertion(tokenRequest)
        val accessToken = tokenProvider.onBehalfOfAccessToken(
            receivedClaimsSet,
            tokenRequest,
            OAuth2TokenCallback
        )
        return OAuth2TokenResponse(
            tokenType = "Bearer",
            idToken = null,
            accessToken = accessToken.serialize(),
            refreshToken = UUID.randomUUID().toString(),
            expiresIn = accessToken.expiresIn(),
            scope = tokenRequest.scope.toString()
        )
    }

    private fun assertion(tokenRequest: TokenRequest): JWTClaimsSet =
        (tokenRequest.authorizationGrant as? JWTBearerGrant)?.jwtAssertion?.jwtClaimsSet
            ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "missing required parameter assertion")

    private fun verifyAssertion(issuerUrl: HttpUrl, assertion: String): JWTClaimsSet {
        val jwtProcessor: ConfigurableJWTProcessor<SecurityContext?> = DefaultJWTProcessor()
        jwtProcessor.jwsTypeVerifier = DefaultJOSEObjectTypeVerifier(JOSEObjectType("at+jwt"))
        val keySelector: JWSKeySelector<SecurityContext?> = JWSVerificationKeySelector(
            JWSAlgorithm.RS256,
            ImmutableJWKSet(tokenProvider.publicJwkSet())
        )
        jwtProcessor.jwsKeySelector = keySelector
        jwtProcessor.jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(
            JWTClaimsSet.Builder().issuer(issuerUrl.toString()).build(),
            HashSet(listOf("sub", "iat", "exp", "aud"))
        )
        return try {
            jwtProcessor.process(assertion, null)
        } catch (e: Exception) {
            throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "invalid assertion.", e)
        }
    }
}
