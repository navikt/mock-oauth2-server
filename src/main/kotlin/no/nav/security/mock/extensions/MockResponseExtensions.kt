package no.nav.security.mock.extensions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.oauth2.sdk.ErrorObject
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse
import okhttp3.mockwebserver.MockResponse

private val objectMapper: ObjectMapper = jacksonObjectMapper()

fun MockResponse.json(anyObject: Any): MockResponse =
    jsonWithCode(200, anyObject)

fun MockResponse.jsonWithCode(statusCode: Int, anyObject: Any): MockResponse =
    this.setResponseCode(statusCode)
        .setHeader("Content-Type", "application/json;charset=UTF-8")
        .setBody(
            when (anyObject) {
                is String -> anyObject
                else -> objectMapper
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValueAsString(anyObject)
            }
        )

fun MockResponse.oauth2Error(error: ErrorObject): MockResponse {
    val responseCode = error.httpStatusCode.takeUnless { it == 302 } ?: 400
    return this.setResponseCode(responseCode)
        .setHeader("Content-Type", "application/json;charset=UTF-8")
        .setBody(
            objectMapper
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(error.toJSONObject())
                .toLowerCase()
        )
}

fun MockResponse.authenticationSuccess(
    authenticationSuccessResponse: AuthenticationSuccessResponse
): MockResponse {
    val httpResponse = authenticationSuccessResponse.toHTTPResponse()
    return this.setResponseCode(httpResponse.statusCode).setHeader("Location", httpResponse.location)
}