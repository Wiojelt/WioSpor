package wiospor

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
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

object WioSettings {
    fun show(context: Context, aggregator: SourceAggregator) {
        fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

        // WioSpor Brand Palette (Red / Crimson Translucent Glass)
        val brandRed = Color.parseColor("#FF2040")
        val brandRedTranslucent = Color.parseColor("#44FF2040")
        val brandRedBorder = Color.parseColor("#66FF2A48")
        val sheetBg = Color.parseColor("#F5100609")
        val cardBg = Color.parseColor("#C81E0E14")
        val cardBgSubtle = Color.parseColor("#99170A0F")
        val textPrimary = Color.parseColor("#FFFFFF")
        val textSecondary = Color.parseColor("#D4A5AF")
        val textMuted = Color.parseColor("#9E7A82")
        val successGreen = Color.parseColor("#00E676")
        val warningOrange = Color.parseColor("#FFA000")

        fun glassDrawable(
            bgColor: Int = cardBg,
            borderColor: Int = brandRedTranslucent,
            radiusDp: Float = 14f,
            borderWidthDp: Int = 1
        ): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(radiusDp.toInt()).toFloat()
                setColor(bgColor)
                if (borderWidthDp > 0) {
                    setStroke(dp(borderWidthDp), borderColor)
                }
            }
        }

        val switchThumbStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                brandRed,
                Color.parseColor("#7A6369")
            )
        )
        val switchTrackStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                Color.parseColor("#77FF2040"),
                Color.parseColor("#331820")
            )
        )

        fun styleSwitch(switch: SwitchMaterial) {
            switch.thumbTintList = switchThumbStates
            switch.trackTintList = switchTrackStates
        }

        fun styleButton(
            button: MaterialButton,
            bgColor: Int = Color.parseColor("#260F16"),
            borderColor: Int = brandRedBorder,
            radiusDp: Int = 12
        ) {
            button.apply {
                backgroundTintList = ColorStateList.valueOf(bgColor)
                strokeColor = ColorStateList.valueOf(borderColor)
                strokeWidth = dp(1)
                cornerRadius = dp(radiusDp)
                setTextColor(textPrimary)
                rippleColor = ColorStateList.valueOf(brandRedTranslucent)
                isAllCaps = false
                elevation = 0f
            }
        }

        val dialog = BottomSheetDialog(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    dp(24).toFloat(), dp(24).toFloat(),
                    dp(24).toFloat(), dp(24).toFloat(),
                    0f, 0f, 0f, 0f
                )
                setColor(sheetBg)
                setStroke(dp(1), Color.parseColor("#33FF2040"))
            }
        }

        // --- 1. HEADER (Title, Summary Badge & Close Button) ---
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val brandTitle = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleWio = TextView(context).apply {
            text = "WIO"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(brandRed)
        }
        val titleSpor = TextView(context).apply {
            text = "SPOR"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
        }
        val titleSettings = TextView(context).apply {
            text = " Ayarları"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textSecondary)
        }
        brandTitle.addView(titleWio)
        brandTitle.addView(titleSpor)
        brandTitle.addView(titleSettings)
        titleCol.addView(brandTitle)

        val summaryText = TextView(context).apply {
            textSize = 12.5f
            setTextColor(textMuted)
            setPadding(0, dp(1), 0, 0)
        }
        titleCol.addView(summaryText)

        val closeBtn = MaterialButton(context).apply {
            text = "✕ Kapat"
            textSize = 13f
            styleButton(this, bgColor = Color.parseColor("#2A1017"), borderColor = brandRedTranslucent, radiusDp = 18)
            setOnClickListener { dialog.dismiss() }
        }

        header.addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(closeBtn, LinearLayout.LayoutParams(-2, dp(38)))
        root.addView(header, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })

        // --- 2. TV BOX / DÜŞÜK BELLEK MODU KARTI ---
        val autoDetected = aggregator.isAutoDetectedTvOrLowRam()
        val tvBoxCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = glassDrawable(cardBg, brandRedBorder, 16f, 1)
        }

        val tvBoxHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvBoxTitleRow = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val tvBoxTitle = TextView(context).apply {
            text = "📺 TV Box / Düşük Bellek Modu"
            textSize = 14.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
        }

        val tvBoxStatus = TextView(context).apply {
            textSize = 11.5f
            setTextColor(if (autoDetected) successGreen else textSecondary)
            text = if (autoDetected) "✓ Cihaz TV / Kısıtlı RAM olarak tespit edildi (Önerilen)"
                   else "ℹ Standart mobil/tablet modu (Yüksek RAM)"
            setPadding(0, dp(1), 0, 0)
        }
        tvBoxTitleRow.addView(tvBoxTitle)
        tvBoxTitleRow.addView(tvBoxStatus)

        val tvBoxSwitch = SwitchMaterial(context).apply {
            isChecked = aggregator.isTvBoxMode()
            styleSwitch(this)
            setOnCheckedChangeListener { _, isChecked ->
                aggregator.setTvBoxMode(isChecked)
            }
        }

        tvBoxHeader.addView(tvBoxTitleRow, LinearLayout.LayoutParams(0, -2, 1f))
        tvBoxHeader.addView(tvBoxSwitch)
        tvBoxCard.addView(tvBoxHeader)

        val tvBoxDesc = TextView(context).apply {
            textSize = 11f
            setTextColor(textMuted)
            text = "Aynı anda çalışan bağlantıyı 2 ile sınırlar, ilk kaliteli linkler (6 adet) geldiğinde taramayı sonlandırarak TV Box çökmesini önler."
            setPadding(0, dp(4), 0, 0)
        }
        tvBoxCard.addView(tvBoxDesc)

        root.addView(tvBoxCard, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(8)
        })

        // --- 3. HIZLI İŞLEM BUTONLARI & BİLGİ ALANI ---
        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val healthText = TextView(context).apply {
            textSize = 11.5f
            setTextColor(textMuted)
            text = "Kaynak durumunu kontrol etmek veya adresleri yenilemek için butonları kullanın."
            setPadding(dp(2), dp(2), dp(2), dp(6))
        }

        val healthButton = MaterialButton(context).apply {
            text = "🔍 Kaynak Kontrolü"
            textSize = 12.5f
            styleButton(this, bgColor = Color.parseColor("#33121B"), borderColor = brandRedBorder, radiusDp = 12)
            setOnClickListener {
                isEnabled = false
                healthText.setTextColor(warningOrange)
                healthText.text = "⏳ Kaynaklar taranıyor, lütfen bekleyin..."
                CoroutineScope(Dispatchers.Main).launch {
                    val health = aggregator.checkHealth()
                    healthText.setTextColor(if (health.onlineCount > 0) successGreen else Color.parseColor("#FF5252"))
                    healthText.text = "✓ Güncel Durum: ${health.onlineCount} / ${health.totalCount} kaynak aktif ve yanıt veriyor."
                    isEnabled = true
                }
            }
        }

        val domainRefreshButton = MaterialButton(context).apply {
            text = "🌐 Domainleri Yenile"
            textSize = 12.5f
            styleButton(this, bgColor = Color.parseColor("#33121B"), borderColor = brandRedBorder, radiusDp = 12)
            setOnClickListener {
                isEnabled = false
                healthText.setTextColor(warningOrange)
                healthText.text = "⏳ Web kaynaklarının güncel adresleri taranıyor..."
                CoroutineScope(Dispatchers.Main).launch {
                    val msg = aggregator.refreshWebDomains()
                    healthText.setTextColor(successGreen)
                    healthText.text = "✓ $msg"
                    isEnabled = true
                }
            }
        }

        actionRow.addView(healthButton, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(6) })
        actionRow.addView(domainRefreshButton, LinearLayout.LayoutParams(0, dp(40), 1f))
        root.addView(actionRow, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(4)
        })
        root.addView(healthText, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(6)
        })

        // --- 4. KAYNAK YÖNETİMİ BAŞLIĞI & TOPLU AÇ/KAPAT ---
        val sourcesHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(6))
        }

        val sourcesTitleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val sourcesTitle = TextView(context).apply {
            text = "📡 Yayın Kaynakları"
            textSize = 14.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
        }
        val sourcesSub = TextView(context).apply {
            text = "Devre dışı bırakılan kaynaklar taranmaz."
            textSize = 11f
            setTextColor(textMuted)
        }
        sourcesTitleCol.addView(sourcesTitle)
        sourcesTitleCol.addView(sourcesSub)

        sourcesHeader.addView(sourcesTitleCol, LinearLayout.LayoutParams(0, -2, 1f))

        val switches = linkedMapOf<String, SwitchMaterial>()

        fun refreshSummary() {
            val enabledCount = switches.values.count { it.isChecked }
            summaryText.text = "$enabledCount / ${aggregator.workers.size} kaynak etkin"
        }

        val openAllBtn = MaterialButton(context).apply {
            text = "Tümünü Aç"
            textSize = 11.5f
            styleButton(this, bgColor = Color.parseColor("#261118"), borderColor = brandRedTranslucent, radiusDp = 10)
            setOnClickListener {
                switches.forEach { (id, toggle) ->
                    toggle.isChecked = true
                    aggregator.setSourceEnabled(id, true)
                }
                refreshSummary()
            }
        }

        val closeAllBtn = MaterialButton(context).apply {
            text = "Tümünü Kapat"
            textSize = 11.5f
            styleButton(this, bgColor = Color.parseColor("#261118"), borderColor = brandRedTranslucent, radiusDp = 10)
            setOnClickListener {
                switches.forEach { (id, toggle) ->
                    toggle.isChecked = false
                    aggregator.setSourceEnabled(id, false)
                }
                refreshSummary()
            }
        }

        sourcesHeader.addView(openAllBtn, LinearLayout.LayoutParams(-2, dp(32)).apply { marginEnd = dp(4) })
        sourcesHeader.addView(closeAllBtn, LinearLayout.LayoutParams(-2, dp(32)))

        root.addView(sourcesHeader, LinearLayout.LayoutParams(-1, -2))

        // --- 5. KAYNAK LİSTESİ (ÇOK SÜTUNLU & CAM KARTLAR) ---
        val isWide = context.resources.displayMetrics.widthPixels > dp(580)

        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = true
        }

        val columnsContainer = LinearLayout(context).apply {
            orientation = if (isWide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        }
        scroll.addView(columnsContainer)

        val col1 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            if (isWide) {
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) }
            }
        }
        val col2 = if (isWide) {
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
        } else null

        columnsContainer.addView(col1)
        col2?.let { columnsContainer.addView(it) }

        aggregator.workers.forEachIndexed { index, worker ->
            val targetCol = if (isWide && index % 2 != 0) col2 ?: col1 else col1

            val itemCard = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = glassDrawable(cardBgSubtle, Color.parseColor("#22FF2040"), 12f, 1)
                setPadding(dp(12), dp(2), dp(8), dp(2))
                isClickable = true
                isFocusable = true
            }

            val label = TextView(context).apply {
                text = worker.displayName
                textSize = 13.5f
                setTextColor(textPrimary)
                typeface = Typeface.DEFAULT_BOLD
            }

            val toggle = SwitchMaterial(context).apply {
                contentDescription = "${worker.displayName} aktif"
                isChecked = aggregator.isSourceEnabled(worker.id)
                styleSwitch(this)
                setOnCheckedChangeListener { _, checked ->
                    aggregator.setSourceEnabled(worker.id, checked)
                    refreshSummary()
                }
            }

            itemCard.setOnClickListener {
                toggle.isChecked = !toggle.isChecked
            }

            itemCard.addView(label, LinearLayout.LayoutParams(0, -2, 1f))
            itemCard.addView(toggle)
            switches[worker.id] = toggle

            targetCol.addView(itemCard, LinearLayout.LayoutParams(-1, dp(44)).apply {
                bottomMargin = dp(5)
            })
        }

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        refreshSummary()
        dialog.setContentView(root)

        val displayHeight = context.resources.displayMetrics.heightPixels
        root.layoutParams.height = (displayHeight * 0.92).toInt()

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = null // Let root draw the rounded glass background
            dialog.behavior.skipCollapsed = true
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        dialog.show()
    }
}
