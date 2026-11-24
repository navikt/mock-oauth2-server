# Migration guide

## Migrating to 8.0.0

### Authorize endpoint errors are sent to the client

Previously, every error from the authorize endpoint returned a JSON `400` from the server.

**New behavior (8.0.0):** when the request has a `client_id` and an absolute `redirect_uri`, errors such as `invalid_request` and `unsupported_response_type` are sent to `redirect_uri` with `error`, `error_description` and `state`, as required by [RFC 6749 section 4.1.2.1](https://www.rfc-editor.org/rfc/rfc6749#section-4.1.2.1). The requested `response_mode` is used: a `302` redirect for `query` and `fragment`, or an auto-submitted form for `form_post`. Without a `response_mode`, the default of the `response_type` is used, which is `fragment` for types such as `token` and `code id_token`.

Errors are still returned as JSON when:

- `client_id` or `redirect_uri` is missing, or `redirect_uri` is not an absolute URI.
- `response_mode` is `form_post` and `redirect_uri` is not an HTTP(S) URL.
- The error is not one of the codes in RFC 6749 section 4.1.2.1, for example `invalid_target`, or `not_found` for a misconfigured login page.

A `response_mode` other than `query`, `fragment` and `form_post` returns a JSON `400 invalid_request`. Previously, an otherwise valid request was redirected with a code for `query.jwt` and `fragment.jwt`, and returned a JSON `500` for other values, such as `form_post.jwt` and `jwt`. These modes were never supported or listed in `response_modes_supported`.

The request is validated before the user logs in:

- A relative `redirect_uri` always returns a JSON `400 invalid_request`. Previously, an otherwise valid request was redirected with a code.
- With `interactiveLogin` enabled, an unsupported `response_type` returns `unsupported_response_type` instead of the login page.

A login without `username` sends `invalid_request` to `redirect_uri` instead of a JSON `400 invalid_request`.

**Affected pattern:** tests that call the authorize endpoint with an invalid request and expect a JSON `400`, or that use a relative `redirect_uri` or an unsupported `response_mode`.

**Migration:** use an absolute `redirect_uri`, and assert on the `error` parameter of the redirect (or the posted form) instead of the JSON body:

```kotlin
val location = response.headers["location"]!!.toHttpUrl()
location.queryParameter("error") shouldBe "invalid_request"
```

## Migrating to 7.0.0

### `io.github.microutils:kotlin-logging` replaced by `io.github.oshai:kotlin-logging-jvm`

The published dependency moved from `io.github.microutils:kotlin-logging` (`mu.*`) to `io.github.oshai:kotlin-logging-jvm` (`io.github.oshai.kotlinlogging.*`). `slf4j-api` still comes transitively.

**Affected pattern:** importing `mu.*` (`mu.KotlinLogging`, `mu.KLogger`) transitively through `mock-oauth2-server` without declaring `io.github.microutils:kotlin-logging-jvm` yourself.

**Migration:** migrate to `io.github.oshai:kotlin-logging-jvm` (`io.github.oshai.kotlinlogging.*`), or declare the legacy dependency explicitly:

```kotlin
testImplementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
```

### Authorization codes are bound to the client and `redirect_uri`

Previously, an authorization code could be redeemed with any `client_id` and any `redirect_uri`.

**New behavior (7.0.0):** the token request must use the same `client_id` as the authorization request. If the authorization request included a `redirect_uri`, the token request must include the exact same value. Otherwise the server returns `400 invalid_grant`, and the code cannot be used again.

**Affected pattern:** tests that authorize with one `client_id` or `redirect_uri` and redeem the code with another, for example a hardcoded `client_id` in the token request that differs from the one in the authorize URL. A client that builds `redirect_uri` from the current request URL can also be affected, because at the callback that URL still carries `code` and `state`.

**Migration:** use the same `client_id` and `redirect_uri` in both requests:

```kotlin
// authorization request
server.authorizationEndpointUrl("default").newBuilder()
    .addQueryParameter("client_id", "client1")
    .addQueryParameter("redirect_uri", "http://localhost:8080/callback")
    // ...

// token request
"grant_type=authorization_code&code=$code&client_id=client1&client_secret=secret&redirect_uri=http://localhost:8080/callback"
```

### Refresh tokens are bound to the client they were issued to

Previously, any client could redeem a refresh token.

**New behavior (7.0.0):** a refresh token stores the `client_id` of the token request that issued it, taken from client authentication or the `client_id` parameter. A refresh request from a different client returns `400 invalid_grant`. With `rotateRefreshToken`, the rotated token stays bound to the same client.

**Affected pattern:** tests that get a refresh token with one client and refresh it with another.

**Migration:** refresh with the same client that redeemed the authorization code:

```kotlin
mapOf("grant_type" to "refresh_token", "refresh_token" to refreshToken, "client_id" to "id", "client_secret" to "secret")
```

### The revocation endpoint requires client authentication

Previously, `POST /{issuerId}/revoke` revoked a token without any client authentication.

**New behavior (7.0.0):** a revocation request needs client authentication (for example `client_secret_basic`, `client_secret_post` or `private_key_jwt`) or, for public clients, a `client_id` parameter. Without either, the endpoint returns `401 invalid_client` and the token is not revoked.

**Affected pattern:** tests that revoke a token with only the `token` parameter.

**Migration:** send the client credentials with the revocation request, as you do on the token endpoint:

```kotlin
client.post(server.revocationEndpointUrl("default"), mapOf("client_id" to "id", "token" to refreshToken))
```

### Only the owning client can revoke a refresh token

Previously, any client could revoke another client's refresh token.

**New behavior (7.0.0):** a revocation request removes a refresh token only when it comes from the client the token was issued to. Client authentication takes precedence over a conflicting `client_id` parameter. A request from another client returns `200` with an empty body without revoking the token, the same as for an unknown token. Rotated tokens keep their owner.

**Affected pattern:** tests that revoke a refresh token with a different client than the one that obtained it.

**Migration:** revoke with the same client that obtained the refresh token:

```kotlin
client.post(server.revocationEndpointUrl("default"), mapOf("client_id" to "id", "token" to refreshToken))
```

## Migrating to 6.0.0

### `logback-classic` and `kotlinx-serialization-json` are no longer bundled

Both appeared in the published POM and ended up on consumers' classpaths. They are now test- and standalone-only.

**Affected pattern:** relying on mock-oauth2-server to pull in an SLF4J binding or `kotlinx-serialization-json`.

**Migration:** if your test logs went quiet, declare a binding yourself:

```kotlin
testRuntimeOnly("ch.qos.logback:logback-classic:<version>")
```

`slf4j-api` still comes transitively; only the binding is gone. If you used `kotlinx-serialization-json` without declaring it, add it explicitly. The standalone server and Docker image still ship with logback.

### The debugger can no longer target an external identity provider

The debugger callback POSTed to a `token_url` taken from a client-supplied cookie and rendered the response, so anyone could make the server call hosts they could not reach themselves. `POST /<issuer>/debugger` redirected to a client-supplied `authorize_url` the same way.

**New behavior (6.0.0):** both endpoints come from the server. In the debugger form, the endpoint and `redirect_uri` fields are readonly; the rest stay editable. The `authorize_url`, `token_url`, `client_secret`, `client_auth_method` and `redirect_uri` form parameters are ignored.

**Affected pattern:** using the debugger as a generic OAuth2 client against a third-party provider.

**Migration:** none. Point a real client at the external provider instead. The debugger still works against this server's own issuers.

If you construct `DebuggerRequestHandler` yourself, it now takes an `OAuth2HttpServer` instead of an `Ssl?`.

### Jackson 3

The library now uses Jackson 3 (`tools.jackson.*`) instead of Jackson 2 (`com.fasterxml.jackson.*`).

**Consumers on Jackson 2 are unaffected.** Jackson 3 uses a different group ID and package, and still depends on `com.fasterxml.jackson.core:jackson-annotations:2.x`. Imports from `com.fasterxml.jackson.annotation.*` (for example `@JsonProperty` and `@JsonInclude`) are still correct and expected with Jackson 3.

**Affected pattern:** using the `no.nav.security.mock.oauth2.http.objectMapper` top-level property, or the `OAuth2TokenProviderDeserializer` and `OAuth2HttpServerDeserializer` classes nested in `OAuth2Config`.

**Migration:** these are now `internal`, so the library no longer exposes Jackson types in its Kotlin API. Construct your own mapper if you used `objectMapper`.

### Kotlin 2.2.21 is the minimum runtime

Jackson 3 brought in `jackson-module-kotlin` 3.x, which references `kotlin.time.Instant`. That class arrived in `kotlin-stdlib` 2.1.20. Releases 6.0.0 and 6.0.1 still declared `kotlin-stdlib` 1.9.0, so consumers who took that version could fail with `NoClassDefFoundError: kotlin/time/Instant`.

**New behavior (6.0.2):** the published `kotlin-stdlib` and `kotlin-reflect` version is 2.2.21, matching what okhttp already asks for.

**Affected pattern:** none directly. Kotlin consumers already needed a 2.0 compiler for 6.x, and the stdlib is backwards compatible.

**Migration:** upgrade to 6.0.2. Nothing else, unless you pinned `kotlin-stdlib` below 2.2.21 yourself — raise it.

### `NettyWrapper` caps request bodies at 1 MiB

Request bodies were aggregated with no limit, so any unauthenticated client could exhaust the heap by sending a large body to any endpoint.

**New behavior (6.0.0):** `NettyWrapper` answers `413 Request Entity Too Large` above 1 MiB. Affects the standalone server and Docker image, which default to `NettyWrapper`. `MockWebServerWrapper` is unaffected.

## Migrating to 5.0.0

### Interactive login + `requestMappings`: claim precedence change

In previous versions, claims submitted on the login page could overwrite claims set by a matching `requestMapping` (including `sub`). This could produce tokens where the JWT subject differed from the `sub` claim in the payload.

**New behavior (5.0.0):** claims set by a matching `requestMapping` take priority. Login-page claims can add new claims but cannot overwrite claims already set by the mapping.

**Affected pattern:** using `interactiveLogin: true` together with mappings/token callbacks, and relying on the login-page `claims` field to override specific claims (for example `sub`) that are also set in the matching `requestMapping`.

**Migration:** move the overriding claim into `requestMappings`, or remove the conflicting key from the mapping if you want the login-page claim to be used.

For details, see the [README](https://github.com/navikt/mock-oauth2-server/blob/master/README.md#interactive-login-matching-and-templating-on-the-login-username) section on interactive login matching and templating.

## Migrating to 4.0.0

### Refresh token validation is now strict

Previously, any arbitrary string passed as a `refresh_token` was silently accepted and used to mint a new token via the default callback. This has been fixed: unknown, expired, and revoked refresh tokens now fail with `400 invalid_grant`.

**What this means for existing tests:**

- Tests that passed a hardcoded or arbitrary string as `refresh_token` will now receive `400 invalid_grant` instead of a valid token response. Use a real refresh token obtained from a prior token request.
- Tests that relied on refresh succeeding after revocation will now fail. This is the correct behavior.
- Tests that presented a refresh token issued by issuer A to issuer B will now receive `400 invalid_grant`.

**Example migration:**

```kotlin
// Before: arbitrary string was accepted
val response = client.post(server.tokenEndpointUrl("default")) {
    body = "grant_type=refresh_token&refresh_token=any-string"
}

// After: obtain a real refresh token first
val tokenResponse = client.post(server.tokenEndpointUrl("default")) {
    body = "grant_type=authorization_code&code=..."
}
val refreshToken = tokenResponse.body.refresh_token
val response = client.post(server.tokenEndpointUrl("default")) {
    body = "grant_type=refresh_token&refresh_token=$refreshToken"
}
```
