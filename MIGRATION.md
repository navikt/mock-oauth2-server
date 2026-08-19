# Migration guide

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

**Consumers on Jackson 2 are unaffected.** Jackson 3 uses a different group ID and package, and still depends on `com.fasterxml.jackson.core:jackson-annotations:2.x`, so both can be on the classpath at once. The Ktor and Spring Boot example apps in this project's test suite stay on Jackson 2 and exercise that combination.

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
