---
title: CorsInterceptor
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.http](../index.html)/[CorsInterceptor](index.html)



# CorsInterceptor



[jvm]\
class [CorsInterceptor](index.html)(allowedMethods: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf(&quot;POST&quot;, &quot;GET&quot;, &quot;OPTIONS&quot;)) : [ResponseInterceptor](../-response-interceptor/index.html)



## Constructors


| | |
|---|---|
| [CorsInterceptor](-cors-interceptor.html) | [jvm]<br>fun [CorsInterceptor](-cors-interceptor.html)(allowedMethods: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf(&quot;POST&quot;, &quot;GET&quot;, &quot;OPTIONS&quot;)) |


## Types


| Name | Summary |
|---|---|
| [HeaderNames](-header-names/index.html) | [jvm]<br>object [HeaderNames](-header-names/index.html) |


## Functions


| Name | Summary |
|---|---|
| [intercept](intercept.html) | [jvm]<br>open override fun [intercept](intercept.html)(request: [OAuth2HttpRequest](../-o-auth2-http-request/index.html), response: [OAuth2HttpResponse](../-o-auth2-http-response/index.html)): [OAuth2HttpResponse](../-o-auth2-http-response/index.html) |

