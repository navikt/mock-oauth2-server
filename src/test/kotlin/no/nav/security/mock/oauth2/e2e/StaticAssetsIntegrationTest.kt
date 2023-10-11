package no.nav.security.mock.oauth2.e2e

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.testutils.client
import no.nav.security.mock.oauth2.testutils.get
import org.junit.jupiter.api.Test
import java.io.File

class StaticAssetsIntegrationTest {
    private val client = client()

    @Test
    fun `request to static asset should return file from static asset directory`() {
        val dir = File("./src/test/resources/static")
        val server = MockOAuth2Server(OAuth2Config(staticAssetsPath = dir.canonicalPath)).apply { start() }
        client.get(server.url("/static/test.txt")).asClue {
            it.code shouldBe 200
            it.headers["content-type"] shouldBe "text/plain"
        }
        client.get(server.url("/static/test.css")).asClue {
            it.code shouldBe 200
            it.headers["content-type"] shouldBe "text/css"
        }
        client.get(server.url("/static/test.js")).asClue {
            it.code shouldBe 200
            it.headers["content-type"] shouldBe "text/javascript"
        }
    }
}
