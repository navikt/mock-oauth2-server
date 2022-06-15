---
title: TemplateMapper
---
//[mock-oauth2-server](../../../index.html)/[no.nav.security.mock.oauth2.templates](../index.html)/[TemplateMapper](index.html)



# TemplateMapper



[jvm]\
class [TemplateMapper](index.html)(config: Configuration)



## Constructors


| | |
|---|---|
| [TemplateMapper](-template-mapper.html) | [jvm]<br>fun [TemplateMapper](-template-mapper.html)(config: Configuration) |


## Types


| Name | Summary |
|---|---|
| [Companion](-companion/index.html) | [jvm]<br>object [Companion](-companion/index.html) |


## Functions


| Name | Summary |
|---|---|
| [authorizationCodeResponseHtml](authorization-code-response-html.html) | [jvm]<br>fun [authorizationCodeResponseHtml](authorization-code-response-html.html)(redirectUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), state: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [debuggerCallbackHtml](debugger-callback-html.html) | [jvm]<br>fun [debuggerCallbackHtml](debugger-callback-html.html)(tokenRequest: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), tokenResponse: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [debuggerErrorHtml](debugger-error-html.html) | [jvm]<br>fun [debuggerErrorHtml](debugger-error-html.html)(debuggerUrl: HttpUrl, stacktrace: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [debuggerFormHtml](debugger-form-html.html) | [jvm]<br>fun [debuggerFormHtml](debugger-form-html.html)(url: HttpUrl, clientAuthMethod: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [loginHtml](login-html.html) | [jvm]<br>fun [loginHtml](login-html.html)(oAuth2HttpRequest: [OAuth2HttpRequest](../../no.nav.security.mock.oauth2.http/-o-auth2-http-request/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

