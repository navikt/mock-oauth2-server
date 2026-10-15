package no.nav.security.mock.oauth2.extensions

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class StringExtensionsTest {
    @Test
    fun `keyValuesToMap should keep everything after the first equals sign in the value`() {
        "a=b=c&d=e".keyValuesToMap("&") shouldBe mapOf("a" to "b=c", "d" to "e")
    }

    @Test
    fun `keyValuesToMap should url decode keys and values`() {
        "my%20key=a%3Db&x=y+z".keyValuesToMap("&") shouldBe mapOf("my key" to "a=b", "x" to "y z")
    }

    @Test
    fun `keyValuesToMap should parse cookie header with base64 padding in value`() {
        "session=YWJj==; other=1".keyValuesToMap(";") shouldBe mapOf("session" to "YWJj==", "other" to "1")
    }
}
