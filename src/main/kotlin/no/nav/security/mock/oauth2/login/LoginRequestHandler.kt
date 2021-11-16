package no.nav.security.mock.oauth2.login

import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.notFound
import no.nav.security.mock.oauth2.templates.TemplateMapper
import java.io.File
import java.io.FileNotFoundException

class LoginRequestHandler(private val templateMapper: TemplateMapper) {

    fun loginHtml(httpRequest: OAuth2HttpRequest, config: OAuth2Config): String =
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
        val username = checkNotNull(formParameters.get("username"))
        return Login(username, formParameters.get("claims"))
    }
}

data class Login(
    val username: String,
    val claims: String? = null
)
