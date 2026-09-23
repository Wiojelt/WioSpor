package wiospor

import android.content.Context
import android.widget.Toast
import turkspor.common.WioCoreSettingsDialog
import turkspor.common.WioProviderSettingItem
import turkspor.common.WioSettingsConfig

object WioSettings {

    fun show(context: Context, aggregator: SourceAggregator) {
        aggregator.migrateLegacySourceProfile()
        val workers = aggregator.workers
        val items = workers.map { worker ->
            WioProviderSettingItem(
                id = worker.id,
                displayName = worker.displayName,
                isDirectTmdb = false,
                domain = null,
                isEnabled = aggregator.isSourceEnabled(worker.id),
                onToggle = { isChecked ->
                    aggregator.setSourceEnabled(worker.id, isChecked)
                }
            )
        }

        val config = WioSettingsConfig(
            brandPrefix = "WIO",
            brandSuffix = "SPOR",
            brandTitleTag = "Kontrol Paneli",
            summarySubtitle = "Tüm canlı TV ve spor yayın sağlayıcıları tek merkezde",
            isTvBoxSupported = true,
            isTvBoxActive = { aggregator.isTvBoxMode() },
            onTvBoxToggled = { enabled -> aggregator.setTvBoxMode(enabled) },
            items = items,
            onSelectAll = {
                workers.forEach { aggregator.setSourceEnabled(it.id, true) }
                items.forEach { it.isEnabled = true }
            },
            onSelectRecommended = {
                workers.forEach { worker ->
                    val enabled = worker.id in SourceAggregator.DEFAULT_ENABLED_SOURCES
                    aggregator.setSourceEnabled(worker.id, enabled)
                }
                items.forEach { it.isEnabled = (it.id in SourceAggregator.DEFAULT_ENABLED_SOURCES) }
            },
            onSelectNone = {
                workers.forEach { aggregator.setSourceEnabled(it.id, false) }
                items.forEach { it.isEnabled = false }
            },
            onWizardClick = {
                // WioSpor ayarları tek panelden yönetilir; eski kurulum
                // sihirbazı ikinci bir profil oluşturup seçimleri geri alıyordu.
            },
            onCustomListClick = {
                WioCustomListDialog.show(context, aggregator.customListManager)
            },
            customListSummary = aggregator.customListManager.getSummary(),
            onAutoScanDomains = { statusCallback ->
                statusCallback("Web adresleri kontrol ediliyor...")
                aggregator.refreshWebDomains()
                Pair(workers.size, workers.size)
            },
            onSaveAndClose = {
                aggregator.clearRuntimeCaches()
                val activeCount = workers.count { aggregator.isSourceEnabled(it.id) }
                Toast.makeText(context, "$activeCount kaynak kaydedildi", Toast.LENGTH_SHORT).show()
            },
            telegramUrl = "https://t.me/wiolandcs3",
            coffeeUrl = "https://kreosus.com/wiojelt"
        )

        WioCoreSettingsDialog.show(context, config)
    }
}
