package com.vadimtoptunov.chaosbank_android.support

import com.vadimtoptunov.chaosbank_android.app.AppServices
import com.vadimtoptunov.chaosbank_android.core.defects.ChaosConfig
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

/** Configure the global [Defects] surface for a test. */
fun configureDefects(vararg defects: DefectId, seed: Int = 0) {
    Defects.configure(ChaosConfig(seed, defects.toSet(), "test"))
}

/** Build an [AppServices] and align the global [Defects] surface with it. */
fun servicesWith(vararg defects: DefectId, seed: Int = 0): AppServices {
    val config = ChaosConfig(seed, defects.toSet(), "test")
    Defects.configure(config)
    return AppServices(config)
}

/** Base class for tests that touch coroutines or [AppServices] (which uses Dispatchers.Main). */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class CoroutineTest {
    protected val dispatcher = StandardTestDispatcher()

    @Before
    fun installMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
        Defects.configure(ChaosConfig(0, emptySet(), "clean"))
    }
}
