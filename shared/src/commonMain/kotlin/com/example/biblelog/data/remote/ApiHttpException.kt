package com.example.biblelog.data.remote

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiHttpException(
    message: String,
    val statusCode: Int,
) : Exception(message)

private val errorJson = Json { ignoreUnknownKeys = true }

suspend inline fun <reified T> HttpResponse.decodeBody(): T {
    if (!status.isSuccess()) {
        throw apiHttpException(this)
    }
    return body()
}

suspend fun apiHttpException(response: HttpResponse): ApiHttpException {
    val bodyText = runCatching { response.bodyAsText() }.getOrDefault("")
    val detail = parseApiErrorDetail(bodyText)
    val message = when (response.status.value) {
        401 -> detail?.let(::mapUnauthorizedDetail)
            ?: "로그인이 만료되었습니다. 다시 로그인해 주세요."
        403 -> detail ?: "접근 권한이 없습니다."
        404 -> detail ?: "요청한 정보를 찾을 수 없습니다."
        else -> detail ?: "요청에 실패했습니다. (${response.status.value})"
    }
    return ApiHttpException(message, response.status.value)
}

private fun mapUnauthorizedDetail(detail: String): String = when {
    detail.contains("token", ignoreCase = true) ||
        detail.contains("Authentication", ignoreCase = true) ->
        "로그인이 만료되었습니다. 다시 로그인해 주세요."
    else -> detail
}

private fun parseApiErrorDetail(body: String): String? {
    if (body.isBlank()) return null
    return runCatching {
        val element = errorJson.parseToJsonElement(body).jsonObject["detail"] ?: return null
        when (element) {
            is JsonPrimitive -> element.content
            is JsonArray -> element.firstOrNull()?.jsonPrimitive?.content
            else -> null
        }
    }.getOrNull()
}
