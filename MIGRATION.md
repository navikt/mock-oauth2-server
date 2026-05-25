# Migration guide

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

### Interactive login + `tokenCallbacks`: claim precedence change

In previous versions, claims submitted on the login page could overwrite claims set by a matching `requestMapping` (including `sub`). This could produce tokens where the JWT subject differed from the `sub` claim in the payload.

**New behavior:** claims set by a matching `requestMapping` take priority. Login-page claims can add new claims but cannot overwrite claims already set by the mapping.

**Affected pattern:** using `interactiveLogin: true` together with `tokenCallbacks`, and relying on the login-page `claims` field to override specific claims (e.g. `sub`) that are also set in the matching `requestMapping`.

**Migration:** move the claim into the `requestMapping` instead of submitting it from the login page, or remove the conflicting key from the mapping so the login-page value takes effect.
