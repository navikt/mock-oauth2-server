package no.nav.security.mock.oauth2.extensions

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
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
}
