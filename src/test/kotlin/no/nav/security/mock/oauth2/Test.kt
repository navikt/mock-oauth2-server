package no.nav.security.mock.oauth2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

data class ParsedTokenResponse(
    val status: Int,
    val body: String
) {
    private val tokenResponse: OAuth2TokenResponse = jacksonObjectMapper().readValue(body)
    val tokenType = tokenResponse.tokenType
    val issuedTokenType = tokenResponse.issuedTokenType
    val refreshToken = tokenResponse.refreshToken
    val expiresIn = tokenResponse.expiresIn
    val scope = tokenResponse.scope
    val accessToken: SignedJWT? = tokenResponse.accessToken?.asJwt()
    val idToken: SignedJWT? = tokenResponse.idToken?.asJwt()
}

fun String.asJwt(): SignedJWT = SignedJWT.parse(this)

val SignedJWT.audience: List<String> get() = jwtClaimsSet.audience
val SignedJWT.issuer: String get() = jwtClaimsSet.issuer
val SignedJWT.subject: String get() = jwtClaimsSet.subject
val SignedJWT.claims: Map<String, Any> get() = jwtClaimsSet.claims

fun SignedJWT.verify(issuer: HttpUrl, jwkSetUri: HttpUrl): JWTClaimsSet {
    return DefaultJWTProcessor<SecurityContext?>()
        .apply {
            jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, RemoteJWKSet(jwkSetUri.toUrl()))
            jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(
                JWTClaimsSet.Builder()
                    .issuer(issuer.toString())
                    .build(),
                HashSet(
                    listOf("sub", "iss", "iat", "exp", "aud")
                )
            )
        }.process(this, null)
}

fun Response.toTokenResponse(): ParsedTokenResponse = ParsedTokenResponse(
    this.code,
    checkNotNull(this.body).string()
)

fun OkHttpClient.tokenRequest(url: HttpUrl, parameters: Map<String, String>): Response =
    tokenRequest(url, Headers.headersOf(), parameters)

fun OkHttpClient.tokenRequest(
    url: HttpUrl,
    headers: Headers,
    parameters: Map<String, String>
): Response =
    this.newCall(
        Request.Builder().post(
            url = url,
            headers = headers,
            parameters = parameters
        )
    ).execute()

fun OkHttpClient.tokenRequest(
    url: HttpUrl,
    basicAuth: Pair<String, String>,
    parameters: Map<String, String>
): Response =
    tokenRequest(
        url,
        Headers.headersOf("Authorization", Credentials.basic(basicAuth.first, basicAuth.second)),
        parameters
    )


fun Request.Builder.post(url: HttpUrl, headers: Headers, parameters: Map<String, String>) =
    this.url(url)
        .headers(headers)
        .post(FormBody.Builder().of(parameters))
        .build()

fun FormBody.Builder.of(parameters: Map<String, String>) =
    this.apply {
        parameters.forEach { (k, v) -> this.add(k, v) }
    }.build()
