package no.nav.security.mock.oauth2.login

import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.templates.TemplateMapper

class LoginRequestHandler(private val templateMapper: TemplateMapper) {

    fun loginHtml(httpRequest: OAuth2HttpRequest): String = templateMapper.loginHtml(httpRequest)

    fun loginSubmit(httpRequest: OAuth2HttpRequest): Login {
        val formParameters = httpRequest.formParameters
        val username = checkNotNull(formParameters.get("username"))
        return Login(username, formParameters.get("claims"))
    }
}

data class Login(
    val username: String,
    val claims: String? = null
)
