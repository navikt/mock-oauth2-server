<p align="center">
  <a href="https://github.com/navikt/mock-oauth2-server/actions"><img src="https://github.com/navikt/mock-oauth2-server/workflows/Build%20master/badge.svg" alt="Build"></a>
  <a href="https://search.maven.org/artifact/no.nav.security/mock-oauth2-server"><img src="https://img.shields.io/maven-central/v/no.nav.security/mock-oauth2-server?color=green&logo=Apache%20Maven" alt="Maven Central"></a>
  <a href="https://github.com/navikt/mock-oauth2-server/packages/"><img src="https://img.shields.io/github/v/release/navikt/mock-oauth2-server?color=green&include_prereleases&label=Docker&logo=Docker" alt="Docker"></a>
  <a href="LICENSE.md"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/kotlin-2.x-blue.svg?logo=kotlin" alt="Kotlin"></a>
</p>

<h1 align="center">mock-oauth2-server</h1>
<p align="center">Scriptable OAuth2/OpenID Connect server for JVM tests and Docker Compose.</p>

> [!NOTE]
> The latest stable version is shown in the Maven Central badge above. See the [release notes](https://github.com/navikt/mock-oauth2-server/releases) for what has changed.

---

## Table of Contents

- [Quick Start](#quick-start)
- [What it does](#what-it-does)
- [Supported Flows](#supported-flows)
- [Using it in JVM tests](#using-it-in-jvm-tests)
  - [Issuing tokens directly](#issuing-tokens-directly)
  - [Testing Authorization Code Flow](#testing-authorization-code-flow-user-login)
  - [Spring Boot](#spring-boot)
- [Running it with Docker](#running-it-with-docker)
  - [Standalone](#standalone)
  - [Docker Compose](#docker-compose)
- [Customizing tokens](#customizing-tokens)
- [More features](#more-features): HTTPS, CORS, Debugger
- [Configuration Reference](#configuration-reference)
- [API Reference](#api-reference)
- [Troubleshooting](#troubleshooting)
- [Contact](#contact) · [Contributing](#contributing) · [License](#license) · [Migration guide](#migration-guide)

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

In Java:

```java
MockOAuth2Server server = new MockOAuth2Server();
server.start();
SignedJWT token = server.issueToken("default", "user123", "my-api");
String wellKnownUrl = server.wellKnownUrl("default").toString();
server.shutdown();
```

Or run it as a Docker container:

```shell
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

- OpenID Connect Authorization Code Flow, with PKCE (`plain` and `S256`)
- OAuth2 Client Credentials Grant
- OAuth2 JWT Bearer Grant (On-Behalf-Of flow)
- OAuth2 Token Exchange Grant
- OAuth2 Refresh Token Grant
- OAuth2 Resource Owner Password Credentials Grant
  - avoid it if you can: [RFC 9700](https://datatracker.ietf.org/doc/html/rfc9700#section-2.4) says it "MUST NOT be used", and OAuth 2.1 removes it

Only `response_type=code` is supported. The implicit and hybrid flows are not.

Issued JWT tokens are verifiable through standard mechanisms via OpenID Connect Discovery and OAuth2 Authorization Server Metadata endpoints. Multi-issuer setups are supported with no configuration — the first path segment in any request URL determines the issuer.

---

## Using it in JVM tests

### Starting and stopping the server

Use `withMockOAuth2Server` to start and stop the server around a block:

```kotlin
withMockOAuth2Server {
    val wellKnownUrl = wellKnownUrl("default").toString()
    // configure your app and run your test here
}
```

### Issuing tokens directly

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

### Testing Authorization Code Flow (user login)

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

### Spring Boot

Start the server before the application context and point Spring Security at the issuer:

```java
@SpringBootTest
class MyApiTest {
    static MockOAuth2Server server = new MockOAuth2Server();

    @BeforeAll
    static void start() { server.start(); }

    @AfterAll
    static void stop() { server.shutdown(); }

    @DynamicPropertySource
    static void oauth2(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> server.issuerUrl("default").toString());
    }
}
```

Then send `server.issueToken("default", "user123", "my-api").serialize()` as the bearer token. See the [Spring Boot examples](src/test/java/examples/java/springboot/) for login and resource server setups.

### Verifying requests made to the server

> [!NOTE]
> `takeRequest()` is only available when using `MockWebServerWrapper` (the default). It throws `UnsupportedOperationException` with `NettyWrapper`.

```kotlin
val request = server.takeRequest()
assertThat(request.path).contains("/default/token")
```

### Controlling token time

```kotlin
val server = MockOAuth2Server(
    config = OAuth2Config(
        tokenProvider = OAuth2TokenProvider(systemTime = Instant.parse("2020-01-21T00:00:00Z"))
    )
)
val token = server.issueToken(issuerId = "issuer1")
// token has iat=2020-01-21T00:00:00Z
```

### Multi-issuer setup

The first path segment in any request URL is the `issuerId`. No configuration needed:

```text
http://localhost:8080/issuer-a/.well-known/openid-configuration  → issuerId = issuer-a
http://localhost:8080/issuer-b/.well-known/openid-configuration  → issuerId = issuer-b
```

Each issuer has its own discovery document, token endpoint, and JWKS.

### More examples

- [Kotlin with the ktor framework](src/test/kotlin/examples/kotlin/ktor)
- [Java with Spring Boot and Spring Security](src/test/java/examples/java/springboot/)

## Running it with Docker

### Standalone

The standalone server defaults to port `8080`.

**Run with Docker:**

```shell
docker run -p 8080:8080 ghcr.io/navikt/mock-oauth2-server:$MOCK_OAUTH2_SERVER_VERSION
```

Images are also tagged by major and minor version, so you can pin to a release stream:

| Tag | Resolves to |
|-----|-------------|
| `ghcr.io/navikt/mock-oauth2-server:6.1.1` | exact version |
| `ghcr.io/navikt/mock-oauth2-server:6.1` | latest 6.1.x |
| `ghcr.io/navikt/mock-oauth2-server:6` | latest 6.x.x |

**Build locally:**

```shell
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

## Customizing tokens

### Request mappings via JSON_CONFIG

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
                    "match": "*",
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

A token request to `http://localhost:8080/issuer1/token` with any `code` parameter will match the first `tokenCallback` and return a token with:

```json
{
  "sub": "subByCode",
  "aud": ["audByCode"],
  "iss": "http://localhost:8080/issuer1",
  ...
}
```

The `match` field supports exact strings, `"*"` (matches any value), and full regular expressions. If the pattern is an invalid regular expression, it does not throw — regex evaluation is skipped, but exact-string matching still applies.

Use `${clientId}` (or `${client_id}`) in claim values to insert the requesting client ID dynamically. All form parameters from the token request are available as template variables:

```json
{
    "issuerId": "issuer1",
    "requestMappings": [
        {
            "requestParam": "code",
            "match": "*",
            "claims": {
                "sub": "${clientId}",
                "aud": ["audByCode"]
            }
        }
    ]
}
```

#### Built-in template variables

In addition to form parameters, the following built-in variables are always available:

| Variable | Value |
|----------|-------|
| `${clientId}` / `${client_id}` | The `client_id` from the token request |

#### Interactive login: matching and templating on the login username

When `interactiveLogin` is enabled, `requestMappings` can match on the username submitted at the login page using `"requestParam": "subject"`. The login username is also available as `${subject}` in claim values.

This allows a single `JSON_CONFIG` to serve different claim sets per user without a custom login page:

```json
{
    "interactiveLogin": true,
    "tokenCallbacks": [
        {
            "issuerId": "default",
            "requestMappings": [
                {
                    "requestParam": "subject",
                    "match": "alice",
                    "claims": {
                        "role": "admin",
                        "preferred_username": "${subject}"
                    }
                },
                {
                    "requestParam": "subject",
                    "match": ".*",
                    "claims": {
                        "role": "user",
                        "preferred_username": "${subject}"
                    }
                }
            ]
        }
    ]
}
```

**Claim precedence** when combining `requestMappings` with interactive login:

1. Claims set by a matching `requestMapping` take priority.
2. Claims submitted on the login page can add new claims but cannot overwrite claims already set by the mapping.

**Template variable precedence** (highest wins):

1. `client_id` / `clientId` — always authoritative
2. Token POST body form parameters
3. `${subject}` and other built-in variables

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

## More features

### HTTPS

#### In unit tests

Generate a keystore automatically:

```kotlin
val ssl = Ssl()
val server = MockOAuth2Server(
    OAuth2Config(httpServer = MockWebServerWrapper(ssl))
)
```

Your app must trust the certificate, see [Make your app trust the server's certificate](#make-your-app-trust-the-servers-certificate).

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

#### Make your app trust the server's certificate

The server uses a self-signed certificate, so the HTTP client in the app you are testing rejects it until you add it to that client's truststore. Until then, fetching the JWKS fails with `PKIX path building failed`.

**With your own keystore (works for Spring MVC, Spring WebFlux and other JVM clients):** create a truststore that holds the server's certificate, and set it when the test JVM starts. `keytool -list -keystore localhost.p12 -storepass ''` shows the alias of the key entry, which is `1` in this repository's `localhost.p12`.

```bash
keytool -exportcert -rfc -keystore localhost.p12 -storetype PKCS12 -storepass '' -alias 1 -file localhost.crt
keytool -importcert -noprompt -alias mock-oauth2-server -file localhost.crt -keystore truststore.p12 -storetype PKCS12 -storepass changeit
```

```kotlin
// build.gradle.kts
tasks.test {
    systemProperty("javax.net.ssl.trustStore", file("src/test/resources/truststore.p12").absolutePath)
    systemProperty("javax.net.ssl.trustStorePassword", "changeit")
}
```

For an app running in Docker, set `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStore=/truststore.p12 -Djavax.net.ssl.trustStorePassword=changeit`.

- Use the truststore, not the keystore: with an empty password the JVM finds no trusted certificate in `localhost.p12` and fails with `the trustAnchors parameter must be non-empty`.
- Set the properties when the JVM starts. Setting them later with `System.setProperty` has no effect once the JVM or Reactor Netty has created its default SSL context.
- This replaces the JVM's default truststore, so other HTTPS calls in the tests no longer trust public certificate authorities.

**With a generated keystore (`Ssl()`) in Spring MVC:** there is no file, so trust the certificate in code before your app fetches the JWKS. Spring MVC (`RestTemplate`) uses `HttpsURLConnection`:

```kotlin
val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
trustManagerFactory.init(ssl.sslKeystore.keyStore)
val sslContext = SSLContext.getInstance("TLS")
sslContext.init(null, trustManagerFactory.trustManagers, null)
HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
```

`SSLContext.setDefault(sslContext)` on its own is not enough. Spring WebFlux ignores both, so use your own keystore and the truststore above.

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

```text
Access-Control-Allow-Origin: <origin>
Access-Control-Allow-Credentials: true
```

For `OPTIONS` preflight requests:

```text
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
| `loginPagePath`      | Path to a custom HTML login page, or a classpath resource prefixed with `classpath:`. The page must contain a form that posts `username` and optionally `claims`. See `src/test/resources/login.example.html` for an example.                                                     |
| `staticAssetsPath`   | Optional path to a directory of static assets served under `/static`, or a classpath directory prefixed with `classpath:`. Omit to disable `/static/*`. E.g. `http://localhost:8080/static/myimage.svg`.                                                                          |
| `rotateRefreshToken` | `true` or `false`. When `true`, a new refresh token is issued on each refresh grant, invalidating the previous one.                                                                                                                                                               |
| `httpServer`         | The HTTP server implementation to use: `MockWebServerWrapper` (default, supports `takeRequest()`) or `NettyWrapper`. Can also be a JSON object with HTTPS enabled, for either type: `{"type": "NettyWrapper", "ssl": {...}}`.                                                       |
| `tokenCallbacks`     | A list of [`RequestMappingTokenCallback`](src/main/kotlin/no/nav/security/mock/oauth2/token/OAuth2TokenCallback.kt) objects that define which claims to return based on request parameters.                                                                                       |

`staticAssetsPath` must not be blank, the classpath root or outside the classpath. Only regular files within the configured directory are served; symlinks pointing outside it are not followed. With `classpath:`, symlinks are not followed at all.

To load the login page and assets from the classpath, for example from `src/test/resources` in your project or from a jar. Resources are looked up with the context class loader of the thread that creates the server:

```kotlin
MockOAuth2Server(
    OAuth2Config(
        interactiveLogin = true,
        loginPagePath = "classpath:login.html",
        staticAssetsPath = "classpath:static",
    ),
)
```

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

`keyProvider.algorithm` sets the algorithm used to sign every token. It also sets the key type published at `/{issuerId}/jwks` and the single value advertised in `id_token_signing_alg_values_supported` in the discovery documents. Supported values are `RS256` (default), `RS384`, `RS512`, `PS256`, `PS384`, `PS512`, `ES256` and `ES384`.

---

## API Reference

### Well-known endpoints

The first path segment in any URL is the `issuerId`. Both OIDC and OAuth2 AS metadata endpoints are served:

```text
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

**Introspect** (`POST /{issuerId}/introspect`) requires an `Authorization` header. Either `Authorization: Bearer <token>` or `Authorization: Basic <credentials>` is accepted. Requests without it will receive `401 invalid_client`.

**Revocation** (`POST /{issuerId}/revoke`) removes the token if it is a refresh token issued by the server. `token_type_hint` is optional and may be `refresh_token` or `access_token`; access tokens are self-contained JWTs and stay valid until they expire. Any other hint returns `400 unsupported_token_type`. An unknown token returns `200`, as RFC 7009 requires.

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

## Troubleshooting

**Your app rejects the token because of the issuer.** The `iss` claim is built from the host used to call the server. Your app and whoever fetches the token must use the same host, see [Docker Compose](#docker-compose).

**The authorize endpoint shows a login page.** The standalone server and Docker image enable `interactiveLogin` by default. Set `JSON_CONFIG={"interactiveLogin": false}` to get a redirect with a code directly.

**The code exchange returns `invalid_grant`.** Authorization codes can only be used once, so a retried or repeated token request fails.

**Fetching the JWKS over HTTPS fails with `PKIX path building failed`.** Your app doesn't trust the server's certificate, see [Make your app trust the server's certificate](#make-your-app-trust-the-servers-certificate).

---

## Contact

This project is maintained by [@navikt](https://github.com/navikt).

To raise an issue or question, open an issue in this repository.

For internal NAV contact, use the Slack channel `#nais`.

---

## Contributing

Fork the repo, check out a new branch, and build with:

```shell
./gradlew build
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for more details.

---

## License

This library is licensed under the [MIT License](LICENSE.md).

---

## Migration guide

See [MIGRATION.md](MIGRATION.md) for upgrade instructions between versions.
