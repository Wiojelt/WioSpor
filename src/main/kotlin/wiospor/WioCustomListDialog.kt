package wiospor

import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object WioCustomListDialog {

    fun show(context: Context, manager: WioCustomListManager, onDismiss: (() -> Unit)? = null) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val dialog = BottomSheetDialog(context)
        fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()

        val brandColor = Color.parseColor("#FFB703")
        val brandBorder = Color.parseColor("#66FFB703")
        val cardBg = Color.parseColor("#C8161B22")
        val inputBg = Color.parseColor("#0D1117")

        fun cardBackground(strokeColor: Int = brandBorder) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(14).toFloat()
            setColor(cardBg)
            setStroke(dp(1), strokeColor)
        }

        fun inputBackground() = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(inputBg)
            setStroke(dp(1), Color.parseColor("#30363D"))
        }

        val scroll = NestedScrollView(context).apply {
            setBackgroundColor(Color.parseColor("#F50D1117"))
            isFillViewport = true
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(28))
        }
        scroll.addView(root)

        // Drag handle
        val handle = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
            val bar = TextView(context).apply {
                val lp = LinearLayout.LayoutParams(dp(44), dp(4))
                layoutParams = lp
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(2).toFloat()
                    setColor(Color.parseColor("#30363D"))
                }
            }
            addView(bar)
        }
        root.addView(handle)

        // Header Row
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(14))
        }
        val headerTitle = TextView(context).apply {
            text = "📋 Özel M3U / IPTV Listelerim"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = MaterialButton(context).apply {
            text = "✕"
            textSize = 14f
            setTextColor(Color.parseColor("#8B949E"))
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            strokeWidth = 0
            val lp = LinearLayout.LayoutParams(dp(36), dp(36))
            layoutParams = lp
            isAllCaps = false
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(headerTitle)
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        // Add New Playlist Form Card
        val addCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = cardBackground(brandBorder)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(16)
            }
            layoutParams = lp
        }

        val addTitle = TextView(context).apply {
            text = "➕ Yeni M3U / IPTV Listesi Ekle"
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(brandColor)
            setPadding(0, 0, 0, dp(8))
        }
        addCard.addView(addTitle)

        val nameInput = EditText(context).apply {
            hint = "Liste Adı (Örn: Spor Listem, IPTV)"
            setHintTextColor(Color.parseColor("#6E7681"))
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = inputBackground()
            isSingleLine = true
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(8)
            }
            layoutParams = lp
        }
        addCard.addView(nameInput)

        val urlInput = EditText(context).apply {
            hint = "M3U Linki (https://...) veya dosya yolu"
            setHintTextColor(Color.parseColor("#6E7681"))
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = inputBackground()
            isSingleLine = true
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            layoutParams = lp
        }
        addCard.addView(urlInput)

        val inputBtnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            layoutParams = lp
        }

        val btnPaste = MaterialButton(context).apply {
            text = "📋 Yapıştır"
            textSize = 11.5f
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#21262D"))
            strokeColor = ColorStateList.valueOf(Color.parseColor("#30363D"))
            strokeWidth = dp(1)
            cornerRadius = dp(10)
            val lp = LinearLayout.LayoutParams(0, dp(40), 0.8f).apply { marginEnd = dp(8) }
            layoutParams = lp
            isAllCaps = false
            setOnClickListener {
                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val text = clip?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                if (!text.isNullOrBlank()) {
                    urlInput.setText(text)
                    Toast.makeText(context, "Panodan yapıştırıldı", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Pano boş!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val listContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        var refreshPlaylistsList: () -> Unit = {}

        val btnAdd = MaterialButton(context).apply {
            text = "✓ Listeyi Ekle & Test Et"
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            backgroundTintList = ColorStateList.valueOf(brandColor)
            cornerRadius = dp(10)
            val lp = LinearLayout.LayoutParams(0, dp(40), 1.2f)
            layoutParams = lp
            isAllCaps = false
            setOnClickListener {
                val url = urlInput.text.toString().trim()
                if (url.isBlank()) {
                    Toast.makeText(context, "Lütfen geçerli bir URL veya dosya yolu girin!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val name = nameInput.text.toString().trim().ifBlank { "Özel Liste" }

                Toast.makeText(context, "Liste taranıyor...", Toast.LENGTH_SHORT).show()

                scope.launch {
                    val content = withContext(Dispatchers.IO) {
                        manager.fetchPlaylistContent(url)
                    }

                    if (content.isNullOrBlank()) {
                        Toast.makeText(context, "Hata: Liste indirilemedi veya dosya bulunamadı!", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val parsed = withContext(Dispatchers.IO) {
                        TvPlaylistParser.parseM3U(content)
                    }

                    if (parsed.items.isEmpty()) {
                        Toast.makeText(context, "Hata: Listede geçerli kanal bulunamadı!", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    manager.addPlaylist(name, url)
                    nameInput.text?.clear()
                    urlInput.text?.clear()

                    Toast.makeText(context, "✓ '$name' eklendi (${parsed.items.size} kanal)", Toast.LENGTH_LONG).show()
                    refreshPlaylistsList()
                }
            }
        }

        inputBtnRow.addView(btnPaste)
        inputBtnRow.addView(btnAdd)
        addCard.addView(inputBtnRow)
        root.addView(addCard)

        // Section Title: Saved Playlists
        val sectionTitle = TextView(context).apply {
            text = "Kayıtlı Listeler"
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(2), dp(4), dp(2), dp(8))
        }
        root.addView(sectionTitle)
        root.addView(listContainer)

        fun renderPlaylists() {
            listContainer.removeAllViews()
            val playlists = manager.getSavedLinks()
            if (playlists.isEmpty()) {
                val emptyTv = TextView(context).apply {
                    text = "Henüz eklenmiş bir liste yok.\nYukarıdaki formdan M3U URL'si ekleyebilirsiniz."
                    textSize = 12f
                    setTextColor(Color.parseColor("#8B949E"))
                    gravity = Gravity.CENTER
                    setPadding(dp(16), dp(18), dp(16), dp(18))
                }
                listContainer.addView(emptyTv)
                return
            }

            for (pl in playlists) {
                val itemCard = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    background = cardBackground(Color.parseColor("#30363D"))
                    val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = dp(8)
                    }
                    layoutParams = lp
                }

                val itemInfo = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val itemTitle = TextView(context).apply {
                    text = pl.name
                    textSize = 13.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                }
                val itemSub = TextView(context).apply {
                    text = if (pl.link.length > 45) pl.link.take(42) + "..." else pl.link
                    textSize = 11f
                    setTextColor(Color.parseColor("#8B949E"))
                    setPadding(0, dp(2), 0, 0)
                }
                itemInfo.addView(itemTitle)
                itemInfo.addView(itemSub)
                itemCard.addView(itemInfo)

                val btnDelete = MaterialButton(context).apply {
                    text = "🗑️ Sil"
                    textSize = 12f
                    setTextColor(Color.parseColor("#F85149"))
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor("#21262D"))
                    strokeColor = ColorStateList.valueOf(Color.parseColor("#30363D"))
                    strokeWidth = dp(1)
                    cornerRadius = dp(8)
                    val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)).apply {
                        marginStart = dp(8)
                    }
                    layoutParams = lp
                    isAllCaps = false
                    setOnClickListener {
                        AlertDialog.Builder(context)
                            .setTitle("Listeyi Kaldır")
                            .setMessage("'${pl.name}' listesi silinsin mi?")
                            .setPositiveButton("Sil") { _, _ ->
                                manager.removePlaylist(pl.link)
                                renderPlaylists()
                                Toast.makeText(context, "Liste silindi", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("İptal", null)
                            .show()
                    }
                }
                itemCard.addView(btnDelete)

                listContainer.addView(itemCard)
            }
        }

        refreshPlaylistsList = {
            renderPlaylists()
        }
        renderPlaylists()

        // Bottom Actions Row
        val bottomActions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(14)
            }
            layoutParams = lp
        }

        val btnSync = MaterialButton(context).apply {
            text = "🔄 Listeleri Yenile"
            textSize = 12f
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#21262D"))
            strokeColor = ColorStateList.valueOf(Color.parseColor("#30363D"))
            strokeWidth = dp(1)
            cornerRadius = dp(10)
            val lp = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                marginEnd = dp(8)
            }
            layoutParams = lp
            isAllCaps = false
            setOnClickListener {
                scope.launch {
                    Toast.makeText(context, "Listeler taranıyor...", Toast.LENGTH_SHORT).show()
                    val count = withContext(Dispatchers.IO) {
                        manager.getStreams(forceRefresh = true).size
                    }
                    Toast.makeText(context, "✓ $count yayın güncellendi", Toast.LENGTH_SHORT).show()
                    renderPlaylists()
                }
            }
        }

        val btnDone = MaterialButton(context).apply {
            text = "Tamam"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            backgroundTintList = ColorStateList.valueOf(brandColor)
            cornerRadius = dp(10)
            val lp = LinearLayout.LayoutParams(0, dp(42), 1f)
            layoutParams = lp
            isAllCaps = false
            setOnClickListener {
                dialog.dismiss()
            }
        }

        bottomActions.addView(btnSync)
        bottomActions.addView(btnDone)
        root.addView(bottomActions)

        dialog.setContentView(scroll)
        dialog.setOnDismissListener {
            scope.cancel()
            onDismiss?.invoke()
        }
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }
}
