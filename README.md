<p align="center">
  <a href="https://github.com/navikt/mock-oauth2-server/actions"><img src="https://github.com/navikt/mock-oauth2-server/workflows/Build%20master/badge.svg" alt="Build"></a>
  <a href="https://search.maven.org/artifact/no.nav.security/mock-oauth2-server"><img src="https://img.shields.io/maven-central/v/no.nav.security/mock-oauth2-server?color=green&logo=Apache%20Maven" alt="Maven Central"></a>
  <a href="https://github.com/navikt/mock-oauth2-server/packages/"><img src="https://img.shields.io/github/v/release/navikt/mock-oauth2-server?color=green&include_prereleases&label=Docker&logo=Docker" alt="Docker"></a>
  <a href="LICENSE.md"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/kotlin-2.x-blue.svg?logo=kotlin" alt="Kotlin"></a>
</p>

<h1 align="center">mock-oauth2-server</h1>
<p align="center">Scriptable OAuth2/OpenID Connect server for JVM tests and Docker Compose.</p>

---

## Table of Contents

- [Quick Start](#quick-start)
- [What it does](#what-it-does)
- [Supported Flows](#supported-flows)
- [Usage](#usage)
  - [In JVM Tests](#in-jvm-tests)
  - [Standalone / Docker](#standalone--docker)
  - [Docker Compose](#docker-compose)
  - [Token Customization via JSON_CONFIG](#token-customization-via-json_config)
  - [Auto-added claims](#auto-added-claims)
  - [aud claim resolution](#aud-claim-resolution)
  - [HTTPS](#https)
  - [CORS](#cors)
  - [Debugger](#debugger)
- [Configuration Reference](#configuration-reference)
- [API Reference](#api-reference)
- [Contributing](#contributing)
- [Contact](#contact)
- [License](#license)

---

## Quick Start

Add the dependency:

**Gradle Kotlin DSL**

```kotlin
testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
```

**Maven**

```xml
<dependency>
  <groupId>no.nav.security</groupId>
  <artifactId>mock-oauth2-server</artifactId>
  <version>${mock-oauth2-server.version}</version>
  <scope>test</scope>
</dependency>
```

Start the server and issue a token in your test:

```kotlin
val server = MockOAuth2Server()
server.start()

val token = server.issueToken(
    issuerId = "default",
    subject = "user123",
    audience = "my-api",
)

// Point your app at the discovery URL
val wellKnownUrl = server.wellKnownUrl("default").toString()

// Attach the token to a request
request.addHeader("Authorization", "Bearer ${token.serialize()}")

server.shutdown()
```

Or run it as a Docker container:

```
docker run -p 8080:8080 ghcr.io/navikt/mock-oauth2-server:$MOCK_OAUTH2_SERVER_VERSION
```

Token endpoint: `http://localhost:8080/default/token`
Discovery: `http://localhost:8080/default/.well-known/openid-configuration`

---

## What it does

mock-oauth2-server lets you test applications that depend on a real OAuth2/OpenID Connect server without disabling security. It issues signed JWTs that are verifiable through standard JWKS and discovery endpoints, so your app does not need any special test configuration.

It supports multi-issuer setups, all major OAuth2 grant types, token customization, and runs both embedded in JVM tests and as a standalone process in Docker Compose.

> [!WARNING]
> This server is for testing only. Do not use it in production.

---

## Supported Flows

- OpenID Connect Authorization Code Flow
- OAuth2 Client Credentials Grant
- OAuth2 JWT Bearer Grant (On-Behalf-Of flow)
- OAuth2 Token Exchange Grant
- OAuth2 Refresh Token Grant
- OAuth2 Resource Owner Password Credentials Grant
  - usage should be avoided if possible as this grant is considered insecure and [removed in its entirety](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics-13#section-3.4) from OAuth 2.1

Issued JWT tokens are verifiable through standard mechanisms via OpenID Connect Discovery and OAuth2 Authorization Server Metadata endpoints. Multi-issuer setups are supported with no configuration — the first path segment in any request URL determines the issuer.

---

## Usage

### In JVM Tests

#### Minimal setup

```kotlin
val server = MockOAuth2Server()
server.start()

val wellKnownUrl = server.wellKnownUrl("default").toString()
// configure your app to use wellKnownUrl, run your test, then:

server.shutdown()
```

Use `withMockOAuth2Server` to avoid manual start/shutdown:

```kotlin
withMockOAuth2Server {
    val wellKnownUrl = wellKnownUrl("default").toString()
    // configure your app and run your test here
}
```

#### Issuing tokens directly

The simplest way to issue a token:

```kotlin
val token: SignedJWT = server.issueToken(
    issuerId = "default",
    subject = "user123",
    audience = "my-api",
    claims = mapOf("roles" to listOf("admin")),
    expiry = 3600,
)
request.addHeader("Authorization", "Bearer ${token.serialize()}")
```

To issue a token with a custom callback object for full control:

```kotlin
val token: SignedJWT = server.issueToken(issuerId, "someclientid", DefaultOAuth2TokenCallback())
```

To issue a token for an external issuer URL that is still verifiable via this server's JWKS:

```kotlin
val token: SignedJWT = server.anyToken(
    issuerUrl = "https://external-idp.example.com".toHttpUrl(),
    claims = mapOf("sub" to "user123", "aud" to "my-api"),
)
```

#### Testing Authorization Code Flow (user login)

Enqueue a callback to control what is returned when your app exchanges the code:

```kotlin
@Test
fun loginWithIdTokenForSubjectFoo() {
    server.enqueueCallback(
        DefaultOAuth2TokenCallback(
            issuerId = issuerId,
            subject = "foo"
        )
    )
    // Invoke your app here and assert user foo is logged in
}
```

To set specific claims in the `id_token`:

```kotlin
@Test
fun loginWithIdTokenForAcrClaimEqualsLevel4() {
    server.enqueueCallback(
        DefaultOAuth2TokenCallback(
            issuerId = issuerId,
            claims = mapOf("acr" to "Level4")
        )
    )
    // Invoke your app here and assert acr=Level4 is present in id_token
}
```

#### Verifying requests made to the server

> [!NOTE]
> `takeRequest()` is only available when using `MockWebServerWrapper` (the default). It throws `UnsupportedOperationException` with `NettyWrapper`.

```kotlin
val request = server.takeRequest()
assertThat(request.path).contains("/default/token")
```

#### Controlling token time

```kotlin
val server = MockOAuth2Server(
    config = OAuth2Config(
        tokenProvider = OAuth2TokenProvider(systemTime = Instant.parse("2020-01-21T00:00:00Z"))
    )
)
val token = server.issueToken(issuerId = "issuer1")
// token has iat=2020-01-21T00:00:00Z
```

#### Multi-issuer setup

The first path segment in any request URL is the `issuerId`. No configuration needed:

```
http://localhost:8080/issuer-a/.well-known/openid-configuration  → issuerId = issuer-a
http://localhost:8080/issuer-b/.well-known/openid-configuration  → issuerId = issuer-b
```

Each issuer has its own discovery document, token endpoint, and JWKS.

#### More examples

- [Kotlin with the ktor framework](src/test/kotlin/examples/kotlin/ktor)
- [Java with Spring Boot and Spring Security](src/test/java/examples/java/springboot/)

### Standalone / Docker

The standalone server defaults to port `8080`.

**Run with Docker:**

```
docker run -p 8080:8080 ghcr.io/navikt/mock-oauth2-server:$MOCK_OAUTH2_SERVER_VERSION
```

**Build locally:**

```
./gradlew -Djib.from.platforms=linux/amd64 jibDockerBuild
docker run -p 8080:8080 $IMAGE_NAME
```

Health check: `GET /isalive` returns `200` when the server is ready.

On Windows, specify the host explicitly: `docker run -p 8080:8080 -h localhost $IMAGE_NAME`

### Docker Compose

When running `mock-oauth2-server` alongside your application in Docker Compose, there are two networking scenarios to consider.

**Scenario 1: Container-to-container only (most common for integration tests)**

Both services communicate over Docker's internal network. Your app references the mock server using the Docker Compose service name as the hostname:

```yaml
services:
  your_app:
    build: .
    ports:
      - 8080:8080
    environment:
      - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://mock-oauth2-server:8080/default/jwks
  mock-oauth2-server:
    image: ghcr.io/navikt/mock-oauth2-server:$MOCK_OAUTH2_SERVER_VERSION
    ports:
      - 8090:8080
```

Your app reaches the mock server at `http://mock-oauth2-server:8080` (internal Docker network). From your host machine the mock server is at `http://localhost:8090`.

**Scenario 2: Container-to-container + browser interaction (e.g. Authorization Code Flow)**

If a browser also needs to reach the mock server, issuer URLs in tokens must be resolvable both from inside Docker and from your browser:

1. Add `127.0.0.1 host.docker.internal` to your `/etc/hosts` file (Linux only; macOS and Windows Docker Desktop add this automatically).
2. Set `hostname: host.docker.internal` on the mock server service.

```yaml
services:
  your_app:
    build: .
    ports:
      - 8080:8080
    environment:
      - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://host.docker.internal:8090/default/jwks
  mock-oauth2-server:
    image: ghcr.io/navikt/mock-oauth2-server:$MOCK_OAUTH2_SERVER_VERSION
    ports:
      - 8090:8080
    hostname: host.docker.internal
```

> [!NOTE]
> Each service must use a different host port. Mapping two services to the same host port causes a `port is already allocated` error.

### Token Customization via JSON_CONFIG

When running standalone or in Docker you can configure token callbacks via the `JSON_CONFIG` environment variable (or `JSON_CONFIG_PATH` pointing to a file). When neither is set the server looks for `config.json` in the current working directory.

A token callback lets you define what claims are returned when a token request matches a given parameter:

```json
{
    "interactiveLogin": true,
    "httpServer": "NettyWrapper",
    "tokenCallbacks": [
        {
            "issuerId": "issuer1",
            "tokenExpiry": 120,
            "requestMappings": [
                {
                    "requestParam": "code",
                    "match": "code1",
                    "claims": {
                        "sub": "subByCode",
                        "aud": ["audByCode"]
                    }
                }
            ]
        },
        {
            "issuerId": "issuer2",
            "requestMappings": [
                {
                    "requestParam": "someparam",
                    "match": "somevalue",
                    "claims": {
                        "sub": "subBySomeParam",
                        "aud": ["audBySomeParam"]
                    }
                }
            ]
        }
    ]
}
```

A token request to `http://localhost:8080/issuer1/token` with parameter `code` equal to `code1` will return a token with:

```json
{
  "sub": "subByCode",
  "aud": "audByCode",
  "iss": "http://localhost:8080/issuer1",
  ...
}
```

The `match` field supports exact strings, `"*"` (matches any value), and full regular expressions.

Use `${clientId}` (or `${client_id}`) in claim values to insert the requesting client ID dynamically. All form parameters from the token request are available as template variables:

```json
{
    "issuerId": "issuer1",
    "requestMappings": [
        {
            "requestParam": "code",
            "match": "code1",
            "claims": {
                "sub": "${clientId}",
                "aud": ["audByCode"]
            }
        }
    ]
}
```

### Auto-added claims

Every token issued by `DefaultOAuth2TokenCallback` automatically includes the following claims regardless of what you configure:

| Claim | Value | When |
|-------|-------|------|
| `tid` | the `issuerId` (e.g. `"default"`) | always |
| `azp` | the `client_id` from the token request | Authorization Code grant only |

You can override `tid` by including it in your `claims` map. `azp` cannot be overridden for Authorization Code grants as it is added after your claims are applied.

### `aud` claim resolution

When using `DefaultOAuth2TokenCallback` the `aud` claim is resolved in the following order:

1. The `audience` value passed explicitly to the constructor
2. The `audience` parameter from the token request (e.g. in Token Exchange)
3. The non-OIDC scopes from the request (e.g. for Client Credentials with scopes)
4. Falls back to `"default"` if none of the above are present

### HTTPS

#### In unit tests

Generate a keystore automatically:

```kotlin
val ssl = Ssl()
val server = MockOAuth2Server(
    OAuth2Config(httpServer = MockWebServerWrapper(ssl))
)
```

> [!TIP]
> Add `ssl.sslKeystore.keyStore` to your client's truststore to trust the generated certificate.

Bring your own keystore:

```kotlin
val ssl = Ssl(
    SslKeystore(
        keyPassword = "",
        keystoreFile = File("src/test/resources/localhost.p12"),
        keystorePassword = "",
        keystoreType = SslKeystore.KeyStoreType.PKCS12
    )
)
val server = MockOAuth2Server(
    OAuth2Config(httpServer = MockWebServerWrapper(ssl))
)
```

#### In Docker / standalone via JSON_CONFIG

Generate keystore:

```json
{
  "httpServer": {
    "type": "NettyWrapper",
    "ssl": {}
  }
}
```

Bring your own:

```json
{
    "httpServer": {
        "type": "NettyWrapper",
        "ssl": {
            "keyPassword": "",
            "keystoreFile": "src/test/resources/localhost.p12",
            "keystoreType": "PKCS12",
            "keystorePassword": ""
        }
    }
}
```

A ready to use Docker Compose setup with SSL is available at [`docker-compose-ssl.yaml`](docker-compose-ssl.yaml) in the root of this repository.

### CORS

The server automatically adds CORS headers to every response when an `Origin` header is present. No configuration is required.

For regular requests:

```
Access-Control-Allow-Origin: <origin>
Access-Control-Allow-Credentials: true
```

For `OPTIONS` preflight requests:

```
Access-Control-Allow-Headers: <requested-headers>
Access-Control-Allow-Methods: POST, GET, OPTIONS
```

Browser based OAuth2 clients and SPAs can call the token, JWKS and other endpoints directly without any proxy setup.

### Debugger

Point your browser to `http://localhost:8080/default/debugger` to open the OAuth2 client debugger. It implements the Authorization Code Flow and lets you inspect request parameters and token responses interactively.

---

## Configuration Reference

### Standalone ENV variables

| Variable                | Description                                                                                                                                                          |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SERVER_HOSTNAME`       | Hostname to bind to. Defaults to the wildcard address (typically `0.0.0.0` or `::` depending on the JVM and OS).                                                    |
| `SERVER_PORT` or `PORT` | Port to listen on. Defaults to `8080`. `PORT` is also accepted for Heroku compatibility.                                                                             |
| `JSON_CONFIG`           | Full JSON content of `OAuth2Config`. Takes precedence over `JSON_CONFIG_PATH`.                                                                                       |
| `JSON_CONFIG_PATH`      | Absolute path to a JSON file containing `OAuth2Config`.                                                                                                              |
| `LOG_LEVEL`             | Root log level. Defaults to `INFO`.                                                                                                                                  |
| `LOGBACK_CONFIG`        | Path to a custom logback XML file. Overrides the default [logback-standalone.xml](src/main/resources/logback-standalone.xml).                                        |

When neither `JSON_CONFIG` nor `JSON_CONFIG_PATH` is set, the server looks for a file named `config.json` in the current working directory before falling back to defaults.

### JSON_CONFIG properties

| Property             | Description                                                                                                                                                                                                                                                                       |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `interactiveLogin`   | `true` or `false`. Enables a login screen on the `/authorize` endpoint. Defaults to `true` in standalone mode, `false` in library mode.                                                                                                                                           |
| `loginPagePath`      | Path to a custom HTML login page. The page must contain a form that posts `username` and optionally `claims`. See `src/test/resources/login.example.html` for an example.                                                                                                         |
| `staticAssetsPath`   | Path to a directory of static assets served under `/static`. E.g. `http://localhost:8080/static/myimage.svg`.                                                                                                                                                                    |
| `rotateRefreshToken` | `true` or `false`. When `true`, a new refresh token is issued on each refresh grant, invalidating the previous one.                                                                                                                                                               |
| `httpServer`         | The HTTP server implementation to use: `MockWebServerWrapper` (default, supports `takeRequest()`) or `NettyWrapper` (required for HTTPS). Can also be a JSON object: `{"type": "NettyWrapper", "ssl": {...}}`.                                                                    |
| `tokenCallbacks`     | A list of [`RequestMappingTokenCallback`](src/main/kotlin/no/nav/security/mock/oauth2/token/OAuth2TokenCallback.kt) objects that define which claims to return based on request parameters.                                                                                       |

Additional token provider options:

```json
{
  "tokenProvider": {
    "keyProvider": {
      "algorithm": "ES256"
    },
    "systemTime": "2020-01-21T00:00:00Z"
  }
}
```

---

## API Reference

### Well-known endpoints

The first path segment in any URL is the `issuerId`. Both OIDC and OAuth2 AS metadata endpoints are served:

```
GET /{issuerId}/.well-known/openid-configuration
GET /{issuerId}/.well-known/oauth-authorization-server
```

<details>
<summary>View example response for <code>http://localhost:8080/default/.well-known/openid-configuration</code></summary>

```json
{
   "issuer": "http://localhost:8080/default",
   "authorization_endpoint": "http://localhost:8080/default/authorize",
   "token_endpoint": "http://localhost:8080/default/token",
   "jwks_uri": "http://localhost:8080/default/jwks",
   "userinfo_endpoint": "http://localhost:8080/default/userinfo",
   "introspection_endpoint": "http://localhost:8080/default/introspect",
   "revocation_endpoint": "http://localhost:8080/default/revoke",
   "end_session_endpoint": "http://localhost:8080/default/endsession"
}
```

</details>

### Endpoint notes

**Introspect** (`POST /{issuerId}/introspect`) requires an `Authorization` header. Either `Authorization: Bearer <token>` or `Authorization: Basic <credentials>` is accepted. Requests without it will receive `400 invalid_client`.

**Revocation** (`POST /{issuerId}/revoke`) only supports `token_type_hint=refresh_token`. Passing any other value returns `400 unsupported_token_type`.

### Server URL methods (Kotlin/Java API)

```kotlin
server.wellKnownUrl("default")                         // OIDC discovery URL
server.oauth2AuthorizationServerMetadataUrl("default") // OAuth2 AS metadata URL
server.tokenEndpointUrl("default")
server.jwksUrl("default")
server.userInfoUrl("default")
server.introspectUrl("default")
server.revocationEndpointUrl("default")
server.endSessionEndpointUrl("default")
server.baseUrl()                                       // server root URL
```

### Full API documentation

[navikt.github.io/mock-oauth2-server](https://navikt.github.io/mock-oauth2-server/)

---

## 👥 Contact

This project is maintained by [@navikt](https://github.com/navikt).

To raise an issue or question, open an issue in this repository.

For internal NAV contact, use the Slack channel `#nais`.

---

## ✏️ Contributing

Fork the repo, check out a new branch, and build with:

```shell
./gradlew build
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for more details.

---

## ⚖️ License

This library is licensed under the [MIT License](LICENSE.md).
