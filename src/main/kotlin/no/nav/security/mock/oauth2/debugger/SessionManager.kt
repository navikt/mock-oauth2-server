package no.nav.security.mock.oauth2.debugger

import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.DirectDecrypter
import com.nimbusds.jose.crypto.DirectEncrypter
import mu.KotlinLogging
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.objectMapper
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

private val log = KotlinLogging.logger { }

class SessionManager {
    private val encryptionKey: SecretKey =
        KeyGenerator.getInstance("AES")
            .apply { this.init(128) }.generateKey()

    fun session(request: OAuth2HttpRequest): Session = Session(encryptionKey, request)

    class Session(
        private val encryptionKey: SecretKey,
        val request: OAuth2HttpRequest,
    ) {
        val parameters: MutableMap<String, String> = getSessionCookie() ?.let { objectMapper.readValue(it) } ?: mutableMapOf()

        fun putAll(map: Map<String, String>) = parameters.putAll(map)

        operator fun get(key: String): String = parameters[key] ?: throw RuntimeException("could not get $key from session.")
        operator fun set(key: String, value: String) = parameters.put(key, value)

        fun asCookie(): String = objectMapper.writeValueAsString(parameters).encrypt(encryptionKey).let {
            "$DEBUGGER_SESSION_COOKIE=$it; HttpOnly; Path=/"
        }

        private fun String.encrypt(key: SecretKey): String =
            JWEObject(
                JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A128GCM),
                Payload(this),
            ).also {
                it.encrypt(DirectEncrypter(key))
            }.serialize()

        private fun String.decrypt(key: SecretKey): String =
            JWEObject.parse(this).also {
                it.decrypt(DirectDecrypter(key))
            }.payload.toString()

        private fun getSessionCookie(): String? =
            runCatching {
                request.cookies[DEBUGGER_SESSION_COOKIE]?.decrypt(encryptionKey)
            }.fold(
                onSuccess = { result -> result },
                onFailure = { error ->
                    log.error("received exception when decrypting cookie", error)
                    null
                },
            )
        companion object {
            const val DEBUGGER_SESSION_COOKIE = "debugger-session"
        }
    }
}
