---
title: DefaultOAuth2TokenCallback
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.token](../index.html)/[DefaultOAuth2TokenCallback](index.html)/[DefaultOAuth2TokenCallback](-default-o-auth2-token-callback.html)



# DefaultOAuth2TokenCallback



[jvm]\




@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)



fun [DefaultOAuth2TokenCallback](-default-o-auth2-token-callback.html)(issuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;default&quot;, subject: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = UUID.randomUUID().toString(), typeHeader: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = JOSEObjectType.JWT.type, audience: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null, claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; = emptyMap(), expiry: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) = 3600)




