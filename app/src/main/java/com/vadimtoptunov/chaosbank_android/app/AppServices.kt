package com.vadimtoptunov.chaosbank_android.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.core.backend.BackendScenario
import com.vadimtoptunov.chaosbank_android.core.backend.MockBackend
import com.vadimtoptunov.chaosbank_android.core.backend.NetworkCondition
import com.vadimtoptunov.chaosbank_android.core.defects.BugProfile
import com.vadimtoptunov.chaosbank_android.core.defects.ChaosConfig
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.feed.MarketStore
import com.vadimtoptunov.chaosbank_android.core.feed.PriceSourceKind
import com.vadimtoptunov.chaosbank_android.models.SeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The shared service container. Holds the backend, live market store, and active
 * build config. `dataVersion` ticks on every mutation; `configVersion` ticks when
 * the active profile changes (dev menu) so the UI tree rebuilds.
 */
class AppServices(config: ChaosConfig) {

    var config by mutableStateOf(config)
        private set

    val backend = MockBackend(scenario = BackendScenario.from(config.activeDefects))
    val market = MarketStore(config.seed, SeedData.assets, config.priceSource)
    val notifications = NotificationStore()
    val locale = LocaleSettings()
    val templates = TemplateStore()
    val kyc = KycStore()

    var dataVersion by mutableStateOf(0)
        private set

    var configVersion by mutableStateOf(0)
        private set

    /** Simulated network environment (reliability cluster), chosen from the dev menu. */
    var networkCondition by mutableStateOf(NetworkCondition.normal)
        private set

    /** True while cached data is served and writes fail. */
    val offline: Boolean get() = networkCondition == NetworkCondition.offline

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun applyNetworkCondition(value: NetworkCondition) {
        networkCondition = value
        backend.setCondition(value)
    }

    fun enableOffline(value: Boolean) =
        applyNetworkCondition(if (value) NetworkCondition.offline else NetworkCondition.normal)

    fun bumpData() { dataVersion += 1 }

    fun startFeed() { market.start() }

    fun isActive(id: DefectId): Boolean = config.activeDefects.contains(id)

    private fun syncScenario() {
        val scenario = BackendScenario.from(config.activeDefects)
        scope.launch { backend.setScenario(scenario) }
    }

    fun applyProfile(profile: BugProfile) {
        config = config.copy(activeDefects = profile.defects, label = profile.id)
        Defects.configure(config)
        syncScenario()
        configVersion += 1
    }

    fun applyDefects(ids: Set<DefectId>, label: String) {
        config = config.copy(activeDefects = ids, label = label)
        Defects.configure(config)
        syncScenario()
        configVersion += 1
    }

    fun toggle(id: DefectId) {
        val d = config.activeDefects.toMutableSet()
        if (id in d) d.remove(id) else d.add(id)
        config = config.copy(activeDefects = d, label = "custom")
        Defects.configure(config)
        syncScenario()
        configVersion += 1
    }

    fun setPriceSource(kind: PriceSourceKind) {
        config = config.copy(priceSource = kind)
        market.switchSource(kind)
    }
}
