package turkspor.papazsports

import android.content.SharedPreferences
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URI

class PapazSportsProvider(
    private val prefs: SharedPreferences,
    private val artwork: turkspor.shared.ChannelArtwork
) : MainAPI() {
    override var mainUrl = prefs.getString("lastGood", START) ?: START
    override var name = "PapazSports • TurkSpor"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Live)
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val mainPage = mainPageOf("/" to "Spor Kanalları")
    override val hasDownloadSupport = false

    val sourceStatus: String get() = mainUrl
    var lastChecked = prefs.getLong("checkedAt", 0L)
        private set

    data class Row(val slug: String, val title: String, val target: String, val source: String, val logo: String) {
        fun artwork() = turkspor.shared.Channel(slug, title, emptyList(), logo)
    }

    private data class Auth(val URL: String? = null, val TOKEN: String? = null)
    private val requestHeaders = mapOf("User-Agent" to UA)

    private suspend fun read(root: String): List<Row> = app.get(
        "$root/", headers = requestHeaders, timeout = 18
    ).document.select(".channel-item, ul.match-list li").mapNotNull { element ->
        val slug = element.attr("data-url").removePrefix("#").takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        val title = element.attr("data-name").replace(Regex("<[^>]+>"), "").trim()
            .ifBlank { element.selectFirst(".name, .match-name")?.text().orEmpty() }
            .takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val target = element.attr("data-target").takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        val source = element.attr("data-source").takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        Row(slug, title, target, source, element.selectFirst("img")?.absUrl("src").orEmpty())
    }.distinctBy { it.slug }

    suspend fun refresh(): Int {
        for (candidate in linkedSetOf(mainUrl, START, "https://www.papazsports1024.pro", "https://www.papazsports1022.pro")) {
            val domain = normalize(candidate) ?: continue
            val rows = runCatching { read(domain) }.getOrDefault(emptyList())
            if (rows.isNotEmpty()) {
                mainUrl = domain
                lastChecked = System.currentTimeMillis()
                prefs.edit().putString("lastGood", domain).putLong("checkedAt", lastChecked).apply()
                return rows.size
            }
        }
        throw ErrorLoadingException("Çalışan PapazSports alanı bulunamadı")
    }

    private fun normalize(value: String): String? = runCatching {
        URI(value.trim().trimEnd('/')).takeIf {
            it.scheme.equals("https", true) && Regex("(www\\.)?papazsports[0-9]+\\.pro", RegexOption.IGNORE_CASE)
                .matches(it.host.orEmpty())
        }?.toString()
    }.getOrNull()

    private suspend fun rows(): List<Row> {
        val current = runCatching { read(mainUrl) }.getOrDefault(emptyList())
        if (current.isNotEmpty()) return current
        refresh()
        return read(mainUrl)
    }

    suspend fun findStreamId(title: String): String? = rows()
        .firstOrNull { it.title.equals(title, true) || it.title.contains(title, true) || title.contains(it.title, true) }
        ?.source

    private fun result(row: Row): SearchResponse = newLiveSearchResponse(row.title, "$mainUrl/#${row.slug}", TvType.Live, false) {
        posterUrl = artwork.poster(row.artwork())
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val current = rows()
        artwork.prepare(current.map { it.artwork() })
        return newHomePageResponse(
            turkspor.common.ChannelGroups.sections(current) { it.title }
                .map { (section, channels) -> HomePageList(section, channels.map(::result), true) },
            false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> = rows()
        .filter { it.title.contains(query, ignoreCase = true) }.map(::result)

    override suspend fun load(url: String): LoadResponse {
        val row = rows().firstOrNull { it.slug == url.substringAfterLast('#') }
            ?: throw ErrorLoadingException("Kanal bulunamadı")
        return newLiveStreamLoadResponse(row.title, url, url) {
            posterUrl = artwork.poster(row.artwork())
            plot = turkspor.common.ChannelGroups.NOTICE
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val row = rows().firstOrNull { it.slug == data.substringAfterLast('#') } ?: return false
        val headers = mapOf("User-Agent" to UA, "X-Requested-With" to "XMLHttpRequest", "Origin" to mainUrl)
        val auth = if (row.target.equals("m3u8", true)) null else {
            val field = if (row.target.equals("viptv", true)) "channel" else "id"
            runCatching {
                app.post("$mainUrl/auth.php", data = mapOf(field to row.source), referer = "$mainUrl/", headers = headers)
                    .parsedSafe<Auth>()
            }.getOrNull()
        }
        val streamUrl = auth?.URL?.takeIf { it.startsWith("http", true) }
            ?: row.source.takeIf { it.startsWith("http", true) } ?: return false
        val streamHeaders = buildMap {
            put("User-Agent", UA)
            put("Origin", mainUrl)
            put("X-Requested-With", "XMLHttpRequest")
            auth?.TOKEN?.takeIf { it.isNotBlank() }?.let { put("usertoken", it); put("pl", "PapazSports") }
        }
        callback(newExtractorLink(name, row.title, streamUrl, ExtractorLinkType.M3U8) {
            referer = "$mainUrl/"
            this.headers = streamHeaders
            quality = Qualities.Unknown.value
        })
        return true
    }

    companion object {
        const val START = "https://www.papazsports1023.pro"
        const val UA = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/139 Mobile Safari/537.36"
    }
}
