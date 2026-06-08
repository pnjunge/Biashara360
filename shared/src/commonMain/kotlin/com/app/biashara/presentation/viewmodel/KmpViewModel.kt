package com.app.biashara.presentation.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Base class for all shared Kotlin Multiplatform ViewModels.
 * Hosts a lifecycle-aware coroutine scope that can be cancelled when the UI leaves the backstack.
 */
open class KmpViewModel {
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Cancels the active coroutine scope, cancelling any active jobs or flows.
     */
    fun clear() {
        scope.cancel()
    }
}
