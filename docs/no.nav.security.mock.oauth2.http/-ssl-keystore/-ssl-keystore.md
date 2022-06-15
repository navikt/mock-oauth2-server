---
title: SslKeystore
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.http](../index.html)/[SslKeystore](index.html)/[SslKeystore](-ssl-keystore.html)



# SslKeystore



[jvm]\




@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)



fun [SslKeystore](-ssl-keystore.html)(keyPassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), keystoreFile: [File](https://docs.oracle.com/javase/8/docs/api/java/io/File.html), keystoreType: [SslKeystore.KeyStoreType](-key-store-type/index.html) = KeyStoreType.PKCS12, keystorePassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;&quot;)





@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)



fun [SslKeystore](-ssl-keystore.html)(keyPassword: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;&quot;, keyStore: [KeyStore](https://docs.oracle.com/javase/8/docs/api/java/security/KeyStore.html) = generate(&quot;localhost&quot;, keyPassword))




