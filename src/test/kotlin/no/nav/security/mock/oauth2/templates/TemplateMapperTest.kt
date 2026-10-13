package no.nav.security.mock.oauth2.templates

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.security.mock.oauth2.http.templateMapper
import org.junit.jupiter.api.Test

class TemplateMapperTest {
    @Test
    fun `authorization form escapes values in HTML attributes`() {
        val html = templateMapper.authorizationCodeResponseHtml("https://example.test/?x=\"onfocus=", "code", "state")

        html shouldContain "https://example.test/?x=&quot;onfocus="
        html shouldNotContain "x=\"onfocus="
    }

    @Test
    fun `debugger callback escapes token request and response`() {
        val html = templateMapper.debuggerCallbackHtml("<script>request</script>", "<script>response</script>")

        html shouldContain "&lt;script&gt;request&lt;/script&gt;"
        html shouldContain "&lt;script&gt;response&lt;/script&gt;"
        html shouldNotContain "<script>request</script>"
        html shouldNotContain "<script>response</script>"
    }
}
