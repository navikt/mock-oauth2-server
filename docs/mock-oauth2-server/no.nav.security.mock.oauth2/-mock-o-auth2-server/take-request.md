---
title: takeRequest
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2](../index.html)/[MockOAuth2Server](index.html)/[takeRequest](take-request.html)



# takeRequest



[jvm]\




@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)



fun [takeRequest](take-request.html)(timeout: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) = 2, unit: [TimeUnit](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeUnit.html) = TimeUnit.SECONDS): RecordedRequest



Awaits the next HTTP request (waiting up to the specified wait time if necessary), removes it from the queue, and returns it. Callers should use this to verify the request was sent as intended within the given time.



## Parameters


jvm

| | |
|---|---|
| timeout | How long to wait before giving up, in units of [unit](take-request.html) |
| unit | A [TimeUnit](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeUnit.html) determining how to interpret the [timeout](take-request.html) parameter |




