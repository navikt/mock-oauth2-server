[![Build](https://github.com/navikt/mock-oauth2-server/workflows/Build%20master/badge.svg)](https://github.com/navikt/mock-oauth2-server/actions) [![Maven Central](https://img.shields.io/maven-central/v/no.nav.security/mock-oauth2-server?color=green&logo=Apache%20Maven)](https://search.maven.org/artifact/no.nav.security/mock-oauth2-server) 

:exclamation: 

As of version 0.3.3 the Docker image is published to the [GitHub Container Registry](https://docs.github.com/en/packages/guides/migrating-to-github-container-registry-for-docker-images#domain-changes) and thus can be pulled anonymously from `ghcr.io/navikt/mock-oauth2-server:<version>`. Update your image pulling spells accordingly. 

:exclamation:

# mock-oauth2-server
A scriptable/customizable web server for testing HTTP clients using OAuth2/OpenID Connect or applications with a dependency to a running OAuth2 server (i.e. APIs requiring signed JWTs from a known issuer).  The server also provides the necessary endpoints for token validation (endpoint for JWKS) and ID Provider metadata discovery ("well-known" endpoints providing  server metadata)

**mock-oauth2-server** is written in Kotlin using the great [OkHttp MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver) as the underlying server library and can be used in unit/integration tests in both **Java** and **Kotlin** or in any language as a standalone server in e.g. docker-compose.

Even though the server aims to be compliant with regards to the supported OAuth2/OpenID Connect specifications, you should never use it for anything else than tests. That being said, when developing OAuth2 clients you should always verify that the expected requests are being made in your tests.

## Motivation

The motivation behind this library is to provide a setup such that application developers don't feel the need to disable security in their apps when running tests! If you have any issues with regards to OAuth2 and tokens et. al. and consider to disable "security" when running tests please submit an issue or a PR so that we can all help developers and security to live in harmony once again (if ever..)!

## Features

* **Multi-issuer/Multi-tenancy support**: the server can represent as many different Identity Providers/Token Issuers as you need (with different token issuer names) WITHOUT any setup!
* **Implements OAuth2/OpenID Connect grants/flows**
  * OpenID Connect Authorization Code Flow
  * OAuth2 Client Credentials Grant
  * OAuth2 JWT Bearer Grant (On-Behalf-Of flow)
  * OAuth2 Token Exchange Grant
  * OAuth2 Refresh Token Grant
  * OAuth2 Resource Owner Password Credentials (Password Grant)
    * *usage should be avoided if possible as this grant is considered insecure and [removed in its entirety](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics-13#section-3.4) from OAuth 2.1*
* **Issued JWT tokens are verifiable** through standard mechanisms with OpenID Connect Discovery / OAuth2 Authorization Server Metadata
* **Unit/Integration test support**
  * Start and stop server for each test
  * Sane defaults with minimal setup if you don't need token customization
  * Enqueue expected tokens if you need to customize token claims
  * Enqueue expected responses
  * Verify expected requests made to the server
  * Customizable through exposure of underlying  [OkHttp MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver) 
* **Standalone support** - i.e. run as application in IDE, run inside your app, or as a Docker image (provided)
* **OAuth2 Client Debugger** - e.g. support for triggering OIDC Auth Code Flow and receiving callback in debugger app, view token response from server (intended for standalone support)

## API Documentation

[mock-oauth2-server](https://navikt.github.io/mock-oauth2-server/)

## 📦 Install

**Gradle Kotlin DSL**

Latest version [![Maven Central](https://img.shields.io/maven-central/v/no.nav.security/mock-oauth2-server?color=green&logo=Apache%20Maven)](https://search.maven.org/artifact/no.nav.security/mock-oauth2-server)

```kotlin
testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
```

**Maven**

Latest version [![Maven Central](https://img.shields.io/maven-central/v/no.nav.security/mock-oauth2-server?color=green&logo=Apache%20Maven)](https://search.maven.org/artifact/no.nav.security/mock-oauth2-server)

```xml
<dependency>
  <groupId>no.nav.security</groupId>
  <artifactId>mock-oauth2-server</artifactId>
  <version>${mock-oauth2-server.version}</version>
  <scope>test</scope>
</dependency>
```

**Docker**

Latest version [![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/navikt/mock-oauth2-server?color=green&include_prereleases&label=GitHub%20Package%20Registry&logo=Docker)](https://github.com/navikt/mock-oauth2-server/packages/)

```
docker pull ghcr.io/navikt/mock-oauth2-server:$MOCK_OAUTH2_SERVER_VERSION
```



## ⌨️ Usage

### Well-Known Configuration

The **mock-oauth2-server** will supply different configurations depending on the url used against the server, more specifically the first **path** (or context root) element in your request url will specify the `issuerId`.

A request to `http://localhost:8080/default/.well-known/openid-configuration` will yield an `issuerId` of `default` with the following configuration:

```json
{
   "issuer":"http://localhost:8080/default",
   "authorization_endpoint":"http://localhost:8080/default/authorize",
   "token_endpoint":"http://localhost:8080/default/token",
   "userinfo_endpoint":"http://localhost:8080/default/userinfo",
   "jwks_uri":"http://localhost:8080/default/jwks",
   "introspection_endpoint":"http://localhost:8080/default/introspect",
   "response_types_supported":[
      "query",
      "fragment",
      "form_post"
   ],
   "subject_types_supported":[
      "public"
   ],
   "id_token_signing_alg_values_supported":[
     "ES256",
     "ES384",
     "RS256",
     "RS384",
     "RS512",
     "PS256",
     "PS384",
     "PS512"
   ],
  "code_challenge_methods_supported":[
      "plain", 
      "S256" 
  ]
}
```

The actual issuer value in a JWT will be `iss: "http://localhost:8080/default"`

To use another issuer with id `anotherissuer` simply make a request to `http://localhost:8080/anotherissuer/.well-known/openid-configuration` and the configuration will change accordingly.

### Unit tests

##### Setup test

* Start the server at a random port
* Get url for server metadata/configuration
* Setup your app to use the OAuth2 server metadata and conduct your tests
* Shutdown the server

```kotlin
val server = MockOAuth2Server()
server.start()
// Can be anything you choose - should uniquely identify your issuer if you have several
val issuerId = "default"
// Discovery url to authorization server metadata
val wellKnownUrl = server.wellKnownUrl(issuerId).toString()
// ......
// Setup your app with metadata from wellKnownUrl and do your testing here
// ......
server.shutdown()
```

##### Testing an app requiring user login with OpenID Connect Authorization Code Flow

* Setup test like above
* Make your test HTTP client follow redirects
* Your callback (redirect_uri) endpoint should receive the callback request as required and be able to retrieve a token from the token endpoint.

If you need to get a login for a specific user you can use the `OAuth2TokenCallback` interface to provide your own or set values in the `DefaultOAuth2TokenCallback`

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

 If you need specific claims in the resulting `id_token` - e.g. `acr` or a custom claim you can also use the `OAuth2TokenCallback`:

~~~kotlin
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
~~~

##### Testing an API requiring access_token (e.g. a signed JWT)

```kotlin
val token: SignedJWT = oAuth2Server.issueToken(issuerId, "someclientid", DefaultOAuth2TokenCallback())
//use your favourite HTTP client to invoke your API and attach the serialized token
val request = // ....
request.addHeader("Authorization", "Bearer ${token.serialize()}")
```

##### More examples 

Have a look at some examples in both Java and Kotlin in the src/test directory:
* [Kotlin with the ktor framework](src/test/kotlin/examples/kotlin/ktor)
* [Java with Spring Boot and Spring Security](src/test/java/examples/java/springboot/)

### API

##### Server URLs

You can retrieve URLs from the server with the correct port and issuerId etc. by invoking one of the ` fun *Url(issuerId: String): HttpUrl` functions/methods: 

```kotlin
val server = MockOAuth2Server()
server.start()
val wellKnownUrl = server.wellKnownUrl("yourissuer")
// will result in the following url:
// http://localhost:<a random port>/yourissuer/.well-known/openid-configuration
```

### Standalone server

The standalone server will default to port `8080` and can be started by invoking `main()` in  `StandaloneMockOAuth2Server.kt` (in kotlin) or `StandaloneMockOAuth2ServerKt` (in Java)

On Windows, it's easier to run the server in docker while specifying the host as localhost, e.g. `docker run -p 8080:8080 -h localhost $IMAGE_NAME`

> **Note**
> If you want to check if the server is up and running you can visit `/isalive` and see if you get a 200 in return.

#### Configuration

The standalone server supports the following configuration by `ENV` variables:

| Variable                | Description                                                                                                                                                                                                                                                                                                                                                                 |
|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SERVER_HOSTNAME`       | Lets the standalone server bind to a specific hostname, by default it binds to `0.0.0.0`                                                                                                                                                                                                                                                                                    |
| `SERVER_PORT` or `PORT` | The port that the standalone server will listen to, defaults to `8080`. The `PORT` environment variable may be used to [run the Docker image on Heroku](https://devcenter.heroku.com/articles/container-registry-and-runtime#pushing-an-existing-image) as per the documentation [here](https://devcenter.heroku.com/articles/setting-the-http-port-for-java-applications). |
| `JSON_CONFIG_PATH`      | The absolute path to a json file containing configuration about the OAuth2 part of the server (`OAuth2Config`). More details on the format below.                                                                                                                                                                                                                           |
| `JSON_CONFIG`           | The actual JSON content of `OAuth2Config`, this ENV var takes precedence over the `JSON_CONFIG_PATH` var. More details on the format below.                                                                                                                                                                                                                                 |
| `LOG_LEVEL`             | How verbose the root logging output is, defaults to `INFO`                                                                                                                                                                                                                                                                                                                  |

##### JSON_CONFIG 

The JSON_CONFIG lets you configure the contents of the [`OAuth2Config`](src/main/kotlin/no/nav/security/mock/oauth2/OAuth2Config.kt) class using JSON.


Example:
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
                    "requestParam": "scope",
                    "match": "scope1",
                    "claims": {
                        "sub": "subByScope",
                        "aud": [
                            "audByScope"
                        ]
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
                        "aud": [
                            "audBySomeParam"
                        ]
                    }
                }
            ]
        }
    ]
}
```

A token provider can support different `signing` algorithms. Configure your token provider and
add this to your config with preferred `JWS algorithm`:

```json
{
  "tokenProvider" : {
    "keyProvider" : {
      "algorithm" : "ES256"
    }
  }
}
```

| Property | Description |
| --- | --- |
| `interactiveLogin` | `true` or `false`, enables login screen when redirecting to server `/authorize` endpoint |
| `loginPagePath` | An optional string refering to a html file that is served as login page. This page needs to contain a form that posts a `username` and optionally a `claims` field. See `src/test/resource/login.example.html` as an example.|
| `httpServer`| A string identifying the httpserver to use. Must match one of the following enum values: `MockWebServerWrapper` or `NettyWrapper`|
| `tokenCallbacks` | A list of [`RequestMappingTokenCallback`](src/main/kotlin/no/nav/security/mock/oauth2/token/OAuth2TokenCallback.kt) that lets you specify which token claims to return when a token request matches the specified condition.|

*From the JSON example above:* 

A token request to `http://localhost:8080/issuer1/token` with parameter `scope` equal to `scope1` will match the first `tokenCallback`:

```json
{
    "issuerId": "issuer1",
    "tokenExpiry": 120,
    "requestMappings": [
        {
            "requestParam": "scope",
            "match": "scope1",
            "claims": {
                "sub": "subByScope",
                "aud": [
                    "audByScope"
                ]
            }
        }
    ]
}
```

and return a token response containing a token with the following claims:

```json
{
  "sub": "subByScope",
  "aud": "audByScope",
  "nbf": 1616416942,
  "iss": "http://localhost:54905/issuer1",
  "exp": 1616417062,
  "iat": 1616416942,
  "jti": "28697333-6f25-4b1f-b2c2-409ce010933a"
}
```


#### Docker 

Build to local docker daemon

```gradle
./gradlew -Djib.from.platforms=linux/amd64 jibDockerBuild # or alternatively -Djib.from.platforms=linux/arm64 for ARM64
```

Run container

```gradle
docker run -p 8080:8080 $IMAGE_NAME
```

#### Docker-Compose

In order to get container-to-container networking to work smoothly alongside browser interaction you must specify a host entry in your `hosts` file, `127.0.0.1 host.docker.internal` and set `hostname` in the **mock-oauth2-server** service in your `docker-compose.yaml` file:

```yaml
version: '3.7'
services:
  your_app:
    build: .
    ports:
      - 8080:8080
  mock-oauth2-server:
    image: ghcr.io/navikt/mock-oauth2-server:$MOCK_OAUTH2_SERVER_VERSION
    ports:
      - 8080:8080
    hostname: host.docker.internal
```

#### Debugger

The debugger is a OAuth2 client implementing the `authorization_code` flow with a UI for debugging (e.g. request parameters).
Point your browser to [http://localhost:8080/default/debugger](http://localhost:8080/default/debugger) to check it out.

### Enabling HTTPS

In order to enable HTTPS you can either provide your own keystore or let the server generate one for you.

#### Unit tests

You need to supply the server with an SSL config, in order to do that you must specify your chosen server type in `OAuth2Config` and 
pass in the SSL config to your server.

*Generate keystore:*
```kotlin
val ssl = Ssl()
val server = MockOAuth2Server(
    OAuth2Config(httpServer = MockWebServerWrapper(ssl))
)
```
*This will generate a SSL certificate for `localhost` and can be added to your client's truststore by getting the ssl config:
`ssl.sslKeystore.keyStore`*

*Bring your own:*
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

#### Docker / Standalone mode - JSON_CONFIG

In order to enable HTTPS for the server in Docker or standalone mode
you can either make the server generate the keystore or bring your own.

*Generate keystore:*

```json
{
  "httpServer" : {
    "type" : "NettyWrapper",
    "ssl" : {}
  }
}
```

*Bring your own:*

```json

{
    "httpServer" : {
        "type" : "NettyWrapper",
        "ssl" : {
            "keyPassword" : "",
            "keystoreFile" : "src/test/resources/localhost.p12",
            "keystoreType" : "PKCS12",
            "keystorePassword" : "" 
        }
    }
}
```

## Upgrading the gradle wrapper
Find the newest version of gradle here: https://gradle.org/releases/
Then run this command:
```./gradlew wrapper --gradle-version $gradleVersion```

Remeber to also update the gradle version in the build.gradle.kts file
``` gradleVersion = "$gradleVersion"```


## 👥 Contact

This project is currently maintained by the organisation [@navikt](https://github.com/navikt).

If you need to raise an issue or question about this library, please create an issue here and tag it with the appropriate label.

For contact requests within the [@navikt](https://github.com/navikt) org, you can use the Slack channel #pig_sikkerhet

If you need to contact anyone directly, please see contributors.

## ✏️ Contributing

To get started, please fork the repo and checkout a new branch. You can then build the library with the Gradle wrapper

```shell script
./gradlew build
```

See more info in [CONTRIBUTING.md](CONTRIBUTING.md)

## ⚖️ License
This library is licensed under the [MIT License](LICENSE)

