package no.nav.security.mock.oauth2

import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback

data class OAuth2Config(
    val interactiveLogin: Boolean,
    val tokenProvider: OAuth2TokenProvider,
    val OAuth2TokenCallbacks: Set<OAuth2TokenCallback>
)
