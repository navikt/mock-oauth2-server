package no.nav.security.mock.oauth2.login

import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.templates.TemplateMapper
import no.nav.security.mock.oauth2.templates.TemplateMapper.Companion.create

val templateMapper: TemplateMapper = create{}

class LoginRequestHandler {

    fun loginHtml(httpRequest: OAuth2HttpRequest): String = templateMapper.loginHtml(httpRequest)

    fun loginSubmit(httpRequest: OAuth2HttpRequest): Login {
        val formParameters = httpRequest.formParameters
        val username = checkNotNull(formParameters.get("username"))
        return Login(username, formParameters.get("acr"))
    }
}

data class Login(
    val username: String,
    val acr: String? = null
)
