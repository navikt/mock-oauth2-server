package no.nav.security.mock.oauth2.extensions

/**
 * Replaces all template values denoted with ${key} in a map with the corresponding values from the templates map.
 *
 * @param templates a map of template values
 * @return a new map with all template values replaced
 */
fun Map<String, Any>.replaceValues(templates: Map<String, Any>): Map<String, Any> {
    fun replaceTemplateString(
        value: String,
        templates: Map<String, Any>,
    ): String {
        val regex = Regex("""\$\{(\w+)\}""")
        return regex.replace(value) { matchResult ->
            val key = matchResult.groupValues[1]
            templates[key]?.toString() ?: matchResult.value
        }
    }

    fun replaceValue(value: Any): Any =
        when (value) {
            is String -> replaceTemplateString(value, templates)
            is List<*> -> value.map { it?.let { replaceValue(it) } }
            is Map<*, *> -> value.mapValues { v -> v.value?.let { replaceValue(it) } }
            else -> value
        }

    return this.mapValues { replaceValue(it.value) }
}
