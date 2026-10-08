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

    @Test
    fun `indexed template values resolve comma-separated segments trimmed`() {
        mapOf(
            "email" to "\${login_hint[0]}",
            "id" to "\${login_hint[1]}",
        ).replaceValues(mapOf("login_hint" to "anna@example.com, X111111111")).asClue {
            it["email"] shouldBe "anna@example.com"
            it["id"] shouldBe "X111111111"
        }
    }

    @Test
    fun `out-of-range index leaves placeholder unreplaced`() {
        mapOf("id" to "\${login_hint[5]}").replaceValues(mapOf("login_hint" to "a,b")).asClue {
            it["id"] shouldBe "\${login_hint[5]}"
        }
    }

    @Test
    fun `indexed access on value without separator resolves index zero and leaves rest unreplaced`() {
        mapOf(
            "first" to "\${login_hint[0]}",
            "second" to "\${login_hint[1]}",
        ).replaceValues(mapOf("login_hint" to "single")).asClue {
            it["first"] shouldBe "single"
            it["second"] shouldBe "\${login_hint[1]}"
        }
    }

    @Test
    fun `missing key with index leaves placeholder unreplaced`() {
        mapOf("id" to "\${unknown[0]}").replaceValues(emptyMap()).asClue {
            it["id"] shouldBe "\${unknown[0]}"
        }
    }
}
