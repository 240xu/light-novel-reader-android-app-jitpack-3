package io.legado.engine.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Coroutine utilities for async rule processing.
 * Pure Kotlin coroutines, zero Android dependencies.
 */
object CoroutineHelper {

    /**
     * Execute a block with timeout.
     */
    suspend fun <T> withTimeoutOrNull(
        timeoutMs: Long = 30_000L,
        block: suspend () -> T
    ): T? {
        return kotlinx.coroutines.withTimeoutOrNull(timeoutMs) { block() }
    }

    /**
     * Execute a list of tasks concurrently with limited parallelism.
     */
    suspend fun <T> parallelMap(
        items: List<T>,
        parallelism: Int = 3,
        block: suspend (T) -> T
    ): List<T> = coroutineScope {
        items.map { item ->
            async(Dispatchers.Default) { block(item) }
        }.awaitAll()
    }

    /**
     * Create a cold flow that emits results sequentially.
     */
    fun <T> sequentialFlow(
        items: List<suspend () -> T>
    ): Flow<T> = flow {
        items.forEach { emit(it()) }
    }

    /**
     * Retry with exponential backoff.
     */
    suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelayMs: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
        return block() // last attempt, let exception propagate
    }
}
