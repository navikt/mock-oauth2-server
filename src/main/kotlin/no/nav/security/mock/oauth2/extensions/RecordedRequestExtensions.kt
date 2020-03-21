package no.nav.security.mock.oauth2.extensions

import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import okhttp3.mockwebserver.RecordedRequest

fun RecordedRequest.asOAuth2HttpRequest(): OAuth2HttpRequest =
    OAuth2HttpRequest(this.headers, this.method!!, this.requestUrl!!, this.body.readUtf8())
