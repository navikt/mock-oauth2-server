---
title: no.nav.security.mock.oauth2.extensions
---
//[mock-oauth2-server](../../index.html)/[no.nav.security.mock.oauth2.extensions](index.html)



# Package no.nav.security.mock.oauth2.extensions



## Types


| Name | Summary |
|---|---|
| [OAuth2Endpoints](-o-auth2-endpoints/index.html) | [jvm]<br>object [OAuth2Endpoints](-o-auth2-endpoints/index.html) |


## Functions


| Name | Summary |
|---|---|
| [asOAuth2HttpRequest](as-o-auth2-http-request.html) | [jvm]<br>fun RecordedRequest.[asOAuth2HttpRequest](as-o-auth2-http-request.html)(): [OAuth2HttpRequest](../no.nav.security.mock.oauth2.http/-o-auth2-http-request/index.html) |
| [authorizationCode](authorization-code.html) | [jvm]<br>fun TokenRequest.[authorizationCode](authorization-code.html)(): AuthorizationCode |
| [clientAuthentication](client-authentication.html) | [jvm]<br>fun HTTPRequest.[clientAuthentication](client-authentication.html)(): ClientAuthentication |
| [clientIdAsString](client-id-as-string.html) | [jvm]<br>fun TokenRequest.[clientIdAsString](client-id-as-string.html)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [endsWith](ends-with.html) | [jvm]<br>fun HttpUrl.[endsWith](ends-with.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [expiresIn](expires-in.html) | [jvm]<br>fun SignedJWT.[expiresIn](expires-in.html)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [grant](grant.html) | [jvm]<br>inline fun &lt;[T](grant.html) : AuthorizationGrant&gt; TokenRequest.[grant](grant.html)(type: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[T](grant.html)&gt;): [T](grant.html) |
| [grantType](grant-type.html) | [jvm]<br>fun TokenRequest.[grantType](grant-type.html)(): GrantType |
| [isAuthorizationEndpointUrl](is-authorization-endpoint-url.html) | [jvm]<br>fun HttpUrl.[isAuthorizationEndpointUrl](is-authorization-endpoint-url.html)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isDebuggerCallbackUrl](is-debugger-callback-url.html) | [jvm]<br>fun HttpUrl.[isDebuggerCallbackUrl](is-debugger-callback-url.html)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isDebuggerUrl](is-debugger-url.html) | [jvm]<br>fun HttpUrl.[isDebuggerUrl](is-debugger-url.html)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isEndSessionEndpointUrl](is-end-session-endpoint-url.html) | [jvm]<br>fun HttpUrl.[isEndSessionEndpointUrl](is-end-session-endpoint-url.html)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isIntrospectUrl](is-introspect-url.html) | [jvm]<br>fun HttpUrl.[isIntrospectUrl](is-introspect-url.html)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isJwksUrl](is-jwks-url.html) | [jvm]<br>fun HttpUrl.[isJwksUrl](is-jwks-url.html)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isPrompt](is-prompt.html) | [jvm]<br>fun AuthenticationRequest.[isPrompt](is-prompt.html)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [issuerId](issuer-id.html) | [jvm]<br>fun HttpUrl.[issuerId](issuer-id.html)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [isTokenEndpointUrl](is-token-endpoint-url.html) | [jvm]<br>fun HttpUrl.[isTokenEndpointUrl](is-token-endpoint-url.html)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isUserInfoUrl](is-user-info-url.html) | [jvm]<br>fun HttpUrl.[isUserInfoUrl](is-user-info-url.html)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isWellKnownUrl](is-well-known-url.html) | [jvm]<br>fun HttpUrl.[isWellKnownUrl](is-well-known-url.html)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [removeAllEncodedQueryParams](remove-all-encoded-query-params.html) | [jvm]<br>fun HttpUrl.Builder.[removeAllEncodedQueryParams](remove-all-encoded-query-params.html)(vararg params: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl.Builder |
| [requirePrivateKeyJwt](require-private-key-jwt.html) | [jvm]<br>fun ClientAuthentication.[requirePrivateKeyJwt](require-private-key-jwt.html)(requiredAudience: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), maxLifetimeSeconds: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)): PrivateKeyJWT |
| [scopesWithoutOidcScopes](scopes-without-oidc-scopes.html) | [jvm]<br>fun TokenRequest.[scopesWithoutOidcScopes](scopes-without-oidc-scopes.html)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [toAuthorizationEndpointUrl](to-authorization-endpoint-url.html) | [jvm]<br>fun HttpUrl.[toAuthorizationEndpointUrl](to-authorization-endpoint-url.html)(): HttpUrl |
| [toDebuggerCallbackUrl](to-debugger-callback-url.html) | [jvm]<br>fun HttpUrl.[toDebuggerCallbackUrl](to-debugger-callback-url.html)(): HttpUrl |
| [toDebuggerUrl](to-debugger-url.html) | [jvm]<br>fun HttpUrl.[toDebuggerUrl](to-debugger-url.html)(): HttpUrl |
| [toEndSessionEndpointUrl](to-end-session-endpoint-url.html) | [jvm]<br>fun HttpUrl.[toEndSessionEndpointUrl](to-end-session-endpoint-url.html)(): HttpUrl |
| [toIntrospectUrl](to-introspect-url.html) | [jvm]<br>fun HttpUrl.[toIntrospectUrl](to-introspect-url.html)(): HttpUrl |
| [toIssuerUrl](to-issuer-url.html) | [jvm]<br>fun HttpUrl.[toIssuerUrl](to-issuer-url.html)(): HttpUrl |
| [toJwksUrl](to-jwks-url.html) | [jvm]<br>fun HttpUrl.[toJwksUrl](to-jwks-url.html)(): HttpUrl |
| [tokenExchangeGrantOrNull](token-exchange-grant-or-null.html) | [jvm]<br>fun TokenRequest.[tokenExchangeGrantOrNull](token-exchange-grant-or-null.html)(): [TokenExchangeGrant](../no.nav.security.mock.oauth2.grant/-token-exchange-grant/index.html)? |
| [toOAuth2AuthorizationServerMetadataUrl](to-o-auth2-authorization-server-metadata-url.html) | [jvm]<br>fun HttpUrl.[toOAuth2AuthorizationServerMetadataUrl](to-o-auth2-authorization-server-metadata-url.html)(): HttpUrl |
| [toTokenEndpointUrl](to-token-endpoint-url.html) | [jvm]<br>fun HttpUrl.[toTokenEndpointUrl](to-token-endpoint-url.html)(): HttpUrl |
| [toUserInfoUrl](to-user-info-url.html) | [jvm]<br>fun HttpUrl.[toUserInfoUrl](to-user-info-url.html)(): HttpUrl |
| [toWellKnownUrl](to-well-known-url.html) | [jvm]<br>fun HttpUrl.[toWellKnownUrl](to-well-known-url.html)(): HttpUrl |
| [verifyPkce](verify-pkce.html) | [jvm]<br>fun AuthenticationRequest.[verifyPkce](verify-pkce.html)(tokenRequest: TokenRequest) |
| [verifySignatureAndIssuer](verify-signature-and-issuer.html) | [jvm]<br>fun SignedJWT.[verifySignatureAndIssuer](verify-signature-and-issuer.html)(issuer: Issuer, jwkSet: JWKSet, jwsAlgorithm: JWSAlgorithm = JWSAlgorithm.RS256): JWTClaimsSet |

