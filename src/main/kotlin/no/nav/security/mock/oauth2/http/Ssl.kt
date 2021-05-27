package no.nav.security.mock.oauth2.http

import java.io.File
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine

class Ssl @JvmOverloads constructor(
    private val keystore: String,
    private val keystorePassword: String = "",
    private val keystoreType: String = "PKCS12",
) {
    fun sslEngine(): SSLEngine = sslContext().createSSLEngine().apply {
        useClientMode = false
        needClientAuth = false
    }

    internal fun sslContext(): SSLContext {
        val keyStore = keyStore()
        val keyManager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, keystorePassword.toCharArray())
        }
        return SSLContext.getInstance("TLS").apply {
            init(keyManager.keyManagers, null, null)
        }
    }

    internal fun keyStore() = KeyStore.getInstance(keystoreType).apply {
        File(keystore).inputStream().use {
            load(it, keystorePassword.toCharArray())
        }
    }
}
