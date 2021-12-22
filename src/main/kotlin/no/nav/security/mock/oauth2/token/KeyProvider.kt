package no.nav.security.mock.oauth2.token

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import mu.KotlinLogging
import java.io.File
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingDeque

private val log = KotlinLogging.logger { }

open class KeyProvider @JvmOverloads constructor(
    private val initialKeys: List<RSAKey> = keysFromFile(INITIAL_KEYS_FILE),
    private val initialMappedKeys: Map<String, RSAKey> = mappedKeysFromFile()
) {

    private val generator = KeyPairGenerator.getInstance("RSA").apply { this.initialize(2048) }

    private val keyDeque = LinkedBlockingDeque<RSAKey>().apply {
        initialKeys.forEach {
            put(it)
        }
    }

    private val signingKeys: ConcurrentHashMap<String, RSAKey> = ConcurrentHashMap<String, RSAKey>().apply {
        this.putAll(initialMappedKeys)
    }

    fun signingKey(keyId: String): RSAKey = signingKeys.computeIfAbsent(keyId) { keyFromDequeOrNew(keyId) }

    private fun keyFromDequeOrNew(keyId: String): RSAKey = keyDeque.poll()?.let {
        RSAKey.Builder(it).keyID(keyId).build()
    } ?: generator.generateRSAKey(keyId)

    companion object {
        const val INITIAL_KEYS_FILE = "/mock-oauth2-server-keys.json"

        fun keysFromFile(filename: String): List<RSAKey> {
            val keysFromFile = KeyProvider::class.java.getResource(filename)
            if (keysFromFile != null) {
                return JWKSet.parse(keysFromFile.readText()).keys.map { it as RSAKey }
            }
            return emptyList()
        }

        /**
         * Expecting a json file with content like `{"issuer1": <JWK>, "issuer2": null, "issuer3: <JWK> }` etc...
         * if the issuer entry has value null, then generate a key which is also logged.
         */
        private fun mappedKeysFromFile(explicitFilename: String? = null): Map<String, RSAKey> {
            val om = jacksonObjectMapper()
            val generator = KeyPairGenerator.getInstance("RSA").apply { this.initialize(2048) }
            val out = mutableMapOf<String, RSAKey>()
            val environmentFilename = System.getenv()["PREDEFINED_ISSUER_JWKS"] ?: System.getProperty("PREDEFINED_ISSUER_JWKS")

            val prioritizedFile: File = when {
                (explicitFilename != null) -> File(explicitFilename)
                (environmentFilename != null) -> File(environmentFilename)
                (File("iss2jwk.json").exists()) -> {
                    log.debug("Found default config file for predefined issuer JWKs")
                    File("iss2jwk.json")
                }
                else -> return emptyMap()
            }
            try {
                val predefinedIssuerJwkConfig = om.readValue(prioritizedFile, ObjectNode::class.java)
                predefinedIssuerJwkConfig.fields().forEach {
                    val issuerName = it.key
                    val jwkString = it.value
                    if (!jwkString.isNull) {
                        try {
                            val parsedJWK = JWK.parse(jwkString.toString()).toRSAKey()
                            if (!parsedJWK.keyID.equals(issuerName)) {
                                throw RuntimeException("Error when parsing JWK for issuer '$issuerName'. kid must match issuer name")
                            }
                            out[issuerName] = parsedJWK
                        } catch (ex: Exception) {
                            throw RuntimeException("Error when parsing JWK for issuer '$issuerName'", ex)
                        }
                    } else {
                        val generated = generator.generateRSAKey(issuerName).apply {
                            val prettyJson = om.writerWithDefaultPrettyPrinter().writeValueAsString(this.toJSONObject())
                            log.debug("Created JWK for issuer '$issuerName' => \n${prettyJson}\n")
                        }
                        out.put(issuerName, generated)
                    }
                }
            } catch (ex: Exception) {
                throw IllegalStateException("Could not add premapped issuer JWKs from file ${prioritizedFile.absolutePath}", ex)
            }
            return out.toMap()
        }
    }
}

private fun KeyPairGenerator.generateRSAKey(keyId: String): RSAKey =
    generateKeyPair()
        .let {
            RSAKey.Builder(it.public as RSAPublicKey)
                .privateKey(it.private as RSAPrivateKey)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(keyId)
                .build()
        }
