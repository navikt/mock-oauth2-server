---
title: NettyWrapper
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.http](../index.html)/[NettyWrapper](index.html)



# NettyWrapper



[jvm]\
class [NettyWrapper](index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(val ssl: [Ssl](../-ssl/index.html)? = null) : [OAuth2HttpServer](../-o-auth2-http-server/index.html)



## Constructors


| | |
|---|---|
| [NettyWrapper](-netty-wrapper.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [NettyWrapper](-netty-wrapper.html)(ssl: [Ssl](../-ssl/index.html)? = null) |


## Functions


| Name | Summary |
|---|---|
| [close](../-o-auth2-http-server/close.html) | [jvm]<br>open override fun [close](../-o-auth2-http-server/close.html)() |
| [port](port.html) | [jvm]<br>open override fun [port](port.html)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [start](../-o-auth2-http-server/start.html) | [jvm]<br>open fun [start](../-o-auth2-http-server/start.html)(requestHandler: [RequestHandler](../index.html#111237332%2FClasslikes%2F863300109)): [OAuth2HttpServer](../-o-auth2-http-server/index.html)<br>open fun [start](../-o-auth2-http-server/start.html)(port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 0, requestHandler: [RequestHandler](../index.html#111237332%2FClasslikes%2F863300109) = { OAuth2HttpResponse(status = 404, body = &quot;no requesthandler configured&quot;) }): [OAuth2HttpServer](../-o-auth2-http-server/index.html)<br>open override fun [start](start.html)(inetAddress: [InetAddress](https://docs.oracle.com/javase/8/docs/api/java/net/InetAddress.html), port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), requestHandler: [RequestHandler](../index.html#111237332%2FClasslikes%2F863300109)): [OAuth2HttpServer](../-o-auth2-http-server/index.html) |
| [stop](stop.html) | [jvm]<br>open override fun [stop](stop.html)(): [NettyWrapper](index.html) |
| [url](url.html) | [jvm]<br>open override fun [url](url.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpUrl |


## Properties


| Name | Summary |
|---|---|
| [ssl](ssl.html) | [jvm]<br>val [ssl](ssl.html): [Ssl](../-ssl/index.html)? = null |

