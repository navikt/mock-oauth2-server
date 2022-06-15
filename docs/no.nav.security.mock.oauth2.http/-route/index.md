---
title: Route
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.http](../index.html)/[Route](index.html)



# Route



[jvm]\
interface [Route](index.html) : [Function1](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-function1/index.html)&lt;[OAuth2HttpRequest](../-o-auth2-http-request/index.html), [OAuth2HttpResponse](../-o-auth2-http-response/index.html)&gt;



## Types


| Name | Summary |
|---|---|
| [Builder](-builder/index.html) | [jvm]<br>class [Builder](-builder/index.html) |


## Functions


| Name | Summary |
|---|---|
| [invoke](index.html#548827542%2FFunctions%2F863300109) | [jvm]<br>abstract operator fun [invoke](index.html#548827542%2FFunctions%2F863300109)(p1: [OAuth2HttpRequest](../-o-auth2-http-request/index.html)): [OAuth2HttpResponse](../-o-auth2-http-response/index.html) |
| [match](match.html) | [jvm]<br>abstract fun [match](match.html)(request: [OAuth2HttpRequest](../-o-auth2-http-request/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |


## Inheritors


| Name |
|---|
| [DebuggerRequestHandler](../../no.nav.security.mock.oauth2.debugger/-debugger-request-handler/index.html) |

