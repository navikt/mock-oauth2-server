package no.nav.security.mock.oauth2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.GrantType
import no.nav.security.mock.oauth2.http.OAuth2TokenResponse
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
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
import java.net.URLEncoder

// TODO add more tests for exception handling
class MockOAuth2ServerTest {
    private val client: OkHttpClient = OkHttpClient()
        .newBuilder()
        .followRedirects(false)
        .build()

    private lateinit var server: MockOAuth2Server
    private lateinit var interactiveLoginServer: MockOAuth2Server

    @BeforeEach
    fun before() {
        server = MockOAuth2Server()
        server.start()
        interactiveLoginServer = MockOAuth2Server(
            OAuth2Config(
                interactiveLogin = true,
                OAuth2TokenCallbacks = emptySet(),
                tokenProvider = OAuth2TokenProvider()
            )
        )
    }

    @AfterEach
    fun shutdown() {
        server.shutdown()
        interactiveLoginServer.shutdown()
    }

    @Test
    fun wellKnownUrlForMultipleIssuers() {
        assertWellKnownResponseForIssuer("default")
        assertWellKnownResponseForIssuer("foo")
        assertWellKnownResponseForIssuer("bar")
    }

    @Test
    fun noIssuerIdInUrlShouldReturn404() {
        val request: Request = Request.Builder()
            .url(server.baseUrl().newBuilder().addPathSegments("/.well-known/openid-configuration").build())
            .get()
            .build()

        assertThat(client.newCall(request).execute().code).isEqualTo(404)
    }

    @Test
    fun startAuthorizationCodeFlow() {
        val authorizationCodeFlowUrl = authorizationCodeFlowUrl(
            "default",
            "client1",
            "http://myapp/callback",
            "openid scope1"
        )
        val request: Request = Request.Builder()
            .url(authorizationCodeFlowUrl)
            .get()
            .build()

        val response: Response = client.newCall(request).execute()
        assertThat(response.code).isEqualTo(302)
        val httpUrl: HttpUrl = checkNotNull(response.headers["location"]?.toHttpUrlOrNull())
        assertThat(httpUrl.queryParameter("state")).isEqualTo(
            authorizationCodeFlowUrl.queryParameter("state")
        )
        assertThat(httpUrl.queryParameter("code")).isNotBlank()
        assertThat(httpUrl.newBuilder().query(null).build()).isEqualTo(
            authorizationCodeFlowUrl.queryParameter("redirect_uri")?.toHttpUrlOrNull()
        )
    }

    @Test
    fun fullAuthorizationCodeFlow() {
        val authorizationCodeFlowUrl = authorizationCodeFlowUrl(
            "default",
            "client1",
            "http://myapp/callback",
            "openid scope1"
        )
        val request: Request = Request.Builder()
            .url(authorizationCodeFlowUrl)
            .get()
            .build()

        val response: Response = client.newCall(request).execute()
        val url: HttpUrl = checkNotNull(response.headers["location"]?.toHttpUrlOrNull())
        val code = checkNotNull(url.queryParameter("code"))
        val tokenResponse: Response = client.newCall(
            authCodeTokenRequest(
                server.tokenEndpointUrl("default"),
                "client1",
                "https://myapp/callback",
                "openid scope1",
                code
            )
        ).execute()
        assertThat(tokenResponse.code).isEqualTo(200)
        val oAuth2TokenResponse: OAuth2TokenResponse = jacksonObjectMapper().readValue(checkNotNull(tokenResponse.body?.string()))
        assertThat(oAuth2TokenResponse.accessToken).isNotNull()
        assertThat(oAuth2TokenResponse.idToken).isNotNull()
        assertThat(oAuth2TokenResponse.expiresIn).isGreaterThan(0)
        assertThat(oAuth2TokenResponse.scope).contains("openid scope1")
        assertThat(oAuth2TokenResponse.tokenType).isEqualTo("Bearer")
        val idToken: SignedJWT = SignedJWT.parse(oAuth2TokenResponse.idToken)
        val accessToken: SignedJWT = SignedJWT.parse(oAuth2TokenResponse.accessToken)
        assertThat(idToken.jwtClaimsSet.audience.first()).isEqualTo("client1")
        assertThat(accessToken.jwtClaimsSet.audience).containsExactly("scope1")
    }

    @Test
    fun fullAuthorizationCodeFlowWithInteractiveLogin() {
        interactiveLoginServer.start()
        val authorizationCodeFlowUrl = authorizationCodeFlowUrl(
            interactiveLoginServer.authorizationEndpointUrl("default"),
            "client1",
            "http://myapp/callback",
            "openid scope1"
        )

        val authEndpointResponse: Response = client.newCall(
            Request.Builder()
                .url(authorizationCodeFlowUrl)
                .get()
                .build()
        ).execute()
        assertThat(authEndpointResponse.headers["Content-Type"]).isEqualTo("text/html;charset=UTF-8")
        val expectedSubject = "foo"
        val loginResponse: Response = client.newCall(loginSubmitRequest(authorizationCodeFlowUrl, expectedSubject)).execute()
        assertThat(loginResponse.code).isEqualTo(302)
        val url: HttpUrl = checkNotNull(loginResponse.headers["location"]?.toHttpUrlOrNull())
        val code = checkNotNull(url.queryParameter("code"))
        val tokenResponse: Response = client.newCall(
            authCodeTokenRequest(
                interactiveLoginServer.tokenEndpointUrl("default"),
                "client1",
                "https://myapp/callback",
                "openid scope1",
                code
            )
        ).execute()
        assertThat(tokenResponse.code).isEqualTo(200)
        val oAuth2TokenResponse: OAuth2TokenResponse = jacksonObjectMapper().readValue(checkNotNull(tokenResponse.body?.string()))
        assertThat(oAuth2TokenResponse.accessToken).isNotNull()
        assertThat(oAuth2TokenResponse.idToken).isNotNull()
        assertThat(oAuth2TokenResponse.expiresIn).isGreaterThan(0)
        assertThat(oAuth2TokenResponse.scope).contains("openid scope1")
        assertThat(oAuth2TokenResponse.tokenType).isEqualTo("Bearer")
        val idToken: SignedJWT = SignedJWT.parse(oAuth2TokenResponse.idToken)
        val accessToken: SignedJWT = SignedJWT.parse(oAuth2TokenResponse.accessToken)
        assertThat(idToken.jwtClaimsSet.subject).isEqualTo("foo")
        assertThat(idToken.jwtClaimsSet.audience.first()).isEqualTo("client1")
        assertThat(accessToken.jwtClaimsSet.audience).containsExactly("scope1")
        interactiveLoginServer.shutdown()
    }

    @Test
    @Throws(IOException::class)
    fun tokenRequestWithCodeShouldReturnTokensWithDefaultClaims() {
        val response: Response = client.newCall(
            authCodeTokenRequest(
                server.tokenEndpointUrl("default"),
                "client1",
                "https://myapp/callback",
                "openid scope1",
                "123"
            )
        ).execute()

        assertThat(response.code).isEqualTo(200)
        val tokenResponse: OAuth2TokenResponse = jacksonObjectMapper().readValue(checkNotNull(response.body?.string()))
        assertThat(tokenResponse.accessToken).isNotNull()
        assertThat(tokenResponse.idToken).isNotNull()
        assertThat(tokenResponse.expiresIn).isGreaterThan(0)
        assertThat(tokenResponse.scope).contains("openid scope1")
        assertThat(tokenResponse.tokenType).isEqualTo("Bearer")
        val idToken: SignedJWT = SignedJWT.parse(tokenResponse.idToken)
        val accessToken: SignedJWT = SignedJWT.parse(tokenResponse.accessToken)
        assertThat(idToken.jwtClaimsSet.audience.first()).isEqualTo("client1")
        assertThat(accessToken.jwtClaimsSet.audience).containsExactly("scope1")
    }

    @Test
    @Throws(IOException::class)
    fun tokenWithCodeShouldReturnTokensWithClaimsFromEnqueuedCallback() {
        server.enqueueCallback(
            DefaultOAuth2TokenCallback(
                issuerId = "custom",
                subject = "yolo",
                audience = "myaud"
            )
        )

        val response: Response = client.newCall(
            authCodeTokenRequest(
                server.tokenEndpointUrl("custom"),
                "client1",
                "https://myapp/callback",
                "openid scope1",
                "123"
            )
        ).execute()

        assertThat(response.code).isEqualTo(200)
        val tokenResponse: OAuth2TokenResponse = jacksonObjectMapper().readValue(checkNotNull(response.body?.string()))
        assertThat(tokenResponse.accessToken).isNotNull()
        assertThat(tokenResponse.idToken).isNotNull()
        assertThat(tokenResponse.expiresIn).isGreaterThan(0)
        assertThat(tokenResponse.scope).contains("openid scope1")
        assertThat(tokenResponse.tokenType).isEqualTo("Bearer")
        val idToken: SignedJWT = SignedJWT.parse(tokenResponse.idToken)
        assertThat(idToken.jwtClaimsSet.audience.first()).isEqualTo("client1")
        val accessToken: SignedJWT = SignedJWT.parse(tokenResponse.accessToken)
        assertThat(accessToken.jwtClaimsSet.audience).containsExactly("myaud")
        assertThat(accessToken.jwtClaimsSet.subject).isEqualTo("yolo")
        assertThat(accessToken.jwtClaimsSet.issuer).endsWith("custom")
    }

    @Test
    fun tokenRequestForjwtBearerGrant() {
        val signedJWT = server.issueToken("default", "client1", DefaultOAuth2TokenCallback())
        val response: Response = client.newCall(
            jwtBearerGrantTokenRequest(
                "default",
                "client1",
                "scope1",
                signedJWT.serialize()
            )
        ).execute()

        assertThat(response.code).isEqualTo(200)
        val tokenResponse: OAuth2TokenResponse = jacksonObjectMapper().readValue(checkNotNull(response.body?.string()))
        assertThat(tokenResponse.accessToken).isNotNull()
        assertThat(tokenResponse.idToken).isNull()
        assertThat(tokenResponse.expiresIn).isGreaterThan(0)
        assertThat(tokenResponse.scope).contains("scope1")
        assertThat(tokenResponse.tokenType).isEqualTo("Bearer")
        val accessToken: SignedJWT = SignedJWT.parse(tokenResponse.accessToken)
        assertThat(accessToken.jwtClaimsSet.audience).containsExactly("scope1")
        assertThat(accessToken.jwtClaimsSet.issuer).endsWith("default")
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

    private fun loginSubmitRequest(url: HttpUrl, username: String): Request {
        val formBody: RequestBody = FormBody.Builder()
            .add("username", username)
            .build()
        return Request.Builder()
            .url(url)
            .post(formBody)
            .build()
    }

    private fun jwtBearerGrantTokenRequest(
        issuerId: String,
        clientId: String,
        scope: String,
        assertion: String
    ): Request {
        val formBody: RequestBody = FormBody.Builder()
            .add("scope", scope)
            .add("assertion", assertion)
            .add("grant_type", GrantType.JWT_BEARER.value)
            .add("requested_token_use", "on_behalf_of")
            .build()
        return Request.Builder()
            .url(server.tokenEndpointUrl(issuerId))
            .addHeader("Authorization", Credentials.basic(clientId, "test"))
            .post(formBody)
            .build()
    }

    private fun authCodeTokenRequest(
        tokenEndpointUrl: HttpUrl,
        clientId: String,
        redirectUri: String,
        scope: String,
        code: String
    ): Request {
        val formBody: RequestBody = FormBody.Builder()
            .add("scope", scope)
            .add("code", code)
            .add("redirect_uri", redirectUri)
            .add("grant_type", "authorization_code")
            .build()
        return Request.Builder()
            .url(tokenEndpointUrl)
            .addHeader("Authorization", Credentials.basic(clientId, "test"))
            .post(formBody)
            .build()
    }

    private fun authorizationCodeFlowUrl(
        issuerId: String,
        clientId: String,
        redirectUri: String,
        scope: String
    ): HttpUrl = authorizationCodeFlowUrl(
        server.authorizationEndpointUrl(issuerId),
        clientId,
        redirectUri,
        scope
    )

    private fun authorizationCodeFlowUrl(
        authEndpointUrl: HttpUrl,
        clientId: String,
        redirectUri: String,
        scope: String
    ): HttpUrl {
        return authEndpointUrl.newBuilder()
            .addEncodedQueryParameter("client_id", clientId)
            .addEncodedQueryParameter("response_type", "code")
            .addEncodedQueryParameter("redirect_uri", redirectUri)
            .addEncodedQueryParameter("response_mode", "query")
            .addEncodedQueryParameter("scope", URLEncoder.encode(scope, "UTF-8"))
            .addEncodedQueryParameter("state", "1234")
            .addEncodedQueryParameter("nonce", "5678")
            .build()
    }
}
