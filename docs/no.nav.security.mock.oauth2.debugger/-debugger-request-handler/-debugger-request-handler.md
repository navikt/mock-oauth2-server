---
title: DebuggerRequestHandler
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.debugger](../index.html)/[DebuggerRequestHandler](index.html)/[DebuggerRequestHandler](-debugger-request-handler.html)



# DebuggerRequestHandler



[jvm]\
fun [DebuggerRequestHandler](-debugger-request-handler.html)(sessionManager: [SessionManager](../-session-manager/index.html) = SessionManager(), route: [Route](../../no.nav.security.mock.oauth2.http/-route/index.html) = routes {
        exceptionHandler(handle(sessionManager))
        debuggerForm(sessionManager)
        debuggerCallback(sessionManager)
    })




