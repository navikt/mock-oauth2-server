---
title: OAuth2HttpServer
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.http](../index.html)/[OAuth2HttpServer](index.html)



# OAuth2HttpServer



[jvm]\
interface [OAuth2HttpServer](index.html) : [AutoCloseable](https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html)



## Functions


| Name | Summary |
|---|---|
| [close](close.html) | [jvm]<br>open override fun [close](close.html)() |
| [port](port.html) | [jvm]<br>abstract fun [port](port.html)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [start](start.html) | [jvm]<br>open fun [start](start.html)(requestHandler: [RequestHandler](../index.html#111237332%2FClasslikes%2F863300109)): [OAuth2HttpServer](index.html)<br>open fun [start](start.html)(port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 0, requestHandler: [RequestHandler](../index.html#111237332%2FClasslikes%2F863300109) = { OAuth2HttpResponse(status = 404, body = &quot;no requesthandler configured&quot;) }): [OAuth2HttpServer](index.html)<br>abstract fun [start](start.html)(inetAddress: [InetAddress](https://docs.oracle.com/javase/8/docs/api/java/net/InetAddress.html), port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), requestHandler: [RequestHandler](../index.html#111237332%2FClasslikes%2F863300109)): [OAuth2HttpServer](index.html) |
| [stop](stop.html) | [jvm]<br>abstract fun [stop](stop.html)(): [OAuth2HttpServer](index.html) |
| [url](url.html) | [jvm]<br>abstract fun [url](url.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl |


## Inheritors


| Name |
|---|
| [MockWebServerWrapper](../-mock-web-server-wrapper/index.html) |
| [NettyWrapper](../-netty-wrapper/index.html) |

