package no.nav.security.mock.oauth2.templates

import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import java.io.StringWriter

data class HtmlContent(
    val template: String,
    val model: Any?
)

class TemplateMapper(
    private val config: Configuration
) {

    fun loginHtml(OAuth2HttpRequest: OAuth2HttpRequest): String =
        asString(HtmlContent("login.ftl", OAuth2HttpRequest))

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
