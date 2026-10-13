package no.nav.security.mock.oauth2.http

import com.nimbusds.oauth2.sdk.ErrorObject
import com.nimbusds.oauth2.sdk.GeneralException
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.GrantType.AUTHORIZATION_CODE
import com.nimbusds.oauth2.sdk.GrantType.CLIENT_CREDENTIALS
import com.nimbusds.oauth2.sdk.GrantType.JWT_BEARER
import com.nimbusds.oauth2.sdk.GrantType.PASSWORD
import com.nimbusds.oauth2.sdk.GrantType.REFRESH_TOKEN
import com.nimbusds.oauth2.sdk.GrantType.TOKEN_EXCHANGE
import com.nimbusds.oauth2.sdk.OAuth2Error
import com.nimbusds.oauth2.sdk.ParseException
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import mu.KotlinLogging
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.OAuth2Exception
import no.nav.security.mock.oauth2.debugger.DebuggerRequestHandler
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.AUTHORIZATION
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.END_SESSION
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.JWKS
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.OAUTH2_WELL_KNOWN
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.OIDC_WELL_KNOWN
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.REVOKE
import no.nav.security.mock.oauth2.extensions.OAuth2Endpoints.TOKEN
import no.nav.security.mock.oauth2.extensions.isPrompt
import no.nav.security.mock.oauth2.extensions.issuerId
import no.nav.security.mock.oauth2.extensions.toIssuerUrl
import no.nav.security.mock.oauth2.grant.AuthorizationCodeHandler
import no.nav.security.mock.oauth2.grant.ClientCredentialsGrantHandler
import no.nav.security.mock.oauth2.grant.GrantHandler
import no.nav.security.mock.oauth2.grant.JwtBearerGrantHandler
import no.nav.security.mock.oauth2.grant.PasswordGrantHandler
import no.nav.security.mock.oauth2.grant.RefreshToken
import no.nav.security.mock.oauth2.grant.RefreshTokenGrantHandler
import no.nav.security.mock.oauth2.grant.RefreshTokenManager
import no.nav.security.mock.oauth2.grant.TokenExchangeGrantHandler
import no.nav.security.mock.oauth2.introspect.introspect
import no.nav.security.mock.oauth2.login.Login
import no.nav.security.mock.oauth2.login.LoginRequestHandler
import no.nav.security.mock.oauth2.missingParameter
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.mock.oauth2.unsupportedGrantType
import no.nav.security.mock.oauth2.userinfo.userInfo
import okhttp3.Headers
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.nio.channels.Channels
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SecureDirectoryStream
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributeView
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

private val log = KotlinLogging.logger {}

class OAuth2HttpRequestHandler(
    private val config: OAuth2Config,
) {
    private val loginRequestHandler = LoginRequestHandler(templateMapper, config)
    private val debuggerRequestHandler = DebuggerRequestHandler(config.httpServer)
    private val tokenCallbackQueue: BlockingQueue<OAuth2TokenCallback> = LinkedBlockingQueue()
    private val refreshTokenManager = RefreshTokenManager()

    private val grantHandlers: Map<GrantType, GrantHandler> =
        mapOf(
            AUTHORIZATION_CODE to AuthorizationCodeHandler(config.tokenProvider, refreshTokenManager),
            CLIENT_CREDENTIALS to ClientCredentialsGrantHandler(config.tokenProvider),
            JWT_BEARER to JwtBearerGrantHandler(config.tokenProvider),
            TOKEN_EXCHANGE to TokenExchangeGrantHandler(config.tokenProvider),
            REFRESH_TOKEN to
                RefreshTokenGrantHandler(config.tokenProvider, refreshTokenManager, config.rotateRefreshToken) { issuerId ->
                    synchronized(tokenCallbackQueue) {
                        if (tokenCallbackQueue.peek()?.issuerId() == issuerId) tokenCallbackQueue.poll() else null
                    }
                },
            PASSWORD to PasswordGrantHandler(config.tokenProvider),
        )

    private val exceptionHandler: ExceptionHandler = { request, error ->
        log.error("received exception when handling request: ${request.url}.", error)
        val msg = URLEncoder.encode(error.message, Charset.forName("UTF-8"))
        val errorObject: ErrorObject =
            when (error) {
                is OAuth2Exception -> error.errorObject
                is ParseException -> error.errorObject ?: OAuth2Error.INVALID_REQUEST.setDescription("failed to parse request: $msg")
                is GeneralException -> error.errorObject
                else -> null
            } ?: OAuth2Error.SERVER_ERROR.setDescription("unexpected exception with message: $msg")
        oauth2Error(errorObject)
    }

    val authorizationServer: Route =
        routes {
            exceptionHandler(exceptionHandler)
            interceptors(CorsInterceptor())
            wellKnown()
            jwks()
            authorization()
            token()
            endSession()
            revocation(refreshTokenManager)
            userInfo(config.tokenProvider)
            introspect(config.tokenProvider)
            preflight()
            staticAssets()
            get("/favicon.ico") { OAuth2HttpResponse(status = 200) }
            attach(debuggerRequestHandler)
        }

    fun enqueueTokenCallback(oAuth2TokenCallback: OAuth2TokenCallback) = tokenCallbackQueue.add(oAuth2TokenCallback)

    private fun Route.Builder.wellKnown() =
        get(OIDC_WELL_KNOWN, OAUTH2_WELL_KNOWN) {
            log.debug("returning well-known json data for url=${it.url}")
            json(
                it.toWellKnown().copy(
                    idTokenSigningAlgValuesSupported = listOf(config.tokenProvider.getAlgorithm().name),
                ),
            )
        }

    private fun Route.Builder.jwks() =
        get(JWKS) {
            log.debug("handle jwks request on url=${it.url}")
            val issuerId = it.url.issuerId()
            val jwkSet = config.tokenProvider.publicJwkSet(issuerId)
            json(jwkSet.toJSONObject())
        }

    private fun Route.Builder.authorization() =
        apply {
            val authorizationCodeHandler = grantHandlers[AUTHORIZATION_CODE] as AuthorizationCodeHandler
            get(AUTHORIZATION) {
                val authRequest: AuthenticationRequest = it.asAuthenticationRequest()
                if (config.interactiveLogin || authRequest.isPrompt()) {
                    html(loginRequestHandler.loginHtml(it))
                } else {
                    authenticationSuccess(authorizationCodeHandler.authorizationCodeResponse(authRequest))
                }
            }
            post(AUTHORIZATION) {
                val authRequest: AuthenticationRequest = it.asAuthenticationRequest()
                val login: Login = loginRequestHandler.loginSubmit(it)
                authenticationSuccess(authorizationCodeHandler.authorizationCodeResponse(authRequest, login))
            }
        }

    private fun Route.Builder.endSession() =
        any(END_SESSION) {
            log.debug("handle end session request $it")
            it.url.queryParameter("post_logout_redirect_uri")?.let { postLogoutRedirectUri ->
                it.url.queryParameter("state")?.let { state ->
                    redirect("$postLogoutRedirectUri?state=$state")
                } ?: redirect(postLogoutRedirectUri)
            } ?: html("logged out")
        }

    private fun Route.Builder.revocation(refreshTokenManager: RefreshTokenManager) =
        post(REVOKE) {
            log.debug("handle revocation request $it")
            val token: RefreshToken = it.formParameters.get("token") ?: missingParameter("token")
            when (val hint = it.formParameters.get("token_type_hint")) {
                "refresh_token" -> {
                    refreshTokenManager.remove(token)
                }

                else -> {
                    throw OAuth2Exception(
                        ErrorObject("unsupported_token_type", "unsupported token type: $hint", 400),
                        "unsupported token type: $hint",
                    )
                }
            }
            OAuth2HttpResponse(status = 200)
        }

    private fun Route.Builder.token() =
        apply {
            get(TOKEN) {
                OAuth2HttpResponse(status = 405, body = "unsupported method")
            }
            post(TOKEN) {
                log.debug("handle token request $it")
                val grantType = it.grantType()
                val issuerId = it.url.issuerId()
                // For refresh_token grants the handler resolves the callback itself (stored vs enqueued priority),
                // so the tokenCallback passed here is unused. For all other grants consume from queue or fall back to config default.
                val tokenCallback: OAuth2TokenCallback =
                    if (grantType == REFRESH_TOKEN) {
                        DefaultOAuth2TokenCallback(issuerId = issuerId)
                    } else {
                        tokenCallbackFromQueueOrDefault(issuerId)
                    }
                val grantHandler: GrantHandler = grantHandlers[grantType] ?: unsupportedGrantType(grantType)
                val tokenResponse = grantHandler.tokenResponse(it, it.url.toIssuerUrl(), tokenCallback)
                json(tokenResponse)
            }
        }

    private fun Route.Builder.staticAssets() =
        apply {
            if (config.staticAssetsPath != null) {
                require(config.staticAssetsPath.isNotBlank()) { "staticAssetsPath must not be blank" }
                val staticAssetsDirectory = File(config.staticAssetsPath).canonicalFile.toPath()
                get("/static/*") {
                    val path =
                        it.url.pathSegments
                            .drop(1)
                            .joinToString("/")
                    val names = Paths.get(path).normalize().toList()
                    val bytes = readStaticAsset(staticAssetsDirectory, names)

                    if (bytes != null) {
                        val contentType = Files.probeContentType(names.fold(staticAssetsDirectory, Path::resolve)) ?: "application/octet-stream"
                        OAuth2HttpResponse(status = 200, bytesBody = bytes, headers = Headers.headersOf("Content-Type", contentType))
                    } else {
                        OAuth2HttpResponse(status = 404, body = "not found")
                    }
                }
            }
        }

    private fun Route.Builder.preflight() = options { OAuth2HttpResponse(status = 204) }

    private fun tokenCallbackFromQueueOrDefault(issuerId: String): OAuth2TokenCallback {
        val queued =
            synchronized(tokenCallbackQueue) {
                if (tokenCallbackQueue.peek()?.issuerId() == issuerId) tokenCallbackQueue.poll() else null
            }
        return queued
            ?: config.tokenCallbacks.firstOrNull { it.issuerId() == issuerId }
            ?: DefaultOAuth2TokenCallback(issuerId = issuerId)
    }
}

/**
 * Reads a regular file below [root] without following symbolic links. Where the platform supports
 * [SecureDirectoryStream] the path is traversed relative to open directory handles, so a component
 * swapped for a symlink after validation cannot redirect the read outside [root].
 */
private fun readStaticAsset(
    root: Path,
    names: List<Path>,
): ByteArray? {
    if (names.isEmpty() || names.any { it.toString() == ".." }) return null
    return try {
        Files.newDirectoryStream(root).use { stream ->
            if (stream is SecureDirectoryStream<Path>) stream.readNoFollow(names) else readNoFollow(root, names)
        }
    } catch (e: IOException) {
        null
    }
}

private fun SecureDirectoryStream<Path>.readNoFollow(names: List<Path>): ByteArray? {
    val name = names.first()
    if (names.size > 1) {
        return newDirectoryStream(name, LinkOption.NOFOLLOW_LINKS).use { it.readNoFollow(names.drop(1)) }
    }
    val attributes = getFileAttributeView(name, BasicFileAttributeView::class.java, LinkOption.NOFOLLOW_LINKS).readAttributes()
    if (!attributes.isRegularFile) return null
    return newByteChannel(name, setOf(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)).use { Channels.newInputStream(it).readBytes() }
}

private fun readNoFollow(
    root: Path,
    names: List<Path>,
): ByteArray? {
    var file = root
    for (name in names) {
        file = file.resolve(name)
        if (Files.isSymbolicLink(file)) return null
    }
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return null
    return Files.newByteChannel(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS).use { Channels.newInputStream(it).readBytes() }
}
