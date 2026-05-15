package no.nav.security.mock.oauth2.extensions

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test

class TemplateTest {
    @Test
    fun `template values in map should be replaced`() {
        val templates =
            mapOf(
                "templateVal1" to "val1",
                "templateVal2" to "val2",
                "templateListVal" to "listVal1",
            )

        mapOf(
            "object1" to mapOf("key1" to "\${templateVal1}"),
            "object2" to "\${templateVal2}",
            "nestedObject" to mapOf("nestedKey" to mapOf("nestedKeyAgain" to "\${templateVal2}")),
            "list1" to listOf("\${templateListVal}"),
        ).replaceValues(templates).asClue {
            it["object1"] shouldBe mapOf("key1" to "val1")
            it["list1"] shouldBe listOf("listVal1")
            println(it)
        }
    }

    @Test
    fun `built-in uuid template variable should resolve to a valid UUID`() {
        val uuidRegex = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        val result = mapOf("sub" to "\${uuid}").replaceValues(emptyMap())
        val sub = result["sub"] as String
        sub shouldMatch uuidRegex
    }

    @Test
    fun `built-in uuid template variable should produce the same value within one replaceValues call`() {
        val result = mapOf("sub" to "\${uuid}", "jti" to "\${uuid}").replaceValues(emptyMap())
        result["sub"] shouldBe result["jti"]
    }

    @Test
    fun `built-in uuid template variable should produce a different value on each replaceValues call`() {
        val first = mapOf("sub" to "\${uuid}").replaceValues(emptyMap())["sub"]
        val second = mapOf("sub" to "\${uuid}").replaceValues(emptyMap())["sub"]
        first shouldNotBe second
    }

    @Test
    fun `caller-supplied templates should override built-in uuid`() {
        val fixedUuid = "00000000-0000-0000-0000-000000000001"
        val result = mapOf("sub" to "\${uuid}").replaceValues(mapOf("uuid" to fixedUuid))
        result["sub"] shouldBe fixedUuid
    }
}
