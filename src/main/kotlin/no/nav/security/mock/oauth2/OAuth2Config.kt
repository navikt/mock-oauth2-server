package no.nav.security.mock.oauth2

import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import no.nav.security.mock.oauth2.token.TokenCallback

data class OAuth2Config(
    val interactiveLogin: Boolean,
    val tokenProvider: OAuth2TokenProvider,
    val tokenCallbacks: Set<TokenCallback>
)
