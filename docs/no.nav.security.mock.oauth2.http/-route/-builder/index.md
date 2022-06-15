---
title: Builder
---
//[mock-oauth2-server](../../../../index.html)/[no.nav.security.mock.oauth2.http](../../index.html)/[Route](../index.html)/[Builder](index.html)



# Builder



[jvm]\
class [Builder](index.html)



## Constructors


| | |
|---|---|
| [Builder](-builder.html) | [jvm]<br>fun [Builder](-builder.html)() |


## Functions


| Name | Summary |
|---|---|
| [any](any.html) | [jvm]<br>fun [any](any.html)(vararg path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestHandler: [RequestHandler](../../index.html#111237332%2FClasslikes%2F863300109)): [Route.Builder](index.html) |
| [attach](attach.html) | [jvm]<br>fun [attach](attach.html)(vararg route: [Route](../index.html)): [Route.Builder](index.html) |
| [build](build.html) | [jvm]<br>fun [build](build.html)(): [Route](../index.html) |
| [exceptionHandler](exception-handler.html) | [jvm]<br>fun [exceptionHandler](exception-handler.html)(exceptionHandler: ExceptionHandler): [Route.Builder](index.html) |
| [get](get.html) | [jvm]<br>fun [get](get.html)(vararg path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestHandler: [RequestHandler](../../index.html#111237332%2FClasslikes%2F863300109)): [Route.Builder](index.html) |
| [interceptors](interceptors.html) | [jvm]<br>fun [interceptors](interceptors.html)(vararg interceptor: [Interceptor](../../-interceptor/index.html)): [Route.Builder](index.html) |
| [options](options.html) | [jvm]<br>fun [options](options.html)(requestHandler: [RequestHandler](../../index.html#111237332%2FClasslikes%2F863300109)): [Route.Builder](index.html) |
| [post](post.html) | [jvm]<br>fun [post](post.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestHandler: [RequestHandler](../../index.html#111237332%2FClasslikes%2F863300109)): [Route.Builder](index.html) |
| [put](put.html) | [jvm]<br>fun [put](put.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestHandler: [RequestHandler](../../index.html#111237332%2FClasslikes%2F863300109)): [Route.Builder](index.html) |

