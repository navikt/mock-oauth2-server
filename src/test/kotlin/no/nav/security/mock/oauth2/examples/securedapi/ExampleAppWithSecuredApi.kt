package no.nav.security.mock.oauth2.examples.securedapi

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.security.mock.oauth2.examples.AbstractExampleApp
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class ExampleAppWithSecuredApi(oauth2DiscoveryUrl: String) : AbstractExampleApp(oauth2DiscoveryUrl) {

    override fun handleRequest(request: RecordedRequest): MockResponse {
        return verifyOAuth2AccessToken(request)
            ?.let {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(greeting(it.subject))
            } ?: notAuthorized()
    }

    private fun greeting(subject: String): String =
        "{\n\"greeting\":\"welcome $subject\"\n}"

    private fun verifyOAuth2AccessToken(request: RecordedRequest): JWTClaimsSet? =
        request.headers["Authorization"]
            ?.split("Bearer ")
            ?.let {
                verifyJwt(it[0], metadata.issuer, retrieveJwks())
            }
}
