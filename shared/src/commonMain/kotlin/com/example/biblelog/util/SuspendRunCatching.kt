package com.example.biblelog.util

import kotlinx.coroutines.CancellationException

suspend inline fun <T> suspendRunCatching(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
