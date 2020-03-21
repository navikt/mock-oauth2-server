package no.nav.security.mock.oauth2

import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider

data class OAuth2Config(
    val interactiveLogin: Boolean = false,
    val tokenProvider: OAuth2TokenProvider = OAuth2TokenProvider(),
    val oAuth2TokenCallbacks: Set<OAuth2TokenCallback> = emptySet()
)
