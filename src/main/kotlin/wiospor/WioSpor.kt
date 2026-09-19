package wiospor

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import java.net.URI

class WioSpor(private val aggregator: SourceAggregator) : MainAPI() {
    override var mainUrl = "https://raw.githubusercontent.com/Wiojelt/WioSpor/main/"
    override var name = "WioSpor"
    override var lang = "tr"
    override val supportedTypes = setOf(TvType.Live)
    override val hasMainPage = true
    override val hasDownloadSupport = false
    override val mainPage = mainPageOf("all" to "Canlı TV & Spor Kanalları")

    private fun channelUrl(channel: WioChannel): String =
        "wiospor://channel?id=${channel.id}"

    private fun channelResult(channel: WioChannel): SearchResponse =
        newLiveSearchResponse(channel.name, channelUrl(channel), TvType.Live, false) {
            posterUrl = channel.logo
        }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val lists = mutableListOf<HomePageList>()

        // 📋 Kullanıcının eklediği özel listelerden gelen kanallar (en başta gösterilir)
        runCatching {
            val customChannels = aggregator.customListManager.getCustomWioChannels()
            if (customChannels.isNotEmpty()) {
                lists.add(
                    HomePageList(
                        name = "📋 Eklenen Özel Kanallar (${customChannels.size})",
                        list = customChannels.map { channelResult(it) },
                        isHorizontalImages = true
                    )
                )
            }
        }

        // Standart kanal grupları
        WioChannels.GROUPS.forEach { groupName ->
            val channels = WioChannels.all.filter { it.group == groupName }
            if (channels.isNotEmpty()) {
                lists.add(
                    HomePageList(
                        name = groupName,
                        list = channels.map { channelResult(it) },
                        isHorizontalImages = true
                    )
                )
            }
        }

        return newHomePageResponse(lists, false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val normQuery = WioChannels.normalize(query)
        if (normQuery.isBlank()) return emptyList()

        val results = mutableListOf<SearchResponse>()

        // Özel kanal araması
        runCatching {
            val customMatches = aggregator.customListManager.getCustomWioChannels().filter {
                WioChannels.normalize(it.name).contains(normQuery) ||
                WioChannels.normalize(it.standardTitle).contains(normQuery)
            }.map { channelResult(it) }
            results.addAll(customMatches)
        }

        // Standart kanal araması
        val standardMatches = WioChannels.all.filter { channel ->
            val normName = WioChannels.normalize(channel.name)
            val normStd = WioChannels.normalize(channel.standardTitle)
            val matchesAlias = channel.aliases.any { WioChannels.normalize(it).contains(normQuery) }

            normName.contains(normQuery) || normStd.contains(normQuery) || matchesAlias
        }.map { channelResult(it) }
        results.addAll(standardMatches)

        return results
    }

    private fun parseChannelId(data: String): String? {
        return try {
            val uri = URI(data)
            uri.query?.split("&")?.mapNotNull { param ->
                val parts = param.split("=")
                if (parts.size == 2 && parts[0] == "id") parts[1] else null
            }?.firstOrNull() ?: data.substringAfter("id=").substringBefore("&")
        } catch (_: Exception) {
            data.substringAfter("id=").substringBefore("&")
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val channelId = parseChannelId(url)
            ?: throw ErrorLoadingException("Kanal kimliği eksik.")
        val channel = WioChannels.byId(channelId)
            ?: aggregator.customListManager.getCustomWioChannels().find { it.id == channelId }
            ?: throw ErrorLoadingException("Kanal bulunamadı.")

        return newLiveStreamLoadResponse(channel.name, url, channelUrl(channel)) {
            posterUrl = channel.logo
            plot = "\u200B"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val channelId = parseChannelId(data)
            ?: throw ErrorLoadingException("Kanal kimliği eksik.")
        val channel = WioChannels.byId(channelId)
            ?: aggregator.customListManager.getCustomWioChannels().find { it.id == channelId }
            ?: throw ErrorLoadingException("Kanal bulunamadı.")

        // Özel kanal ise doğrudan özel listeden oynat
        if (channelId.startsWith("custom_")) {
            val streams = aggregator.customListManager.getStreams().filter {
                val id = "custom_" + it.title.lowercase().replace(Regex("[^a-z0-9]"), "_").trim('_')
                id == channelId || it.title.equals(channel.standardTitle, ignoreCase = true)
            }
            if (streams.isNotEmpty()) {
                streams.forEach { stream ->
                    callback(
                        ExtractorLink(
                            source = "WioSpor",
                            name = "[Özel Liste] ${stream.title}",
                            url = stream.url,
                            referer = "",
                            quality = Qualities.Unknown.value,
                            type = if (stream.url.contains(".m3u8", ignoreCase = true)) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                        )
                    )
                }
                return true
            }
        }

        val found = aggregator.fetchAlternativeLinks(channel, callback)
        if (!found) {
            throw ErrorLoadingException(
                "Bu kanal için şu anda aktif yayın kaynağı bulunamadı. " +
                "Yayınlar maç saatinde açılıyor olabilir veya WARP/VPN gerekebilir."
            )
        }
        return true
    }
}
