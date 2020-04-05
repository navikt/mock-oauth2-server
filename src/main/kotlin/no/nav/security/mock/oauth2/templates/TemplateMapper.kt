package no.nav.security.mock.oauth2.templates

import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import no.nav.security.mock.oauth2.extensions.toTokenEndpointUrl
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import java.io.StringWriter

data class HtmlContent(
    val template: String,
    val model: Any?
)

class TemplateMapper(
    private val config: Configuration
) {

    fun loginHtml(oAuth2HttpRequest: OAuth2HttpRequest): String =
        asString(
            HtmlContent(
                "login.ftl", mapOf(
                    "request_url" to oAuth2HttpRequest.url.newBuilder().query(null).build().toString(),
                    "query" to OAuth2HttpRequest.Parameters(oAuth2HttpRequest.url.query).map
                )
            )
        )

    fun debuggerCallbackHtml(tokenRequest: String, tokenResponse: String): String {
        return asString(
            HtmlContent(
                "debugger_callback.ftl", mapOf(
                    "token_request" to tokenRequest,
                    "token_response" to tokenResponse
                )
            )
        )
    }

    fun debuggerFormHtml(oAuth2HttpRequest: OAuth2HttpRequest): String {
        val urlWithoutQuery = oAuth2HttpRequest.url.newBuilder().query(null)
        return asString(
            HtmlContent(
                "debugger.ftl", mapOf(
                    "url" to urlWithoutQuery,
                    "token_url" to oAuth2HttpRequest.url.toTokenEndpointUrl(),
                    "query" to OAuth2HttpRequest.Parameters(oAuth2HttpRequest.url.query).map
                )
            )
        )
    }

    fun authorizationCodeResponseHtml(redirectUri: String, code: String, state: String): String =
        asString(
            HtmlContent(
                "authorization_code_response.ftl", mapOf(
                    "redirect_uri" to redirectUri,
                    "code" to code,
                    "state" to state
                )
            )
        )

    private fun asString(htmlContent: HtmlContent): String =
        StringWriter().apply {
            config.getTemplate(htmlContent.template).process(htmlContent.model, this)
        }.toString()

    companion object {
        fun create(configure: Configuration.() -> Unit): TemplateMapper {
            val config = Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)
                .apply {
                    templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
                }
                .apply(configure)
            return TemplateMapper(config)
        }
    }
}
