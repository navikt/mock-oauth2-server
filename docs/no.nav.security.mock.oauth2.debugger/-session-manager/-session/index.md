---
title: Session
---
//[mock-oauth2-server](../../../../index.html)/[no.nav.security.mock.oauth2.debugger](../../index.html)/[SessionManager](../index.html)/[Session](index.html)



# Session



[jvm]\
class [Session](index.html)(encryptionKey: [SecretKey](https://docs.oracle.com/javase/8/docs/api/javax/crypto/SecretKey.html), val request: [OAuth2HttpRequest](../../../no.nav.security.mock.oauth2.http/-o-auth2-http-request/index.html))



## Constructors


| | |
|---|---|
| [Session](-session.html) | [jvm]<br>fun [Session](-session.html)(encryptionKey: [SecretKey](https://docs.oracle.com/javase/8/docs/api/javax/crypto/SecretKey.html), request: [OAuth2HttpRequest](../../../no.nav.security.mock.oauth2.http/-o-auth2-http-request/index.html)) |


## Types


| Name | Summary |
|---|---|
| [Companion](-companion/index.html) | [jvm]<br>object [Companion](-companion/index.html) |


## Functions


| Name | Summary |
|---|---|
| [asCookie](as-cookie.html) | [jvm]<br>fun [asCookie](as-cookie.html)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [get](get.html) | [jvm]<br>operator fun [get](get.html)(key: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [putAll](put-all.html) | [jvm]<br>fun [putAll](put-all.html)(map: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;) |
| [set](set.html) | [jvm]<br>operator fun [set](set.html)(key: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |


## Properties


| Name | Summary |
|---|---|
| [parameters](parameters.html) | [jvm]<br>val [parameters](parameters.html): [MutableMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [request](request.html) | [jvm]<br>val [request](request.html): [OAuth2HttpRequest](../../../no.nav.security.mock.oauth2.http/-o-auth2-http-request/index.html) |

