---
title: SslKeystore
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.http](../index.html)/[SslKeystore](index.html)



# SslKeystore



[jvm]\
class [SslKeystore](index.html)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(val keyPassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;&quot;, val keyStore: [KeyStore](https://docs.oracle.com/javase/8/docs/api/java/security/KeyStore.html) = generate(&quot;localhost&quot;, keyPassword))



## Constructors


| | |
|---|---|
| [SslKeystore](-ssl-keystore.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [SslKeystore](-ssl-keystore.html)(keyPassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), keystoreFile: [File](https://docs.oracle.com/javase/8/docs/api/java/io/File.html), keystoreType: [SslKeystore.KeyStoreType](-key-store-type/index.html) = KeyStoreType.PKCS12, keystorePassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;&quot;) |
| [SslKeystore](-ssl-keystore.html) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [SslKeystore](-ssl-keystore.html)(keyPassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;&quot;, keyStore: [KeyStore](https://docs.oracle.com/javase/8/docs/api/java/security/KeyStore.html) = generate(&quot;localhost&quot;, keyPassword)) |


## Types


| Name | Summary |
|---|---|
| [Companion](-companion/index.html) | [jvm]<br>object [Companion](-companion/index.html) |
| [KeyStoreType](-key-store-type/index.html) | [jvm]<br>enum [KeyStoreType](-key-store-type/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[SslKeystore.KeyStoreType](-key-store-type/index.html)&gt; |


## Properties


| Name | Summary |
|---|---|
| [keyPassword](key-password.html) | [jvm]<br>val [keyPassword](key-password.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [keyStore](key-store.html) | [jvm]<br>val [keyStore](key-store.html): [KeyStore](https://docs.oracle.com/javase/8/docs/api/java/security/KeyStore.html) |

