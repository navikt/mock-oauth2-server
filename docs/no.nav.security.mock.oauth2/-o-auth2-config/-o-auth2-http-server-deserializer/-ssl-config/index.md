---
title: SslConfig
---
//[mock-oauth2-server](../../../../../index.html)/[no.nav.security.mock.oauth2](../../../index.html)/[OAuth2Config](../../index.html)/[OAuth2HttpServerDeserializer](../index.html)/[SslConfig](index.html)



# SslConfig



[jvm]\
data class [SslConfig](index.html)(val keyPassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;&quot;, val keystoreFile: [File](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)? = null, val keystoreType: [SslKeystore.KeyStoreType](../../../../no.nav.security.mock.oauth2.http/-ssl-keystore/-key-store-type/index.html) = SslKeystore.KeyStoreType.PKCS12, val keystorePassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;&quot;)



## Constructors


| | |
|---|---|
| [SslConfig](-ssl-config.html) | [jvm]<br>fun [SslConfig](-ssl-config.html)(keyPassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;&quot;, keystoreFile: [File](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)? = null, keystoreType: [SslKeystore.KeyStoreType](../../../../no.nav.security.mock.oauth2.http/-ssl-keystore/-key-store-type/index.html) = SslKeystore.KeyStoreType.PKCS12, keystorePassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;&quot;) |


## Functions


| Name | Summary |
|---|---|
| [ssl](ssl.html) | [jvm]<br>fun [ssl](ssl.html)(): [Ssl](../../../../no.nav.security.mock.oauth2.http/-ssl/index.html) |


## Properties


| Name | Summary |
|---|---|
| [keyPassword](key-password.html) | [jvm]<br>val [keyPassword](key-password.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [keystoreFile](keystore-file.html) | [jvm]<br>val [keystoreFile](keystore-file.html): [File](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)? = null |
| [keystorePassword](keystore-password.html) | [jvm]<br>val [keystorePassword](keystore-password.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [keystoreType](keystore-type.html) | [jvm]<br>val [keystoreType](keystore-type.html): [SslKeystore.KeyStoreType](../../../../no.nav.security.mock.oauth2.http/-ssl-keystore/-key-store-type/index.html) |

