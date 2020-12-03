package no.nav.security.mock.oauth2.debugger

import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.DirectDecrypter
import com.nimbusds.jose.crypto.DirectEncrypter
import com.nimbusds.oauth2.sdk.OAuth2Error
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.extensions.removeAllEncodedQueryParams
import no.nav.security.mock.oauth2.extensions.toAuthorizationEndpointUrl
import no.nav.security.mock.oauth2.extensions.toDebuggerCallbackUrl
import no.nav.security.mock.oauth2.extensions.toDebuggerUrl
import no.nav.security.mock.oauth2.http.OAuth2HttpRequest
import no.nav.security.mock.oauth2.http.OAuth2HttpResponse
import no.nav.security.mock.oauth2.http.html
import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.security.mock.oauth2.http.redirect
import no.nav.security.mock.oauth2.templates.TemplateMapper
import okhttp3.Credentials
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.toHostHeader

private val log = KotlinLogging.logger { }

private val client: OkHttpClient = OkHttpClient()
    .newBuilder()
    .followRedirects(false)
    .build()

class DebuggerRequestHandler(private val templateMapper: TemplateMapper) {

    private val encryptionKey: SecretKey =
        KeyGenerator.getInstance("AES")
            .apply { this.init(128) }.generateKey()

    fun handleDebuggerForm(request: OAuth2HttpRequest): OAuth2HttpResponse {
        return when (request.method) {
            "GET" -> {
                log.debug("handling GET request, return html form")
                html(
                    templateMapper.debuggerFormHtml(
                        debuggerAuthorizationRequest(request.url.toAuthorizationEndpointUrl(), request.url.toDebuggerCallbackUrl()),
                        ClientAuthMethod.CLIENT_SECRET_BASIC.name
                    )
                )
            }
            "POST" -> {
                log.debug("handling POST request, return redirect")
                val authorizeUrl = request.formParameters.get("authorize_url") ?: error("authorize_url is missing")
                val httpUrl = authorizeUrl.toHttpUrl().newBuilder()
                    .encodedQuery(request.formParameters.parameterString)
                    .removeAllEncodedQueryParams("authorize_url", "token_url", "client_secret", "client_auth_method")
                    .build()

                log.debug("attempting to redirect to $httpUrl, setting received params in encrypted cookie")
                val cookieValue = objectMapper.writeValueAsString(request.formParameters.map).encrypt(encryptionKey)
                redirect(httpUrl.toString(), Headers.headersOf("Set-Cookie", "$DEBUGGER_SESSION_COOKIE=$cookieValue; HttpOnly;"))
            }
            else -> throw OAuth2Exception(
                OAuth2Error.INVALID_REQUEST,
                "Unsupported request method ${request.method}"
            )
        }
    }

    fun handleDebuggerCallback(request: OAuth2HttpRequest): OAuth2HttpResponse {
        log.debug("handling ${request.method} request to debugger callback")

        val decryptedSessionCookie: String = getDecryptedSessionCookie(request)
            ?: return OAuth2HttpResponse(
                status = 500,
                headers = Headers.headersOf("Content-Type", "text/html"),
                body = "<p>Expired session, please try again using the debugger form - " +
                    "<a href='${request.url.toDebuggerUrl()}'>${request.url.toDebuggerUrl()}</></p>"
            )

        val sessionParameters: MutableMap<String, String> = objectMapper.readValue(decryptedSessionCookie)
        val tokenUrl: HttpUrl = sessionParameters["token_url"]?.toHttpUrl()
            ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "missing token_url initial call")

        val code: String = request.url.queryParameter("code")
            ?: request.formParameters.get("code")
            ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "no code parameter present")

        val clientAuthentication = ClientAuthentication.fromMap(sessionParameters)
        val formBodyString: String = mapOf(
            "grant_type" to "authorization_code",
            "code" to code,
            "scope" to sessionParameters.urlEncode("scope"),
            "redirect_uri" to sessionParameters.urlEncode("redirect_uri")
        ).toKeyValueString("&")

        val body = when (clientAuthentication.clientAuthMethod) {
            ClientAuthMethod.CLIENT_SECRET_POST -> {
                formBodyString.plus("&${clientAuthentication.form()}")
            }
            else -> formBodyString
        }
        val headers = when (clientAuthentication.clientAuthMethod) {
            ClientAuthMethod.CLIENT_SECRET_BASIC -> Headers.headersOf("Authorization", clientAuthentication.basic())
            else -> Headers.headersOf()
        }
        val tokenResponse: String = client.newCall(
            Request.Builder()
                .headers(headers)
                .url(tokenUrl)
                .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()
        ).execute().body!!.string()

        val formattedTokenRequest = "POST ${tokenUrl.encodedPath} HTTP/1.1\n" +
            "Host: ${tokenUrl.toHostHeader(true)}\n" +
            "Content-Type: application/x-www-form-urlencoded\n" +
            headers.joinToString("\n") {
                "${it.first}: ${it.second}"
            } +
            "\n\n$body"

        return html(templateMapper.debuggerCallbackHtml(formattedTokenRequest, tokenResponse))
    }

    private fun getDecryptedSessionCookie(request: OAuth2HttpRequest): String? =
        runCatching {
            request.cookies[DEBUGGER_SESSION_COOKIE]?.decrypt(encryptionKey)
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error ->
                log.error("received exception when decrypting cookie", error)
                null
            }
        )

    companion object {
        const val DEBUGGER_SESSION_COOKIE = "debugger-session"
    }
}

private fun debuggerAuthorizationRequest(
    authEndpointUrl: HttpUrl,
    redirectUrl: HttpUrl
): OAuth2HttpRequest =
    authEndpointUrl.newBuilder()
        .addQueryParameter("client_id", "debugger")
        .addQueryParameter("response_type", "code")
        .addQueryParameter("redirect_uri", redirectUrl.toString())
        .addQueryParameter("response_mode", "query")
        .addQueryParameter("scope", "openid somescope")
        .addQueryParameter("state", "1234")
        .addQueryParameter("nonce", "5678")
        .build().let {
            OAuth2HttpRequest(
                headers = Headers.headersOf(),
                method = "GET",
                originalUrl = it
            )
        }

private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

private fun Map<String, String>.require(key: String): String =
    this[key] ?: throw OAuth2Exception(OAuth2Error.INVALID_REQUEST, "missing required parameter $key")

private fun Map<String, String>.urlEncode(key: String): String =
    require(key).urlEncode()

private fun Map<String, String>.toKeyValueString(entrySeparator: String): String =
    this.map { "${it.key}=${it.value}" }
        .toList().joinToString(entrySeparator)

private fun String.encrypt(key: SecretKey): String =
    JWEObject(
        JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A128GCM),
        Payload(this)
    ).also {
        it.encrypt(DirectEncrypter(key))
    }.serialize()

private fun String.decrypt(key: SecretKey): String =
    JWEObject.parse(this).also {
        it.decrypt(DirectDecrypter(key))
    }.payload.toString()

private enum class ClientAuthMethod {
    CLIENT_SECRET_POST,
    CLIENT_SECRET_BASIC
}

private data class ClientAuthentication(
    val clientId: String,
    val clientSecret: String,
    val clientAuthMethod: ClientAuthMethod
) {
    fun form(): String = "client_id=${clientId.urlEncode()}&client_secret=${clientSecret.urlEncode()}"
    fun basic(): String = Credentials.basic(clientId, clientSecret, StandardCharsets.UTF_8)

    companion object {
        fun fromMap(map: Map<String, String>): ClientAuthentication =
            ClientAuthentication(
                map.require("client_id"),
                map.require("client_secret"),
                ClientAuthMethod.valueOf(map.require("client_auth_method"))
            )
    }
}
