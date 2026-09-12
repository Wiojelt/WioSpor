package wiospor

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object WioSettings {
    fun show(context: Context, aggregator: SourceAggregator) {
        fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

        val dialog = BottomSheetDialog(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(20))
        }

        val title = TextView(context).apply {
            text = "WioSpor Ayarları"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(title)

        val summary = TextView(context).apply {
            textSize = 14f
            setPadding(0, dp(6), 0, dp(4))
        }
        root.addView(summary)

        val autoDetected = aggregator.isAutoDetectedTvOrLowRam()

        val tvBoxCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(Color.parseColor("#1E222D"))
        }

        val tvBoxHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvBoxTitle = TextView(context).apply {
            text = "📺 TV Box / Düşük Bellek Modu"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        val tvBoxSwitch = SwitchMaterial(context).apply {
            isChecked = aggregator.isTvBoxMode()
            setOnCheckedChangeListener { _, isChecked ->
                aggregator.setTvBoxMode(isChecked)
            }
        }

        tvBoxHeader.addView(tvBoxTitle, LinearLayout.LayoutParams(0, -2, 1f))
        tvBoxHeader.addView(tvBoxSwitch)
        tvBoxCard.addView(tvBoxHeader)

        val tvBoxStatus = TextView(context).apply {
            textSize = 12f
            setTextColor(if (autoDetected) Color.parseColor("#4CAF50") else Color.parseColor("#9E9E9E"))
            text = if (autoDetected) "✓ Cihazınız TV / Kısıtlı RAM olarak tespit edildi (Önerilen)"
                   else "ℹ Standart mobil/tablet modu"
            setPadding(0, dp(2), 0, dp(4))
        }
        tvBoxCard.addView(tvBoxStatus)

        val tvBoxDesc = TextView(context).apply {
            textSize = 11.5f
            setTextColor(Color.parseColor("#B0BEC5"))
            text = "Aynı anda çalışan bağlantıyı 2 ile sınırlar, ilk kaliteli linkler (6 adet) geldiğinde taramayı durdurarak TV Box'ların RAM yetersizliğinden kapanmasını önler."
        }
        tvBoxCard.addView(tvBoxDesc)

        root.addView(tvBoxCard, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(4)
            bottomMargin = dp(10)
        })

        val healthText = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#4CAF50"))
            setPadding(0, 0, 0, dp(6))
            text = "Kaynak kontrolü yapmak için aşağıdaki butona tıklayın."
        }
        root.addView(healthText)

        val healthButton = MaterialButton(context).apply {
            text = "🔍 Kaynak Kontrolü Yap"
            isAllCaps = false
            setOnClickListener {
                isEnabled = false
                healthText.setTextColor(Color.parseColor("#FFA000"))
                healthText.text = "Kaynaklar taranıyor, lütfen bekleyin..."
                CoroutineScope(Dispatchers.Main).launch {
                    val health = aggregator.checkHealth()
                    healthText.setTextColor(
                        if (health.onlineCount > 0) Color.parseColor("#4CAF50")
                        else Color.parseColor("#F44336")
                    )
                    healthText.text = "Güncel Durum: ${health.onlineCount} / ${health.totalCount} kaynak aktif ve yanıt veriyor."
                    isEnabled = true
                }
            }
        }
        root.addView(healthButton, LinearLayout.LayoutParams(-1, dp(48)).apply {
            bottomMargin = dp(6)
        })

        val domainRefreshButton = MaterialButton(context).apply {
            text = "🌐 Web Linklerini & Domainleri Yenile"
            isAllCaps = false
            setOnClickListener {
                isEnabled = false
                healthText.setTextColor(Color.parseColor("#FFA000"))
                healthText.text = "Web kaynaklarının güncel adresleri taranıyor..."
                CoroutineScope(Dispatchers.Main).launch {
                    val msg = aggregator.refreshWebDomains()
                    healthText.setTextColor(Color.parseColor("#4CAF50"))
                    healthText.text = "✓ $msg"
                    isEnabled = true
                }
            }
        }
        root.addView(domainRefreshButton, LinearLayout.LayoutParams(-1, dp(48)).apply {
            bottomMargin = dp(8)
        })

        val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(actions)

        val scroll = ScrollView(context)
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val switches = linkedMapOf<String, SwitchMaterial>()

        fun refreshSummary() {
            summary.text = "${switches.values.count { it.isChecked }} / ${aggregator.workers.size} kaynak etkin"
        }

        listOf("Tümünü Aç" to true, "Tümünü Kapat" to false).forEach { (label, state) ->
            actions.addView(MaterialButton(context).apply {
                text = label
                isAllCaps = false
                setOnClickListener {
                    switches.forEach { (id, toggle) ->
                        toggle.isChecked = state
                        aggregator.setSourceEnabled(id, state)
                    }
                    refreshSummary()
                }
            }, LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                marginEnd = dp(4)
                bottomMargin = dp(6)
            })
        }

        aggregator.workers.forEach { worker ->
            val row = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(52)
            }
            val label = TextView(context).apply {
                text = worker.displayName
                textSize = 15f
                setPadding(0, dp(10), dp(8), dp(10))
            }
            val toggle = SwitchMaterial(context).apply {
                contentDescription = "${worker.displayName} aktif"
                isChecked = aggregator.isSourceEnabled(worker.id)
                setOnCheckedChangeListener { _, checked ->
                    aggregator.setSourceEnabled(worker.id, checked)
                    refreshSummary()
                }
            }
            row.setOnClickListener { toggle.isChecked = !toggle.isChecked }
            row.addView(label, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(toggle)
            switches[worker.id] = toggle
            list.addView(row)
        }

        root.addView(TextView(context).apply {
            text = "Devre dışı bırakılan kaynaklar kanal aramasında ve yayın getirmede taranmaz."
            textSize = 12f
            setPadding(0, dp(8), 0, dp(6))
        })

        root.addView(MaterialButton(context).apply {
            text = "Kapat"
            isAllCaps = false
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(-1, dp(50)))

        refreshSummary()
        dialog.setContentView(root)
        root.layoutParams.height = (context.resources.displayMetrics.heightPixels * .85).toInt()
        dialog.setOnShowListener { dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED }
        dialog.show()
    }
}
