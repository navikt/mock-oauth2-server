package no.nav.security.mock.oauth2.extensions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal fun String.keyValuesToMap(listDelimiter: String): Map<String, String> =
    this
        .split(listDelimiter)
        .filter { it.contains("=") }
        .associate {
            val (key, value) = it.split("=")
            key.urlDecode().trim() to value.urlDecode().trim()
        }

internal fun String.urlDecode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8)

internal fun String.parseJson(): Map<String, String> = jacksonObjectMapper().readValue(this)
