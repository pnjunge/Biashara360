package com.app.biashara.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class NativeSyncState(
    val generation: Long = 0,
    val active: Int = 0,
    val lastSuccess: Instant? = null,
    val failures: Set<String> = emptySet()
)

/** Tracks completed API-to-cache syncs, rather than guessing from network availability. */
object NativeSyncStatus {
    private val mutableState = MutableStateFlow(NativeSyncState())
    val state = mutableState.asStateFlow()

    fun reset() {
        mutableState.update { NativeSyncState(generation = it.generation + 1) }
    }

    suspend fun <T> track(resource: String, block: suspend () -> T): Result<T> {
        val generation = mutableState.value.generation
        mutableState.update { if (it.generation == generation) it.copy(active = it.active + 1) else it }
        var completed: Result<T>? = null
        try {
            completed = try {
                Result.success(block())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
            return completed
        } finally {
            mutableState.update { current ->
                if (current.generation != generation) current
                else current.copy(
                    active = (current.active - 1).coerceAtLeast(0),
                    lastSuccess = if (completed?.isSuccess == true) Clock.System.now() else current.lastSuccess,
                    failures = when {
                        completed?.isSuccess == true -> current.failures - resource
                        completed?.isFailure == true -> current.failures + resource
                        else -> current.failures
                    }
                )
            }
        }
    }
}
