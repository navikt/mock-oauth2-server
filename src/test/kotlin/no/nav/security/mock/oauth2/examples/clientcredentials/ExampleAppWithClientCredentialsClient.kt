package no.nav.security.mock.oauth2.examples.clientcredentials

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.security.mock.oauth2.examples.AbstractExampleApp
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class ExampleAppWithClientCredentialsClient(
    oauth2DiscoveryUrl: String,
) : AbstractExampleApp(oauth2DiscoveryUrl) {
    override fun handleRequest(request: RecordedRequest): MockResponse =
        getClientCredentialsAccessToken()
            ?.let {
                MockResponse()
                    .setResponseCode(200)
                    .setBody("token=$it")
            }
            ?: MockResponse().setResponseCode(500).setBody("could not get access_token")

    private fun getClientCredentialsAccessToken(): String? {
        val tokenResponse: Response =
            oauth2Client
                .newCall(
                    Request
                        .Builder()
                        .url(metadata.tokenEndpointURI.toURL())
                        .addHeader("Authorization", Credentials.basic("ExampleAppWithClientCredentialsClient", "test"))
                        .post(
                            FormBody
                                .Builder()
                                .add("client_id", "ExampleAppWithClientCredentialsClient")
                                .add("scope", "scope1")
                                .add("grant_type", "client_credentials")
                                .build(),
                        ).build(),
                ).execute()
        return tokenResponse.body?.string()?.let {
            ObjectMapper().readValue<JsonNode>(it).get("access_token")?.textValue()
        }
    }
}
