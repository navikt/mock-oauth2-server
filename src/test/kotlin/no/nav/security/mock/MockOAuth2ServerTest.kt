package no.nav.security.mock

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import no.nav.security.mock.oauth2.OAuth2TokenResponse
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.IOException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MockOAuth2ServerTest {
    private val client: OkHttpClient = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .build()

    private lateinit var triggerAuthCodeFlowUrl: HttpUrl

    private lateinit var server: MockOAuth2Server

    @BeforeEach
    fun before() {
        server = MockOAuth2Server()
        server.start()
        triggerAuthCodeFlowUrl = server.authorizationEndpointUrl("default").newBuilder()
            .addEncodedQueryParameter("client_id", "client1")
            .addEncodedQueryParameter("response_type", "code")
            .addEncodedQueryParameter("redirect_uri", "http://myapp/callback")
            .addEncodedQueryParameter("response_mode", "query")
            .addEncodedQueryParameter(
                "scope",
                "openid%20offline_access%20https%3A%2F%2Fgraph.microsoft.com%2Fuser.read"
            )
            .addEncodedQueryParameter("state", "1234")
            .addEncodedQueryParameter("nonce", "5678")
            .build()
    }

    @AfterEach
    fun shutdown() {
        server.shutdown()
    }

    @Test
    fun wellKnownUrl() {
        assertWellKnownResponseForIssuer("default")
        assertWellKnownResponseForIssuer("foobar")
    }

    private fun assertWellKnownResponseForIssuer(issuerId: String): String? {
        val request: Request = Request.Builder()
            .url(server.wellKnownUrl(issuerId))
            .get()
            .build()

        val responseBody: String? = client.newCall(request).execute().body?.string()
        assertThat(responseBody).contains(server.authorizationEndpointUrl(issuerId).toString())
        assertThat(responseBody).contains(server.tokenEndpointUrl(issuerId).toString())
        assertThat(responseBody).contains(server.jwksUrl(issuerId).toString())
        assertThat(responseBody).contains(server.issuerUrl(issuerId).toString())
        return responseBody
    }

    @Test
    fun authorize() {
        val request: Request = Request.Builder()
            .url(triggerAuthCodeFlowUrl)
            .get()
            .build()

        val response: Response = client.newCall(request).execute()
        assertThat(response.code).isEqualTo(302)
        val httpUrl: HttpUrl = checkNotNull(response.headers["location"]?.toHttpUrlOrNull())
        assertThat(httpUrl.queryParameter("state")).isEqualTo(
            triggerAuthCodeFlowUrl.queryParameter("state")
        )
        assertThat(httpUrl.queryParameter("code")).isNotBlank()
        assertThat(httpUrl.newBuilder().query(null).build()).isEqualTo(
            triggerAuthCodeFlowUrl.queryParameter("redirect_uri")?.toHttpUrlOrNull()
        )
    }

    @Test
    @Throws(IOException::class)
    fun tokenWithCode() {
        val authorizationCode = server.issueAuthorizationCodeForTest(
            AuthenticationRequest.parse(triggerAuthCodeFlowUrl.toUri())
        )
        val redirectUri = triggerAuthCodeFlowUrl.queryParameter("redirect_uri").toString()
        val formBody: RequestBody = FormBody.Builder()
            .add("scope", "scope1")
            .add("code", authorizationCode.value)
            .add("redirect_uri", redirectUri)
            .add("grant_type", "authorization_code")
            .build()
        val request: Request = Request.Builder()
            .url(server.tokenEndpointUrl("default"))
            .addHeader(
                "Authorization", Credentials.basic(
                    triggerAuthCodeFlowUrl.queryParameter("client_id")!!.toString(), "test"
                )
            )
            .post(formBody)
            .build()
        val response: Response = client.newCall(request).execute()
        assertThat(response.code).isEqualTo(200)
        val tokenResponse: OAuth2TokenResponse = jacksonObjectMapper().readValue(checkNotNull(response.body?.string()))
        assertThat(tokenResponse.accessToken).isNotNull()
        assertThat(tokenResponse.idToken).isNotNull()
        assertThat(tokenResponse.expiresIn).isGreaterThan(0)
        assertThat(tokenResponse.scope).isEqualTo("scope1")
        assertThat(tokenResponse.tokenType).isEqualTo("Bearer")
        assertThat(SignedJWT.parse(tokenResponse.idToken).jwtClaimsSet.audience.first()).isEqualTo("client1")
    }

    @Test
    fun noIssuerIdInUrl() {
        val request: Request = Request.Builder()
            .url(server.baseUrl())
            .get()
            .build()

        val responseBody: String? = client.newCall(request).execute().body?.string()
        println("body: $responseBody")
    }
    // TODO tests for exception handling
}