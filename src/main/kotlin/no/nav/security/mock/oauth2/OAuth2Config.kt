package no.nav.security.mock.oauth2

import no.nav.security.mock.oauth2.http.MockWebServerWrapper
import no.nav.security.mock.oauth2.http.OAuth2HttpServer
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider

data class OAuth2Config @JvmOverloads constructor(
    val interactiveLogin: Boolean = false,
    val tokenProvider: OAuth2TokenProvider = OAuth2TokenProvider(),
    val tokenCallbacks: Set<OAuth2TokenCallback> = emptySet(),
    val httpServer: OAuth2HttpServer = MockWebServerWrapper()
)
