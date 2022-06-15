---
title: OAuth2TokenProviderDeserializer
---
//[mock-oauth2-server](../../../../index.html)/[no.nav.security.mock.oauth2](../../index.html)/[OAuth2Config](../index.html)/[OAuth2TokenProviderDeserializer](index.html)



# OAuth2TokenProviderDeserializer



[jvm]\
class [OAuth2TokenProviderDeserializer](index.html) : JsonDeserializer&lt;[OAuth2TokenProvider](../../../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html)&gt;



## Constructors


| | |
|---|---|
| [OAuth2TokenProviderDeserializer](-o-auth2-token-provider-deserializer.html) | [jvm]<br>fun [OAuth2TokenProviderDeserializer](-o-auth2-token-provider-deserializer.html)() |


## Types


| Name | Summary |
|---|---|
| [KeyProviderConfig](-key-provider-config/index.html) | [jvm]<br>data class [KeyProviderConfig](-key-provider-config/index.html)(val initialKeys: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, val algorithm: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [ProviderConfig](-provider-config/index.html) | [jvm]<br>data class [ProviderConfig](-provider-config/index.html)(val keyProvider: [OAuth2Config.OAuth2TokenProviderDeserializer.KeyProviderConfig](-key-provider-config/index.html)?) |


## Functions


| Name | Summary |
|---|---|
| [deserialize](deserialize.html) | [jvm]<br>open override fun [deserialize](deserialize.html)(p: JsonParser, ctxt: DeserializationContext?): [OAuth2TokenProvider](../../../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html)<br>open fun [deserialize](index.html#-666974444%2FFunctions%2F863300109)(p0: JsonParser, p1: DeserializationContext, p2: [OAuth2TokenProvider](../../../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html)): [OAuth2TokenProvider](../../../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html) |
| [deserializeWithType](../-o-auth2-http-server-deserializer/index.html#303597567%2FFunctions%2F863300109) | [jvm]<br>open fun [deserializeWithType](../-o-auth2-http-server-deserializer/index.html#303597567%2FFunctions%2F863300109)(p0: JsonParser, p1: DeserializationContext, p2: TypeDeserializer): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)<br>open fun [deserializeWithType](index.html#1139390765%2FFunctions%2F863300109)(p0: JsonParser, p1: DeserializationContext, p2: TypeDeserializer, p3: [OAuth2TokenProvider](../../../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html)): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [findBackReference](../-o-auth2-http-server-deserializer/index.html#1438700766%2FFunctions%2F863300109) | [jvm]<br>open fun [findBackReference](../-o-auth2-http-server-deserializer/index.html#1438700766%2FFunctions%2F863300109)(p0: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): SettableBeanProperty |
| [getAbsentValue](../-o-auth2-http-server-deserializer/index.html#-390729380%2FFunctions%2F863300109) | [jvm]<br>open override fun [getAbsentValue](../-o-auth2-http-server-deserializer/index.html#-390729380%2FFunctions%2F863300109)(p0: DeserializationContext): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [getDelegatee](../-o-auth2-http-server-deserializer/index.html#-1050556161%2FFunctions%2F863300109) | [jvm]<br>open fun [getDelegatee](../-o-auth2-http-server-deserializer/index.html#-1050556161%2FFunctions%2F863300109)(): JsonDeserializer&lt;*&gt; |
| [getEmptyAccessPattern](../-o-auth2-http-server-deserializer/index.html#2004370652%2FFunctions%2F863300109) | [jvm]<br>open fun [getEmptyAccessPattern](../-o-auth2-http-server-deserializer/index.html#2004370652%2FFunctions%2F863300109)(): AccessPattern |
| [getEmptyValue](../-o-auth2-http-server-deserializer/index.html#2066120599%2FFunctions%2F863300109) | [jvm]<br>~~open~~ ~~fun~~ [~~getEmptyValue~~](../-o-auth2-http-server-deserializer/index.html#2066120599%2FFunctions%2F863300109)~~(~~~~)~~~~:~~ [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)<br>open fun [getEmptyValue](../-o-auth2-http-server-deserializer/index.html#-1621668596%2FFunctions%2F863300109)(p0: DeserializationContext): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [getKnownPropertyNames](../-o-auth2-http-server-deserializer/index.html#808020811%2FFunctions%2F863300109) | [jvm]<br>open fun [getKnownPropertyNames](../-o-auth2-http-server-deserializer/index.html#808020811%2FFunctions%2F863300109)(): [MutableCollection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-collection/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; |
| [getNullAccessPattern](../-o-auth2-http-server-deserializer/index.html#-96796966%2FFunctions%2F863300109) | [jvm]<br>open override fun [getNullAccessPattern](../-o-auth2-http-server-deserializer/index.html#-96796966%2FFunctions%2F863300109)(): AccessPattern |
| [getNullValue](../-o-auth2-http-server-deserializer/index.html#-1752557675%2FFunctions%2F863300109) | [jvm]<br>~~open~~ ~~fun~~ [~~getNullValue~~](../-o-auth2-http-server-deserializer/index.html#-1752557675%2FFunctions%2F863300109)~~(~~~~)~~~~:~~ [OAuth2TokenProvider](../../../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html)<br>open override fun [getNullValue](../-o-auth2-http-server-deserializer/index.html#432781262%2FFunctions%2F863300109)(p0: DeserializationContext): [OAuth2TokenProvider](../../../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html) |
| [getObjectIdReader](../-o-auth2-http-server-deserializer/index.html#911426750%2FFunctions%2F863300109) | [jvm]<br>open fun [getObjectIdReader](../-o-auth2-http-server-deserializer/index.html#911426750%2FFunctions%2F863300109)(): ObjectIdReader |
| [handledType](../-o-auth2-http-server-deserializer/index.html#1063755675%2FFunctions%2F863300109) | [jvm]<br>open fun [handledType](../-o-auth2-http-server-deserializer/index.html#1063755675%2FFunctions%2F863300109)(): [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;*&gt; |
| [isCachable](../-o-auth2-http-server-deserializer/index.html#1654902530%2FFunctions%2F863300109) | [jvm]<br>open fun [isCachable](../-o-auth2-http-server-deserializer/index.html#1654902530%2FFunctions%2F863300109)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [logicalType](../-o-auth2-http-server-deserializer/index.html#1638353390%2FFunctions%2F863300109) | [jvm]<br>open fun [logicalType](../-o-auth2-http-server-deserializer/index.html#1638353390%2FFunctions%2F863300109)(): LogicalType |
| [replaceDelegatee](../-o-auth2-http-server-deserializer/index.html#79105129%2FFunctions%2F863300109) | [jvm]<br>open fun [replaceDelegatee](../-o-auth2-http-server-deserializer/index.html#79105129%2FFunctions%2F863300109)(p0: JsonDeserializer&lt;*&gt;): JsonDeserializer&lt;*&gt; |
| [supportsUpdate](../-o-auth2-http-server-deserializer/index.html#336340330%2FFunctions%2F863300109) | [jvm]<br>open fun [supportsUpdate](../-o-auth2-http-server-deserializer/index.html#336340330%2FFunctions%2F863300109)(p0: DeserializationConfig): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [unwrappingDeserializer](../-o-auth2-http-server-deserializer/index.html#-1815728544%2FFunctions%2F863300109) | [jvm]<br>open fun [unwrappingDeserializer](../-o-auth2-http-server-deserializer/index.html#-1815728544%2FFunctions%2F863300109)(p0: NameTransformer): JsonDeserializer&lt;[OAuth2TokenProvider](../../../no.nav.security.mock.oauth2.token/-o-auth2-token-provider/index.html)&gt; |

