---
title: WellKnown
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.http](../index.html)/[WellKnown](index.html)



# WellKnown



[jvm]\
data class [WellKnown](index.html)(val issuer: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val authorizationEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val endSessionEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val tokenEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val userInfoEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val jwksUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val introspectionEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val responseTypesSupported: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf(&quot;query&quot;, &quot;fragment&quot;, &quot;form_post&quot;), val subjectTypesSupported: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf(&quot;public&quot;), val idTokenSigningAlgValuesSupported: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = (KeyGenerator.ecAlgorithmFamily + KeyGenerator.rsaAlgorithmFamily).map { it.name }.toList())



## Constructors


| | |
|---|---|
| [WellKnown](-well-known.html) | [jvm]<br>fun [WellKnown](-well-known.html)(issuer: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), authorizationEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), endSessionEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), tokenEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), userInfoEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), jwksUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), introspectionEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), responseTypesSupported: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf(&quot;query&quot;, &quot;fragment&quot;, &quot;form_post&quot;), subjectTypesSupported: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf(&quot;public&quot;), idTokenSigningAlgValuesSupported: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = (KeyGenerator.ecAlgorithmFamily + KeyGenerator.rsaAlgorithmFamily).map { it.name }.toList()) |


## Properties


| Name | Summary |
|---|---|
| [authorizationEndpoint](authorization-endpoint.html) | [jvm]<br>val [authorizationEndpoint](authorization-endpoint.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [endSessionEndpoint](end-session-endpoint.html) | [jvm]<br>val [endSessionEndpoint](end-session-endpoint.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [idTokenSigningAlgValuesSupported](id-token-signing-alg-values-supported.html) | [jvm]<br>val [idTokenSigningAlgValuesSupported](id-token-signing-alg-values-supported.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [introspectionEndpoint](introspection-endpoint.html) | [jvm]<br>val [introspectionEndpoint](introspection-endpoint.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [issuer](issuer.html) | [jvm]<br>val [issuer](issuer.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [jwksUri](jwks-uri.html) | [jvm]<br>val [jwksUri](jwks-uri.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [responseTypesSupported](response-types-supported.html) | [jvm]<br>val [responseTypesSupported](response-types-supported.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [subjectTypesSupported](subject-types-supported.html) | [jvm]<br>val [subjectTypesSupported](subject-types-supported.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [tokenEndpoint](token-endpoint.html) | [jvm]<br>val [tokenEndpoint](token-endpoint.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [userInfoEndpoint](user-info-endpoint.html) | [jvm]<br>val [userInfoEndpoint](user-info-endpoint.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

