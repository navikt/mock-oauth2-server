---
title: DebuggerRequestHandler
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.debugger](../index.html)/[DebuggerRequestHandler](index.html)



# DebuggerRequestHandler



[jvm]\
class [DebuggerRequestHandler](index.html)(sessionManager: [SessionManager](../-session-manager/index.html) = SessionManager(), route: [Route](../../no.nav.security.mock.oauth2.http/-route/index.html) = routes {
        exceptionHandler(handle(sessionManager))
        debuggerForm(sessionManager)
        debuggerCallback(sessionManager)
    }) : [Route](../../no.nav.security.mock.oauth2.http/-route/index.html)



## Constructors


| | |
|---|---|
| [DebuggerRequestHandler](-debugger-request-handler.html) | [jvm]<br>fun [DebuggerRequestHandler](-debugger-request-handler.html)(sessionManager: [SessionManager](../-session-manager/index.html) = SessionManager(), route: [Route](../../no.nav.security.mock.oauth2.http/-route/index.html) = routes {         exceptionHandler(handle(sessionManager))         debuggerForm(sessionManager)         debuggerCallback(sessionManager)     }) |


## Functions


| Name | Summary |
|---|---|
| [invoke](../../no.nav.security.mock.oauth2.http/-route/index.html#548827542%2FFunctions%2F863300109) | [jvm]<br>open operator override fun [invoke](../../no.nav.security.mock.oauth2.http/-route/index.html#548827542%2FFunctions%2F863300109)(p1: [OAuth2HttpRequest](../../no.nav.security.mock.oauth2.http/-o-auth2-http-request/index.html)): [OAuth2HttpResponse](../../no.nav.security.mock.oauth2.http/-o-auth2-http-response/index.html) |
| [match](../../no.nav.security.mock.oauth2.http/-route/match.html) | [jvm]<br>open override fun [match](../../no.nav.security.mock.oauth2.http/-route/match.html)(request: [OAuth2HttpRequest](../../no.nav.security.mock.oauth2.http/-o-auth2-http-request/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |

