package wiospor

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import kotlin.math.min

object WioOnboarding {

    fun show(
        context: Context,
        aggregator: SourceAggregator,
        onDismiss: (() -> Unit)? = null
    ) {
        fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

        val brandRed = Color.parseColor("#FF2040")
        val brandRedBorder = Color.parseColor("#66FF2A48")
        val dialogBg = Color.parseColor("#F2100609")
        val cardBg = Color.parseColor("#CC1E0E14")
        val textPrimary = Color.parseColor("#FFFFFF")
        val textSecondary = Color.parseColor("#D4A5AF")
        val textMuted = Color.parseColor("#9E7A82")
        val successGreen = Color.parseColor("#00E676")

        fun focusableCardDrawable(
            normalBg: Int = cardBg,
            normalBorder: Int = brandRedBorder,
            focusedBg: Int = Color.parseColor("#E6351520"),
            focusedBorder: Int = Color.parseColor("#FFFF2E4C"),
            radiusDp: Float = 14f,
            normalBorderDp: Int = 1,
            focusedBorderDp: Int = 2
        ): StateListDrawable {
            val normal = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(radiusDp.toInt()).toFloat()
                setColor(normalBg)
                if (normalBorderDp > 0) setStroke(dp(normalBorderDp), normalBorder)
            }
            val focused = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(radiusDp.toInt()).toFloat()
                setColor(focusedBg)
                setStroke(dp(focusedBorderDp), focusedBorder)
            }
            return StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focused)
                addState(intArrayOf(android.R.attr.state_pressed), focused)
                addState(intArrayOf(), normal)
            }
        }

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = true
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
                setColor(dialogBg)
                setStroke(dp(1), Color.parseColor("#44FF2040"))
            }
        }
        scroll.addView(root)

        // --- 1. HEADER ---
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(14))
        }

        val brandTitle = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val titleWio = TextView(context).apply {
            text = "WIO"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(brandRed)
        }
        val titleSpor = TextView(context).apply {
            text = "SPOR"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
        }
        val titleWizard = TextView(context).apply {
            text = " • Kurulum Sihirbazı"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textSecondary)
        }
        brandTitle.addView(titleWio)
        brandTitle.addView(titleSpor)
        brandTitle.addView(titleWizard)
        header.addView(brandTitle)

        val autoDetected = aggregator.isAutoDetectedTvOrLowRam()
        val subTitle = TextView(context).apply {
            textSize = 12.5f
            setTextColor(textSecondary)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
            text = if (autoDetected) {
                "Cihazınız TV / Kısıtlı Bellek olarak algılandı.\nEn akıcı deneyim için TV Box modunu kullanmanız önerilir."
            } else {
                "Cihazınıza en uygun çalışma modunu seçin.\nBu ayarı dilediğiniz zaman eklenti ayarlarından değiştirebilirsiniz."
            }
        }
        header.addView(subTitle)
        root.addView(header)

        // Helper to create selectable option cards
        fun createOptionCard(
            titleText: String,
            descText: String,
            badgeText: String? = null,
            badgeColor: Int = successGreen,
            onClick: () -> Unit
        ): LinearLayout {
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = focusableCardDrawable()
                isClickable = true
                isFocusable = true
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                setOnClickListener { onClick() }
            }

            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val title = TextView(context).apply {
                text = titleText
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textPrimary)
            }
            topRow.addView(title, LinearLayout.LayoutParams(0, -2, 1f))

            if (badgeText != null) {
                val badge = TextView(context).apply {
                    text = badgeText
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.BLACK)
                    setPadding(dp(6), dp(2), dp(6), dp(2))
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(6).toFloat()
                        setColor(badgeColor)
                    }
                }
                topRow.addView(badge)
            }

            card.addView(topRow)

            val desc = TextView(context).apply {
                text = descText
                textSize = 11.5f
                setTextColor(textMuted)
                setPadding(0, dp(4), 0, 0)
            }
            card.addView(desc)

            return card
        }

        // --- OPTION 1: TV Box Modu (Önerilen) ---
        val cardTv = createOptionCard(
            titleText = "📺 TV Box Modu",
            descText = "Sadece en hızlı 5 kaynak aktifleştirilir (BeyazElma, Domino, İnat TV, KralSpor, Betmatik). Aşırı bellek kullanımı ve çökme engellenir.",
            badgeText = "ÖNERİLEN",
            badgeColor = successGreen
        ) {
            aggregator.setTvBoxMode(true)
            aggregator.enableOnlyRecommendedSources()
            aggregator.setOnboardingCompleted(true)
            dialog.dismiss()
            Toast.makeText(context, "📺 TV Box Modu aktif: En hızlı 5 kaynak devrede.", Toast.LENGTH_SHORT).show()
            onDismiss?.invoke()
        }
        root.addView(cardTv, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })

        // --- OPTION 2: Kendin Seç (Özelleştir) ---
        val cardCustom = createOptionCard(
            titleText = "⚙ Kendin Seç (Özelleştir)",
            descText = "Hangi sağlayıcıların taranacağını açılır menüden tek tek kendiniz belirleyin.",
            badgeText = "ÖZEL",
            badgeColor = Color.parseColor("#FF9100")
        ) {
            aggregator.setOnboardingCompleted(true)
            dialog.dismiss()
            WioSettings.show(context, aggregator)
            onDismiss?.invoke()
        }
        root.addView(cardCustom, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })

        // --- OPTION 3: Standart Mod ---
        val cardStandard = createOptionCard(
            titleText = "📱 Standart Mod (Tüm Kaynaklar)",
            descText = "Güçlü telefon, tablet veya bilgisayar için tüm kaynaklar (40+ sağlayıcı) eksiksiz taranır.",
            badgeText = null
        ) {
            aggregator.setTvBoxMode(false)
            aggregator.enableAllSources()
            aggregator.setOnboardingCompleted(true)
            dialog.dismiss()
            Toast.makeText(context, "📱 Standart Mod aktif: Tüm kaynaklar taranacak.", Toast.LENGTH_SHORT).show()
            onDismiss?.invoke()
        }
        root.addView(cardStandard, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(14)
        })

        // --- CLOSE / SKIP BUTTON ---
        val skipBtn = MaterialButton(context).apply {
            text = "Daha Sonra Ayarla"
            textSize = 12.5f
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#260F16"))
            strokeColor = ColorStateList.valueOf(brandRedBorder)
            strokeWidth = dp(1)
            cornerRadius = dp(12)
            setTextColor(textSecondary)
            isAllCaps = false
            elevation = 0f
            isFocusable = true
            setOnClickListener {
                aggregator.setOnboardingCompleted(true)
                dialog.dismiss()
                onDismiss?.invoke()
            }
        }
        root.addView(skipBtn, LinearLayout.LayoutParams(-1, dp(40)))

        dialog.setContentView(scroll)

        val metrics = context.resources.displayMetrics
        val maxWidth = dp(480)
        val dialogWidth = min((metrics.widthPixels * 0.92).toInt(), maxWidth)
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        dialog.setOnShowListener {
            cardTv.requestFocus()
        }

        dialog.show()
    }
}
