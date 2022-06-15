---
title: start
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2](../index.html)/[MockOAuth2Server](index.html)/[start](start.html)



# start



[jvm]\




@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)



fun [start](start.html)(port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 0)



Starts the [MockOAuth2Server](index.html) on the localhost interface.



## Parameters


jvm

| | |
|---|---|
| port | The port the server should listen on, a value of 0 (which is the default) selects any available port. |



## Throws


| | |
|---|---|
| [no.nav.security.mock.oauth2.OAuth2Exception](../-o-auth2-exception/index.html) | Runtime error if unable to start server. |




[jvm]\
fun [start](start.html)(inetAddress: [InetAddress](https://docs.oracle.com/javase/8/docs/api/java/net/InetAddress.html), port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))



Starts the [MockOAuth2Server](index.html) on the given [inetAddress](start.html) IP address at the given [port](start.html).



## Parameters


jvm

| | |
|---|---|
| port | The port that the server should listen on, a value of 0 selects any available port. |
| inetAddress | The IP address that the server should bind to. |



## Throws


| | |
|---|---|
| [no.nav.security.mock.oauth2.OAuth2Exception](../-o-auth2-exception/index.html) | Runtime error if unable to start server. |



