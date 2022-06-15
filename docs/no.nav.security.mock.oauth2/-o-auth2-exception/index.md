---
title: OAuth2Exception
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2](../index.html)/[OAuth2Exception](index.html)



# OAuth2Exception



[jvm]\
class [OAuth2Exception](index.html)(val errorObject: ErrorObject?, msg: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)?) : [RuntimeException](https://docs.oracle.com/javase/8/docs/api/java/lang/RuntimeException.html)



## Constructors


| | |
|---|---|
| [OAuth2Exception](-o-auth2-exception.html) | [jvm]<br>fun [OAuth2Exception](-o-auth2-exception.html)(msg: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [OAuth2Exception](-o-auth2-exception.html) | [jvm]<br>fun [OAuth2Exception](-o-auth2-exception.html)(msg: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)?) |
| [OAuth2Exception](-o-auth2-exception.html) | [jvm]<br>fun [OAuth2Exception](-o-auth2-exception.html)(errorObject: ErrorObject?, msg: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [OAuth2Exception](-o-auth2-exception.html) | [jvm]<br>fun [OAuth2Exception](-o-auth2-exception.html)(errorObject: ErrorObject?, msg: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)?) |


## Functions


| Name | Summary |
|---|---|
| [addSuppressed](index.html#282858770%2FFunctions%2F863300109) | [jvm]<br>fun [addSuppressed](index.html#282858770%2FFunctions%2F863300109)(p0: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) |
| [fillInStackTrace](index.html#-1102069925%2FFunctions%2F863300109) | [jvm]<br>open fun [fillInStackTrace](index.html#-1102069925%2FFunctions%2F863300109)(): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html) |
| [getLocalizedMessage](index.html#1043865560%2FFunctions%2F863300109) | [jvm]<br>open fun [getLocalizedMessage](index.html#1043865560%2FFunctions%2F863300109)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [getStackTrace](index.html#2050903719%2FFunctions%2F863300109) | [jvm]<br>open fun [getStackTrace](index.html#2050903719%2FFunctions%2F863300109)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[StackTraceElement](https://docs.oracle.com/javase/8/docs/api/java/lang/StackTraceElement.html)&gt; |
| [getSuppressed](index.html#672492560%2FFunctions%2F863300109) | [jvm]<br>fun [getSuppressed](index.html#672492560%2FFunctions%2F863300109)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)&gt; |
| [initCause](index.html#-418225042%2FFunctions%2F863300109) | [jvm]<br>open fun [initCause](index.html#-418225042%2FFunctions%2F863300109)(p0: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html) |
| [printStackTrace](index.html#-1769529168%2FFunctions%2F863300109) | [jvm]<br>open fun [printStackTrace](index.html#-1769529168%2FFunctions%2F863300109)()<br>open fun [printStackTrace](index.html#1841853697%2FFunctions%2F863300109)(p0: [PrintStream](https://docs.oracle.com/javase/8/docs/api/java/io/PrintStream.html))<br>open fun [printStackTrace](index.html#1175535278%2FFunctions%2F863300109)(p0: [PrintWriter](https://docs.oracle.com/javase/8/docs/api/java/io/PrintWriter.html)) |
| [setStackTrace](index.html#2135801318%2FFunctions%2F863300109) | [jvm]<br>open fun [setStackTrace](index.html#2135801318%2FFunctions%2F863300109)(p0: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[StackTraceElement](https://docs.oracle.com/javase/8/docs/api/java/lang/StackTraceElement.html)&gt;) |


## Properties


| Name | Summary |
|---|---|
| [cause](index.html#-654012527%2FProperties%2F863300109) | [jvm]<br>open val [cause](index.html#-654012527%2FProperties%2F863300109): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? |
| [errorObject](error-object.html) | [jvm]<br>val [errorObject](error-object.html): ErrorObject? |
| [message](index.html#1824300659%2FProperties%2F863300109) | [jvm]<br>open val [message](index.html#1824300659%2FProperties%2F863300109): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |

