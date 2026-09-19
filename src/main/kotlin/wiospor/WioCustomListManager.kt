package wiospor

import android.content.Context
import android.content.SharedPreferences
import com.lagradost.cloudstream3.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class CustomPlaylist(
    val id: String,
    val name: String,
    val url: String,
    var isEnabled: Boolean = true
)

data class CustomStream(
    val channelName: String,
    val streamName: String,
    val logo: String,
    val group: String,
    val url: String
)

class WioCustomListManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wiospor_custom_lists_pref", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PLAYLISTS = "saved_playlists_json"
        private const val KEY_ENABLED = "custom_lists_master_enabled"

        @Volatile private var cachedStreams: List<CustomStream>? = null
        @Volatile private var lastFetchTime = 0L
    }

    fun isMasterEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getPlaylists(): List<CustomPlaylist> {
        val jsonStr = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<CustomPlaylist>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CustomPlaylist(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Özel Liste ${i + 1}"),
                        url = obj.optString("url", ""),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun savePlaylists(list: List<CustomPlaylist>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("url", item.url)
                put("isEnabled", item.isEnabled)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
        cachedStreams = null
    }

    fun addPlaylist(name: String, url: String) {
        val current = getPlaylists().toMutableList()
        val newId = UUID.randomUUID().toString()
        val displayName = if (name.isNotBlank()) name.trim() else "Özel Liste ${current.size + 1}"
        current.add(CustomPlaylist(newId, displayName, url.trim(), true))
        savePlaylists(current)
    }

    fun removePlaylist(id: String) {
        val current = getPlaylists().filterNot { it.id == id }
        savePlaylists(current)
    }

    fun togglePlaylist(id: String, enabled: Boolean) {
        val current = getPlaylists()
        current.find { it.id == id }?.isEnabled = enabled
        savePlaylists(current)
    }

    fun getSummary(): String {
        val list = getPlaylists()
        if (list.isEmpty()) return "Liste eklemek için tıkla"
        val activeCount = list.count { it.isEnabled }
        return "$activeCount / ${list.size} liste aktif ✎"
    }

    suspend fun getStreams(forceRefresh: Boolean = false): List<CustomStream> = withContext(Dispatchers.IO) {
        if (!isMasterEnabled()) return@withContext emptyList()
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedStreams != null && (now - lastFetchTime) < 300_000L) {
            return@withContext cachedStreams.orEmpty()
        }

        val playlists = getPlaylists().filter { it.isEnabled && it.url.isNotBlank() }
        if (playlists.isEmpty()) {
            cachedStreams = emptyList()
            return@withContext emptyList()
        }

        val results = mutableListOf<CustomStream>()
        for (pl in playlists) {
            val content = fetchPlaylistContent(pl.url) ?: continue
            results.addAll(parseM3u(content, pl.name))
        }

        cachedStreams = results
        lastFetchTime = now
        results
    }

    private suspend fun fetchPlaylistContent(urlOrPath: String): String? {
        val trimmed = urlOrPath.trim()
        return runCatching {
            when {
                trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                    app.get(trimmed, timeout = 15).text
                }
                trimmed.startsWith("file://", ignoreCase = true) || trimmed.startsWith("/") || (trimmed.length > 2 && trimmed[1] == ':') -> {
                    val filePath = trimmed.removePrefix("file://")
                    File(filePath).readText(Charsets.UTF_8)
                }
                else -> {
                    trimmed
                }
            }
        }.getOrNull()
    }

    private fun parseM3u(content: String, listLabel: String): List<CustomStream> {
        val list = mutableListOf<CustomStream>()
        var curName = ""
        var curLogo = ""
        var curGroup = ""

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val logoMatch = Regex("""tvg-logo=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(line)
                val groupMatch = Regex("""group-title=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(line)
                curLogo = logoMatch?.groupValues?.get(1).orEmpty()
                curGroup = groupMatch?.groupValues?.get(1).orEmpty()
                curName = line.substringAfterLast(",").trim()
            } else if (line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true)) {
                if (curName.isNotBlank() && !curName.startsWith("##") && !curName.startsWith("######")) {
                    list.add(
                        CustomStream(
                            channelName = curName,
                            streamName = "[$listLabel] $curName",
                            logo = curLogo,
                            group = if (curGroup.isNotBlank()) curGroup else listLabel,
                            url = line
                        )
                    )
                }
                curName = ""
            }
        }
        return list
    }

    suspend fun getStreamsForChannel(channel: WioChannel): List<CustomStream> {
        val allStreams = getStreams()
        if (allStreams.isEmpty()) return emptyList()

        return allStreams.filter { stream ->
            WioChannels.matches(channel, stream.channelName, stream.channelName)
        }
    }

    suspend fun getCustomWioChannels(): List<WioChannel> {
        val allStreams = getStreams()
        if (allStreams.isEmpty()) return emptyList()

        return allStreams.groupBy { it.channelName }.map { (name, streams) ->
            val id = "custom_" + name.lowercase().replace(Regex("[^a-z0-9]"), "_").trim('_')
            val logo = streams.firstOrNull { it.logo.isNotBlank() }?.logo ?: ""
            WioChannel(
                id = id,
                name = "📋 $name",
                group = "📋 Özel Liste",
                standardTitle = name,
                aliases = listOf(name),
                logo = logo
            )
        }
    }
}
