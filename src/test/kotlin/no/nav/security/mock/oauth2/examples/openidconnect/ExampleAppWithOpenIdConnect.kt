package no.nav.security.mock.oauth2.examples.openidconnect

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import mu.KotlinLogging
import no.nav.security.mock.oauth2.examples.AbstractExampleApp
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

private val log = KotlinLogging.logger {}

class ExampleAppWithOpenIdConnect(
    oidcDiscoveryUrl: String,
) : AbstractExampleApp(oidcDiscoveryUrl) {
    override fun handleRequest(request: RecordedRequest): MockResponse =
        when (request.requestUrl?.encodedPath) {
            "/login" -> {
                MockResponse()
                    .setResponseCode(302)
                    .setHeader("Location", authenticationRequest().toURI())
            }
            "/callback" -> {
                log.debug("got callback: $request")
                val code = request.requestUrl?.queryParameter("code")!!
                val tokenResponse =
                    oauth2Client
                        .newCall(
                            Request
                                .Builder()
                                .url(metadata.tokenEndpointURI.toURL())
                                .post(
                                    FormBody
                                        .Builder()
                                        .add("client_id", "client1")
                                        .add("code", code)
                                        .add("redirect_uri", exampleApp.url("/callback").toString())
                                        .add("grant_type", "authorization_code")
                                        .build(),
                                ).build(),
                        ).execute()
                val idToken: String = ObjectMapper().readValue<JsonNode>(tokenResponse.body!!.string()).get("id_token").textValue()
                val idTokenClaims: JWTClaimsSet = verifyJwt(idToken, metadata.issuer, retrieveJwks())
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Set-Cookie", "id_token=$idToken")
                    .setBody("logged in as ${idTokenClaims.subject}")
            }
            "/secured" -> {
                getCookies(request)["id_token"]
                    ?.let {
                        verifyJwt(it, metadata.issuer, retrieveJwks())
                    }?.let {
                        MockResponse()
                            .setResponseCode(200)
                            .setBody("welcome ${it.subject}")
                    } ?: MockResponse().setResponseCode(302).setHeader("Location", exampleApp.url("/login"))
            }
            else -> MockResponse().setResponseCode(404)
        }

    private fun getCookies(request: RecordedRequest): Map<String, String> =
        request
            .getHeader("Cookie")
            ?.split(";")
            ?.filter { it.contains("=") }
            ?.associate {
                val (key, value) = it.split("=")
                key.trim() to value.trim()
            } ?: emptyMap()

    private fun authenticationRequest(): AuthenticationRequest =
        AuthenticationRequest.parse(
            metadata.authorizationEndpointURI,
            mutableMapOf(
                "client_id" to listOf("client"),
                "response_type" to listOf("code"),
                "redirect_uri" to listOf(exampleApp.url("/callback").toString()),
                "response_mode" to listOf("query"),
                "scope" to listOf("openid scope1"),
                "state" to listOf("1234"),
                "nonce" to listOf("5678"),
            ),
        )
}
