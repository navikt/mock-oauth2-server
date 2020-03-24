package no.nav.security.mock.oauth2.examples.securedapi

import no.nav.security.mock.oauth2.examples.AbstractExampleApp
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class ExampleAppWithSecuredApi(oauth2DiscoveryUrl: String) : AbstractExampleApp(oauth2DiscoveryUrl) {

    override fun handleRequest(request: RecordedRequest): MockResponse {
        return bearerToken(request)
            ?.let {
                verifyJwt(it, metadata.issuer, retrieveJwks())
            }?.let {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(greeting(it.subject))
            } ?: notAuthorized()
    }

    private fun greeting(subject: String): String =
        "{\n\"greeting\":\"welcome $subject\"\n}"
}
