# Copilot instructions

mock-oauth2-server is a Kotlin library that projects use in their tests. It is also shipped as a standalone server and a Docker image. It must behave like a real OAuth2/OIDC provider. Most fixes in this repo's history are places where it did not.

`./gradlew build` runs `lintKotlin`, the tests and `minStdlibTest`. Lint fails on unformatted code; `./gradlew formatKotlin` fixes it.

## How it works

- The issuer id is the path in front of the endpoint, for example `/{issuerId}/token`. The issuer URL comes from the request's `Host` header, `X-Forwarded-Proto` and `X-Forwarded-Port`.
- Routes live in `OAuth2HttpRequestHandler`. Its exception handler maps `OAuth2Exception` and Nimbus `ParseException`/`GeneralException` to OAuth2 errors. Any other exception becomes `server_error` (500).
- Claims come from `OAuth2TokenCallback`. The order is: enqueued callbacks (one-shot, per issuer), then `tokenCallbacks` from config, then `DefaultOAuth2TokenCallback`. The `refresh_token` grant is different: it uses an enqueued callback, then the callback stored with the refresh token, so refreshed tokens keep their claims. An unknown refresh token returns `invalid_grant`.

## Code review

Look for these first. `lintKotlin` checks formatting and import order in CI, so skip them.

1. **Client mistakes return 500.** Bad or unexpected input must return an OAuth2 error with status 400 or 401 (#1068, #1071, #1072).
2. **Not spec-compliant.** Check these against the RFC or OIDC section the PR names, and ask for the section if the PR names none (#975, #1007, #1079, #1083, #1086, #1090):
   - error code and status
   - headers:
     - `Cache-Control: no-store` and `Pragma: no-cache` on successful token responses only
     - a Bearer `WWW-Authenticate` challenge on `invalid_token`
     - a challenge on `invalid_client` when the client sent an `Authorization` header
     - CORS headers on error responses too
   - claims
   - discovery
3. **Refresh token and callback state.**
   - Authorization codes are single-use.
   - Refresh tokens are rejected at an issuer other than the one that issued them.
   - Shared state must be thread safe (#934, #936).
4. **Wrong clock.** Token times must come from the token provider's `systemTime`/`timeProvider`, not `Instant.now()` (#1090).
5. **Silent breaking change.** A changed public API, a change to documented behavior or a default, or a change to the published dependencies, needs a `MIGRATION.md` entry. Examples: #936, #998, #1011. It also needs the `breaking` label, which sets the major version (#1070). A fix that rejects usage outside the README contract, such as a request that mixes issuers, is not breaking. It needs a "Behavior change" line in the PR description instead.
6. **Unsafe input handling.**
   - The server must not send its own requests to URLs that the client supplied (#1000). Redirects that the protocol defines, such as `redirect_uri` and `post_logout_redirect_uri`, are expected.
   - Keep the 1 MiB request body limit in `NettyWrapper` (#1001).

## Writing a comment

- Give the request that fails, what happens, and the line that causes it.
- Say which test case is missing.
- Mark each comment as a confirmed defect or as something to check.
- Do not suggest refactoring that does not fix a defect.

A Copilot review is not a security approval. A human must review changes to the debugger, TLS, keys and token validation, and changes to these instruction files.
