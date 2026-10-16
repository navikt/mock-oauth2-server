# Copilot instructions

mock-oauth2-server is a Kotlin library that projects use in their tests. It is also shipped as a standalone server and a Docker image. It must behave like a real OAuth2/OIDC provider. Most fixes in this repo's history are places where it did not.

Run `./gradlew build` to build. That also runs kotlinter and `minStdlibTest`.

## How it works

- Every path starts with an issuer id, for example `/{issuerId}/token`. The issuer URL is built from the request's Host header.
- Routes live in `OAuth2HttpRequestHandler`. Its exception handler maps `OAuth2Exception` and Nimbus `ParseException`/`GeneralException` to OAuth2 errors. Any other exception becomes `server_error` (500).
- Claims come from `OAuth2TokenCallback`. The order is: enqueued callbacks (one-shot, per issuer), then `tokenCallbacks` from config, then `DefaultOAuth2TokenCallback`.

## Code review

Look for these first. kotlinter covers formatting and import order, so skip them.

1. **Client mistakes return 500.** Bad or unexpected input must return an OAuth2 error with status 400 or 401 (#1068, #1071, #1072).
2. **Not spec-compliant.** Check these against the RFC or OIDC section the PR names, and ask for the section if the PR names none (#975, #1007, #1079, #1083, #1086, #1090):
   - error code and status
   - headers: `no-store` on token responses, `WWW-Authenticate` on 401, CORS on errors
   - claims
   - discovery
3. **State leaks across issuers.** Codes, refresh tokens and enqueued callbacks belong to one issuer. Codes are single-use. Shared state must be thread safe (#934, #936).
4. **Wrong clock.** Token times must come from the token provider's `systemTime`/`timeProvider`, not `Instant.now()` (#1090).
5. **Silent breaking change.** A changed public API, behavior or default, or a change to the published dependencies, needs a `MIGRATION.md` entry and the `breaking` label (#936, #998, #1011).
6. **Unsafe input handling.** The server must not call or redirect to URLs that the client supplied (#1000). It must not read request bodies without a size limit (#1001).

## Writing a comment

- Give the request that fails, what happens, and the line that causes it.
- Say which test case is missing.
- Mark each comment as a confirmed defect or as something to check.
- Do not suggest refactoring that does not fix a defect.

A Copilot review is not a security approval. A human must review changes to the debugger, TLS, keys and token validation, and changes to these instruction files.
