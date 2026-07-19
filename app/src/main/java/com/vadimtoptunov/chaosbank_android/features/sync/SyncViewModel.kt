package com.vadimtoptunov.chaosbank_android.features.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.app.AppServices
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * A tiny concurrency playground: run N parallel "+1" operations against a shared
 * counter. The correct path uses an atomic increment. The `syncLostUpdate` defect
 * does a non-atomic read-modify-write, so concurrent increments clobber each other
 * and the final value is short — the classic lost-update race.
 */
class SyncViewModel(private val services: AppServices) {
    val concurrency = 20

    var counter by mutableIntStateOf(0); private set
    var running by mutableStateOf(false); private set

    private fun active(id: DefectId) = Defects.isActive(id)

    suspend fun load() { counter = services.backend.syncValue() }

    suspend fun reset() {
        services.backend.syncReset()
        counter = 0
    }

    /** Expected final value after a run: start + concurrency. */
    val expected: Int get() = counter + concurrency

    suspend fun runConcurrent() {
        if (running) return
        running = true
        try {
            coroutineScope {
                repeat(concurrency) {
                    launch {
                        if (active(DefectId.syncLostUpdate)) {
                            // Non-atomic read-modify-write: interleaves and loses updates.
                            val v = services.backend.syncValue()
                            services.backend.syncSet(v + 1)
                        } else {
                            services.backend.syncIncrement()
                        }
                    }
                }
            }
            counter = services.backend.syncValue()
        } finally {
            running = false
        }
    }
}
