---
applyTo: "src/**/*.kt"
---

# Kotlin

## `src/main/kotlin`

- Fail through the helpers in `OAuth2Exception.kt` with Nimbus `OAuth2Error` codes. `!!`, `require`, `error()` or `.first()` on request data become a 500.
- Build issuer URLs with `toIssuerUrl()`/`issuerId()` on the request URL. Never hard-code the host.
- Get the client id with `TokenRequest.clientIdAsString()`. It covers both the form and HTTP Basic auth (#935).
- Register new endpoints in `authorizationServer` so the exception handler and CORS apply to them.
- Use Jackson 3 (`tools.jackson.*`). `com.fasterxml.jackson.annotation` is fine.
- Consumers extend `KeyProvider`, `DefaultOAuth2TokenCallback` and `MockOAuth2Server`, and implement `OAuth2TokenCallback`. Changing their public or open members is a breaking change.
- `MockWebServerWrapper` and `NettyWrapper` share the request handling. The only intended difference is that `NettyWrapper` limits bodies to 1 MiB.

## `src/test/kotlin`

- A protocol change needs a test in `e2e/`. Most e2e tests use `withMockOAuth2Server { }` and the helpers in `testutils/`. Assert the status, the `error` code and the headers.
