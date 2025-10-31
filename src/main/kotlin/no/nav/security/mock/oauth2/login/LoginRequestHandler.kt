package no.nav.security.mock.oauth2.login

import com.nimbusds.jwt.JWTClaimsSet
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.extensions.issuerId
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.missingParameter
import no.nav.security.mock.oauth2.notFound
import no.nav.security.mock.oauth2.templates.TemplateMapper
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID

private val log = KotlinLogging.logger { }

class LoginRequestHandler(
    private val templateMapper: TemplateMapper,
    private val config: OAuth2Config,
) {
    fun loginHtml(httpRequest: OAuth2HttpRequest): String =
        config.loginPagePath
            ?.let {
                try {
                    File(it).readText()
                } catch (e: FileNotFoundException) {
                    notFound("The configured loginPagePath '${config.loginPagePath}' is invalid, please ensure that it points to a valid html file")
                }
            }
            ?: templateMapper.loginHtml(httpRequest)

    fun loginSubmit(httpRequest: OAuth2HttpRequest): Login {
        val formParameters = httpRequest.formParameters
        val username = formParameters.get("username") ?: missingParameter("username")
        return Login(username, formParameters.get("claims"))
    }
}

data class Login(
    val username: String,
    val claims: String? = null,
) {
    companion object {
        private fun defaultLoginClaimSetBuilder(request: OAuth2HttpRequest): JWTClaimsSet.Builder {
            val issuerId = request.url.issuerId()
            val clientId = request.asAuthenticationRequest().clientID.value

            val claimsBuilder =
                JWTClaimsSet
                    .Builder()
                    .subject(UUID.randomUUID().toString())
                    .audience(clientId)

            when (issuerId) {
                "idporten" ->
                    claimsBuilder
                        .claim("pid", "01010199999")
                        .claim("amr", "mock-idp")
                        .claim("acr", "eidas-loa-high")
                "signicat" ->
                    claimsBuilder
                        .claim("nin", "01010199999")
                        .claim("idp_issuer", "mock-idp")
                else -> log.warn { "Unknown issuer $issuerId, cannot guarantee valid claims" }
            }
            return claimsBuilder
        }

        fun fromRequest(request: OAuth2HttpRequest): Login {
            val claimsBuilder = defaultLoginClaimSetBuilder(request)

            request
                .asAuthenticationRequest()
                .idTokenHint
                ?.jwtClaimsSet
                ?.writeTo(claimsBuilder)

            val claims = claimsBuilder.build()

            log.debug("authorizing user with claims: {}", claims)

            return Login(claims.subject, claims.toPayload().toString())
        }

        fun JWTClaimsSet.writeTo(builder: JWTClaimsSet.Builder) {
            this.claims.forEach { (key, value) -> builder.claim(key, value) }
        }
    }
}
