package wiospor

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

data class TvLink(
    val name: String,
    val link: String
)

data class TvPlaylistItem(
    val title: String,
    val url: String,
    val attributes: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val userAgent: String? = null,
    val key: String? = null,
    val keyid: String? = null
)

data class TvPlaylist(
    val items: List<TvPlaylistItem>
)

object TvPlaylistParser {
    private const val TAG = "TvPlaylistParser"
    private const val EXT_M3U = "#EXTM3U"
    private const val EXT_INF = "#EXTINF"
    private const val EXT_VLC_OPT = "#EXTVLCOPT"

    private val ATTRIBUTES_REGEX = Regex("""(\w+-\w+|tvg-\w+|group-title)="([^"]*)"""")

    fun parseM3U(input: String): TvPlaylist {
        return parseM3U(ByteArrayInputStream(input.toByteArray(Charsets.UTF_8)))
    }

    fun parseM3U(stream: InputStream): TvPlaylist {
        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
        val rawLines = reader.readLines()
        Log.d(TAG, "Starting M3U parse. Total raw lines: ${rawLines.size}")

        val items = mutableListOf<TvPlaylistItem>()
        var currentItem = TvPlaylistItem(title = "", url = "")
        var currentVlcHeaders = mutableMapOf<String, String>()

        for (rawLine in rawLines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            try {
                when {
                    line.startsWith(EXT_INF, ignoreCase = true) -> {
                        val attributes = getAttributes(line)
                        val title = getTitle(line, attributes)
                        currentItem = currentItem.copy(
                            title = title,
                            attributes = attributes
                        )
                    }
                    line.startsWith(EXT_VLC_OPT, ignoreCase = true) -> {
                        val opt = line.substringAfter(":", "").trim()
                        val key = opt.substringBefore("=").trim().lowercase()
                        val value = opt.substringAfter("=").trim()
                        if (key == "http-user-agent" || key == "user-agent") {
                            currentVlcHeaders["User-Agent"] = value
                        } else if (key == "http-referrer" || key == "referrer") {
                            currentVlcHeaders["Referer"] = value
                        }
                    }
                    !line.startsWith("#") -> {
                        val finalUrl = getUrl(line)
                        val pipeHeaders = mutableMapOf<String, String>()
                        pipeHeaders.putAll(currentVlcHeaders)

                        val userAgent = getUrlParameter(line, "user-agent") ?: currentVlcHeaders["User-Agent"]
                        val referer = getUrlParameter(line, "referer") ?: currentVlcHeaders["Referer"]
                        if (!userAgent.isNullOrBlank()) pipeHeaders["User-Agent"] = userAgent
                        if (!referer.isNullOrBlank()) pipeHeaders["Referer"] = referer

                        val key = getUrlParameter(line, "key")
                        val keyid = getUrlParameter(line, "keyid")

                        val itemTitle = if (currentItem.title.isNotBlank()) currentItem.title else finalUrl.substringAfterLast("/")
                        if (!itemTitle.startsWith("##") && !itemTitle.startsWith("######")) {
                            items.add(
                                currentItem.copy(
                                    title = itemTitle,
                                    url = finalUrl,
                                    headers = pipeHeaders,
                                    userAgent = userAgent,
                                    key = key,
                                    keyid = keyid
                                )
                            )
                        }
                        currentItem = TvPlaylistItem(title = "", url = "")
                        currentVlcHeaders = mutableMapOf()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing line: $line", e)
            }
        }

        Log.d(TAG, "Finished M3U parse. Successfully extracted ${items.size} channels")
        return TvPlaylist(items)
    }

    private fun getAttributes(line: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        ATTRIBUTES_REGEX.findAll(line).forEach { match ->
            map[match.groupValues[1]] = match.groupValues[2]
        }
        return map
    }

    private fun getTitle(line: String, attributes: Map<String, String>): String {
        val commaTitle = line.substringAfterLast(",").trim()
        if (commaTitle.isNotBlank()) return commaTitle
        return attributes["tvg-name"] ?: attributes["tvg-id"] ?: "Bilinmeyen Kanal"
    }

    private fun getUrl(line: String): String {
        return if (line.contains("|")) line.substringBefore("|").trim() else line
    }

    private fun getUrlParameter(line: String, paramName: String): String? {
        if (!line.contains("|")) return null
        val paramsPart = line.substringAfter("|")
        val match = Regex("""(?i)(?:^|&)${Regex.escape(paramName)}=([^&]*)""").find(paramsPart)
        return match?.groupValues?.get(1)?.trim()
    }
}

class WioCustomListManager(private val context: Context) {
    companion object {
        const val STORAGE_KEY = "plt_tv_links"
        const val LEGACY_STORAGE_KEY = "iptv_links"
        private const val PREFS_FILE = "wiospor_custom_lists_pref"
        private const val KEY_PLAYLISTS = "saved_playlists_json"

        @Volatile private var cachedStreams: List<TvPlaylistItem>? = null
        @Volatile private var lastFetchTime = 0L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun getSavedLinks(): List<TvLink> {
        val storedJson = runCatching {
            getKey<String>(STORAGE_KEY) ?: getKey<String>(LEGACY_STORAGE_KEY)
        }.getOrNull()

        val jsonStr = storedJson ?: prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<TvLink>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name", "Özel Liste ${i + 1}")
                val url = obj.optString("url", obj.optString("link", ""))
                if (url.isNotBlank()) {
                    list.add(TvLink(name, url))
                }
            }
            list
        }.getOrDefault(emptyList())
    }

    fun saveLinks(links: List<TvLink>) {
        val arr = JSONArray()
        for (item in links) {
            val obj = JSONObject().apply {
                put("name", item.name)
                put("url", item.link)
                put("link", item.link)
            }
            arr.put(obj)
        }
        val jsonStr = arr.toString()

        runCatching {
            setKey(STORAGE_KEY, jsonStr)
            setKey(LEGACY_STORAGE_KEY, jsonStr)
        }

        prefs.edit().putString(KEY_PLAYLISTS, jsonStr).apply()
        cachedStreams = null
    }

    fun addPlaylist(name: String, url: String) {
        val current = getSavedLinks().toMutableList()
        val trimmedUrl = url.trim()
        val displayName = if (name.isNotBlank()) name.trim() else "Özel Liste ${current.size + 1}"
        current.removeAll { it.link.equals(trimmedUrl, ignoreCase = true) }
        current.add(TvLink(displayName, trimmedUrl))
        saveLinks(current)
    }

    fun removePlaylist(linkUrl: String) {
        val current = getSavedLinks().filterNot { it.link.equals(linkUrl.trim(), ignoreCase = true) }
        saveLinks(current)
    }

    fun getSummary(): String {
        val list = getSavedLinks()
        if (list.isEmpty()) return "Liste eklemek için tıkla"
        return "${list.size} liste aktif ✎"
    }

    suspend fun getStreams(forceRefresh: Boolean = false): List<TvPlaylistItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedStreams != null && (now - lastFetchTime) < 300_000L) {
            return@withContext cachedStreams.orEmpty()
        }

        val playlists = getSavedLinks().filter { it.link.isNotBlank() }
        if (playlists.isEmpty()) {
            cachedStreams = emptyList()
            return@withContext emptyList()
        }

        val results = mutableListOf<TvPlaylistItem>()
        for (pl in playlists) {
            val content = fetchPlaylistContent(pl.link) ?: continue
            val parsed = TvPlaylistParser.parseM3U(content)
            results.addAll(parsed.items)
        }

        cachedStreams = results
        lastFetchTime = now
        results
    }

    suspend fun fetchPlaylistContent(urlOrPath: String): String? {
        val trimmed = urlOrPath.trim()
        return runCatching {
            when {
                trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                    try {
                        app.get(trimmed, timeout = 15).text
                    } catch (_: Exception) {
                        // HttpURLConnection fallback
                        val conn = URL(trimmed).openConnection() as HttpURLConnection
                        conn.connectTimeout = 15000
                        conn.readTimeout = 15000
                        conn.setRequestProperty("User-Agent", "Player (Linux; Android 14)")
                        conn.inputStream.bufferedReader().use { it.readText() }
                    }
                }
                trimmed.startsWith("content://", ignoreCase = true) -> {
                    context.contentResolver.openInputStream(Uri.parse(trimmed))?.use { input ->
                        input.bufferedReader().readText()
                    }
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

    suspend fun getStreamsForChannel(channel: WioChannel): List<TvPlaylistItem> {
        val allStreams = getStreams()
        if (allStreams.isEmpty()) return emptyList()

        return allStreams.filter { stream ->
            val tvgName = stream.attributes["tvg-name"].orEmpty()
            WioChannels.matches(channel, stream.title, tvgName)
        }
    }

    suspend fun getCustomWioChannels(): List<WioChannel> {
        val allStreams = getStreams()
        if (allStreams.isEmpty()) return emptyList()

        return allStreams.groupBy { it.title }.map { (name, streams) ->
            val id = "custom_" + name.lowercase().replace(Regex("[^a-z0-9]"), "_").trim('_')
            val logo = streams.firstOrNull { it.attributes["tvg-logo"]?.isNotBlank() == true }?.attributes?.get("tvg-logo") ?: ""
            val group = streams.firstOrNull { it.attributes["group-title"]?.isNotBlank() == true }?.attributes?.get("group-title") ?: "📋 Özel Liste"
            WioChannel(
                id = id,
                name = "📋 $name",
                group = group,
                standardTitle = name,
                aliases = listOf(name),
                logo = logo
            )
        }
    }
}
