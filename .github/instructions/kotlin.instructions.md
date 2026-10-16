---
applyTo: "src/**/*.kt"
---

# Kotlin

## `src/main/kotlin`

- Fail through the helpers in `OAuth2Exception.kt` with Nimbus `OAuth2Error` codes. `!!`, `require`, `error()` or `.first()` on request data become a 500.
- Build issuer URLs with `toIssuerUrl()`/`issuerId()` from the request. Never hard-code the host.
- Read `client_id` from both the form and HTTP Basic auth (#935).
- Register new endpoints in `authorizationServer` so the exception handler and CORS apply to them.
- Use Jackson 3 (`tools.jackson.*`). `com.fasterxml.jackson.annotation` is fine.
- Consumers extend `KeyProvider`, `OAuth2TokenCallback` and `MockOAuth2Server`. Changing their open members is a breaking change.
- `MockWebServerWrapper` and `NettyWrapper` must behave the same.

## `src/test/kotlin`

- A protocol change needs a test in `e2e/` that uses `withMockOAuth2Server { }` and `testutils/`. Assert the status, the `error` code and the headers.
