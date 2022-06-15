---
title: start
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.http](../index.html)/[OAuth2HttpServer](index.html)/[start](start.html)



# start



[jvm]\
open fun [start](start.html)(requestHandler: [RequestHandler](../index.html#111237332%2FClasslikes%2F863300109)): [OAuth2HttpServer](index.html)

open fun [start](start.html)(port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 0, requestHandler: [RequestHandler](../index.html#111237332%2FClasslikes%2F863300109) = { OAuth2HttpResponse(status = 404, body = &quot;no requesthandler configured&quot;) }): [OAuth2HttpServer](index.html)

abstract fun [start](start.html)(inetAddress: [InetAddress](https://docs.oracle.com/javase/8/docs/api/java/net/InetAddress.html), port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), requestHandler: [RequestHandler](../index.html#111237332%2FClasslikes%2F863300109)): [OAuth2HttpServer](index.html)




