---
title: no.nav.security.mock.oauth2.grant
---
//[mock-oauth2-server](../../index.html)/[no.nav.security.mock.oauth2.grant](index.html)



# Package no.nav.security.mock.oauth2.grant



## Types


| Name | Summary |
|---|---|
| [GrantHandler](-grant-handler/index.html) | [jvm]<br>interface [GrantHandler](-grant-handler/index.html) |
| [RefreshToken](index.html#-1134906885%2FClasslikes%2F863300109) | [jvm]<br>typealias [RefreshToken](index.html#-1134906885%2FClasslikes%2F863300109) = [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [TokenExchangeGrant](-token-exchange-grant/index.html) | [jvm]<br>class [TokenExchangeGrant](-token-exchange-grant/index.html)(val subjectTokenType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val subjectToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val audience: [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;) : AuthorizationGrant |


## Functions


| Name | Summary |
|---|---|
| [subjectToken](subject-token.html) | [jvm]<br>fun TokenRequest.[subjectToken](subject-token.html)(): SignedJWT |
| [tokenExchangeGrant](token-exchange-grant.html) | [jvm]<br>fun TokenRequest.[tokenExchangeGrant](token-exchange-grant.html)(): [TokenExchangeGrant](-token-exchange-grant/index.html) |


## Properties


| Name | Summary |
|---|---|
| [TOKEN_EXCHANGE](-t-o-k-e-n_-e-x-c-h-a-n-g-e.html) | [jvm]<br>val [TOKEN_EXCHANGE](-t-o-k-e-n_-e-x-c-h-a-n-g-e.html): GrantType |

