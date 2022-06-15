---
title: OAuth2HttpServerDeserializer
---
//[mock-oauth2-server](../../../../index.html)/[no.nav.security.mock.oauth2](../../index.html)/[OAuth2Config](../index.html)/[OAuth2HttpServerDeserializer](index.html)



# OAuth2HttpServerDeserializer



[jvm]\
class [OAuth2HttpServerDeserializer](index.html) : JsonDeserializer&lt;[OAuth2HttpServer](../../../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html)&gt;



## Constructors


| | |
|---|---|
| [OAuth2HttpServerDeserializer](-o-auth2-http-server-deserializer.html) | [jvm]<br>fun [OAuth2HttpServerDeserializer](-o-auth2-http-server-deserializer.html)() |


## Types


| Name | Summary |
|---|---|
| [ServerConfig](-server-config/index.html) | [jvm]<br>data class [ServerConfig](-server-config/index.html)(val type: [OAuth2Config.OAuth2HttpServerDeserializer.ServerType](-server-type/index.html), val ssl: [OAuth2Config.OAuth2HttpServerDeserializer.SslConfig](-ssl-config/index.html)? = null) |
| [ServerType](-server-type/index.html) | [jvm]<br>enum [ServerType](-server-type/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[OAuth2Config.OAuth2HttpServerDeserializer.ServerType](-server-type/index.html)&gt; |
| [SslConfig](-ssl-config/index.html) | [jvm]<br>data class [SslConfig](-ssl-config/index.html)(val keyPassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;&quot;, val keystoreFile: [File](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)? = null, val keystoreType: [SslKeystore.KeyStoreType](../../../no.nav.security.mock.oauth2.http/-ssl-keystore/-key-store-type/index.html) = SslKeystore.KeyStoreType.PKCS12, val keystorePassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;&quot;) |


## Functions


| Name | Summary |
|---|---|
| [deserialize](deserialize.html) | [jvm]<br>open override fun [deserialize](deserialize.html)(p: JsonParser, ctxt: DeserializationContext): [OAuth2HttpServer](../../../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html)<br>open fun [deserialize](index.html#-961449278%2FFunctions%2F863300109)(p0: JsonParser, p1: DeserializationContext, p2: [OAuth2HttpServer](../../../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html)): [OAuth2HttpServer](../../../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html) |
| [deserializeWithType](index.html#303597567%2FFunctions%2F863300109) | [jvm]<br>open fun [deserializeWithType](index.html#303597567%2FFunctions%2F863300109)(p0: JsonParser, p1: DeserializationContext, p2: TypeDeserializer): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)<br>open fun [deserializeWithType](index.html#-1185719973%2FFunctions%2F863300109)(p0: JsonParser, p1: DeserializationContext, p2: TypeDeserializer, p3: [OAuth2HttpServer](../../../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html)): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [findBackReference](index.html#1438700766%2FFunctions%2F863300109) | [jvm]<br>open fun [findBackReference](index.html#1438700766%2FFunctions%2F863300109)(p0: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): SettableBeanProperty |
| [getAbsentValue](index.html#-390729380%2FFunctions%2F863300109) | [jvm]<br>open override fun [getAbsentValue](index.html#-390729380%2FFunctions%2F863300109)(p0: DeserializationContext): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [getDelegatee](index.html#-1050556161%2FFunctions%2F863300109) | [jvm]<br>open fun [getDelegatee](index.html#-1050556161%2FFunctions%2F863300109)(): JsonDeserializer&lt;*&gt; |
| [getEmptyAccessPattern](index.html#2004370652%2FFunctions%2F863300109) | [jvm]<br>open fun [getEmptyAccessPattern](index.html#2004370652%2FFunctions%2F863300109)(): AccessPattern |
| [getEmptyValue](index.html#2066120599%2FFunctions%2F863300109) | [jvm]<br>~~open~~ ~~fun~~ [~~getEmptyValue~~](index.html#2066120599%2FFunctions%2F863300109)~~(~~~~)~~~~:~~ [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)<br>open fun [getEmptyValue](index.html#-1621668596%2FFunctions%2F863300109)(p0: DeserializationContext): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [getKnownPropertyNames](index.html#808020811%2FFunctions%2F863300109) | [jvm]<br>open fun [getKnownPropertyNames](index.html#808020811%2FFunctions%2F863300109)(): [MutableCollection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-collection/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; |
| [getNullAccessPattern](index.html#-96796966%2FFunctions%2F863300109) | [jvm]<br>open override fun [getNullAccessPattern](index.html#-96796966%2FFunctions%2F863300109)(): AccessPattern |
| [getNullValue](index.html#-1752557675%2FFunctions%2F863300109) | [jvm]<br>~~open~~ ~~fun~~ [~~getNullValue~~](index.html#-1752557675%2FFunctions%2F863300109)~~(~~~~)~~~~:~~ [OAuth2HttpServer](../../../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html)<br>open override fun [getNullValue](index.html#432781262%2FFunctions%2F863300109)(p0: DeserializationContext): [OAuth2HttpServer](../../../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html) |
| [getObjectIdReader](index.html#911426750%2FFunctions%2F863300109) | [jvm]<br>open fun [getObjectIdReader](index.html#911426750%2FFunctions%2F863300109)(): ObjectIdReader |
| [handledType](index.html#1063755675%2FFunctions%2F863300109) | [jvm]<br>open fun [handledType](index.html#1063755675%2FFunctions%2F863300109)(): [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;*&gt; |
| [isCachable](index.html#1654902530%2FFunctions%2F863300109) | [jvm]<br>open fun [isCachable](index.html#1654902530%2FFunctions%2F863300109)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [logicalType](index.html#1638353390%2FFunctions%2F863300109) | [jvm]<br>open fun [logicalType](index.html#1638353390%2FFunctions%2F863300109)(): LogicalType |
| [replaceDelegatee](index.html#79105129%2FFunctions%2F863300109) | [jvm]<br>open fun [replaceDelegatee](index.html#79105129%2FFunctions%2F863300109)(p0: JsonDeserializer&lt;*&gt;): JsonDeserializer&lt;*&gt; |
| [supportsUpdate](index.html#336340330%2FFunctions%2F863300109) | [jvm]<br>open fun [supportsUpdate](index.html#336340330%2FFunctions%2F863300109)(p0: DeserializationConfig): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [unwrappingDeserializer](index.html#-1815728544%2FFunctions%2F863300109) | [jvm]<br>open fun [unwrappingDeserializer](index.html#-1815728544%2FFunctions%2F863300109)(p0: NameTransformer): JsonDeserializer&lt;[OAuth2HttpServer](../../../no.nav.security.mock.oauth2.http/-o-auth2-http-server/index.html)&gt; |

