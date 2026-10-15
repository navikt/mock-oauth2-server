package no.nav.security.mock.oauth2.extensions

/**
 * Replaces all template values denoted with ${key} in a map with the corresponding values from the templates map.
 *
 * Indexed access to multi-valued parameters is supported via ${key[i]} (zero-based): the
 * template value is split on [valueSeparator], segments are trimmed of surrounding
 * whitespace, and the i-th segment replaces the placeholder. An out-of-range or
 * non-numeric index leaves the placeholder unreplaced.
 *
 * The [valueSeparator] defaults to a single space, matching how repeated authorize
 * parameters are joined in [no.nav.security.mock.oauth2.OAuth2Config.authorizeParamValueSeparator].
 * It can be overridden when a different join convention is in use.
 *
 * @param templates a map of template values
 * @param valueSeparator separator used to split a template value for `${key[i]}` indexing
 * @return a new map with all template values replaced
 */
fun Map<String, Any>.replaceValues(
    templates: Map<String, Any>,
    valueSeparator: String = " ",
): Map<String, Any> {
    fun replaceTemplateString(
        value: String,
        templates: Map<String, Any>,
    ): String {
        val regex = Regex("""\$\{(\w+)(?:\[(\d+)\])?\}""")
        return regex.replace(value) { matchResult ->
            val key = matchResult.groupValues[1]
            val index = matchResult.groupValues[2]
            val template = templates[key]?.toString() ?: return@replace matchResult.value
            if (index.isEmpty()) {
                template
            } else {
                val safeIndex = index.toIntOrNull() ?: return@replace matchResult.value
                template.split(valueSeparator).map { it.trim() }.getOrNull(safeIndex) ?: matchResult.value
            }
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
