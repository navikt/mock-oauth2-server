package no.nav.security.mock.oauth2.extensions

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TemplateTest {
    @Test
    fun `template values in map should be replaced`()  {
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

    // Example usage
    fun main() {
        val jsonData: Map<String, Any> =
            mapOf(
                "myobject" to
                    mapOf(
                        "participantId" to "\${someTemlateVal}",
                        "actAs" to listOf("\${templateVal1}, \${templateVal2}"),
                        "readAs" to listOf("\${templateVal2}"),
                    ),
                "myobject2" to "someValue",
            )

        val templates =
            mapOf(
                "someTemlateVal" to "participant123",
                "templateVal1" to "actor123",
                "templateVal2" to "reader123",
            )

        val replacedData = jsonData.replaceValues(templates)
        println(replacedData)
    }

    /*fun replaceTemplates(data: Map<String, Any>, templates: Map<String, String>): Map<String, Any> {
        fun replaceValue(value: Any): Any {
            return when (value) {
                is String -> replaceTemplateString(value, templates)
                is List<*> -> value.map { replaceValue(it) }
                is Map<*, *> -> value.mapValues { replaceValue(it.value) }
                else -> value
            }
        }

        fun replaceTemplateString(value: String, templates: Map<String, String>): String {
            val regex = Regex("""\$\{(\w+)\}""")
            return regex.replace(value) { matchResult ->
                val key = matchResult.groupValues[1]
                templates[key] ?: matchResult.value
            }
        }

        return data.mapValues { replaceValue(it.value) }
    }*/
}
