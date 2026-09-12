package wiospor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import java.util.Calendar

@CloudstreamPlugin
class WioSporPlugin : Plugin() {
    private var mainApi: WioSpor? = null

    override fun load(context: Context) {
        val aggregator = SourceAggregator(context)
        val api = WioSpor(aggregator)
        mainApi = api
        registerMainAPI(api)

        showOnboardingIfDue(context, aggregator)
        showSupportNoticeIfDue(context)

        openSettings = { uiContext ->
            WioSettings.show(uiContext, aggregator)
        }
    }

    private fun showOnboardingIfDue(context: Context, aggregator: SourceAggregator) {
        if (aggregator.isOnboardingCompleted()) return
        val activity = context as? Activity ?: return
        Handler(Looper.getMainLooper()).postDelayed({
            if (activity.isFinishing || activity.isDestroyed) return@postDelayed
            runCatching {
                WioOnboarding.show(activity, aggregator)
            }
        }, 500L)
    }

    private fun showSupportNoticeIfDue(context: Context) {
        val activity = context as? Activity ?: return
        val prefs = context.getSharedPreferences("wiospor_support_notice", Context.MODE_PRIVATE)
        val today = Calendar.getInstance().run { "${get(Calendar.YEAR)}-${get(Calendar.DAY_OF_YEAR)}" }
        if (prefs.getString("last_day", "") == today) return
        prefs.edit().putString("last_day", today).apply()
        Handler(Looper.getMainLooper()).postDelayed({
            if (activity.isFinishing || activity.isDestroyed) return@postDelayed
            runCatching {
                MaterialAlertDialogBuilder(activity)
                    .setTitle("WioSpor")
                    .setMessage("Eklenti ücretsiz kalacak.\n\nTelegram kanalına katıl: güncel linkler, eklenti haberleri ve istekler için.\n\nBu hatırlatma günde bir kez gösterilir.")
                    .setPositiveButton("Telegram ↗") { _, _ ->
                        openUrl(activity, "https://t.me/wiolandcs3")
                    }
                    .setNeutralButton("Destek ol ☕") { _, _ ->
                        openUrl(activity, "https://buymeacoffee.com/wiojelt")
                    }
                    .setNegativeButton("Kapat", null)
                    .show()
            }
        }, 1200L)
    }

    private fun openUrl(context: Context, url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
