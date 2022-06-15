---
title: TokenExchangeGrant
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.grant](../index.html)/[TokenExchangeGrant](index.html)



# TokenExchangeGrant



[jvm]\
class [TokenExchangeGrant](index.html)(val subjectTokenType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val subjectToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val audience: [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;) : AuthorizationGrant



## Constructors


| | |
|---|---|
| [TokenExchangeGrant](-token-exchange-grant.html) | [jvm]<br>fun [TokenExchangeGrant](-token-exchange-grant.html)(subjectTokenType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), subjectToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), audience: [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;) |


## Types


| Name | Summary |
|---|---|
| [Companion](-companion/index.html) | [jvm]<br>object [Companion](-companion/index.html) |


## Functions


| Name | Summary |
|---|---|
| [getType](index.html#-244146955%2FFunctions%2F863300109) | [jvm]<br>open fun [getType](index.html#-244146955%2FFunctions%2F863300109)(): GrantType |
| [toParameters](to-parameters.html) | [jvm]<br>open override fun [toParameters](to-parameters.html)(): [MutableMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt; |


## Properties


| Name | Summary |
|---|---|
| [audience](audience.html) | [jvm]<br>val [audience](audience.html): [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [subjectToken](subject-token.html) | [jvm]<br>val [subjectToken](subject-token.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [subjectTokenType](subject-token-type.html) | [jvm]<br>val [subjectTokenType](subject-token-type.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

