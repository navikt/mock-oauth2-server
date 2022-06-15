---
title: RequestMapping
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.token](../index.html)/[RequestMapping](index.html)



# RequestMapping



[jvm]\
data class [RequestMapping](index.html)(requestParam: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), match: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;*&quot;, val claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; = emptyMap(), val typeHeader: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = JOSEObjectType.JWT.type)



## Constructors


| | |
|---|---|
| [RequestMapping](-request-mapping.html) | [jvm]<br>fun [RequestMapping](-request-mapping.html)(requestParam: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), match: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;*&quot;, claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; = emptyMap(), typeHeader: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = JOSEObjectType.JWT.type) |


## Functions


| Name | Summary |
|---|---|
| [isMatch](is-match.html) | [jvm]<br>fun [isMatch](is-match.html)(tokenRequest: TokenRequest): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |


## Properties


| Name | Summary |
|---|---|
| [claims](claims.html) | [jvm]<br>val [claims](claims.html): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; |
| [typeHeader](type-header.html) | [jvm]<br>val [typeHeader](type-header.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

