package no.nav.security.mock.oauth2.extensions

import mockwebserver3.RecordedRequest
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest

fun RecordedRequest.asOAuth2HttpRequest(): OAuth2HttpRequest =
    OAuth2HttpRequest(this.headers, checkNotNull(this.method), checkNotNull(this.requestUrl), this.body.copy().readUtf8())
