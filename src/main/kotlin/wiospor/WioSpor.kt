package wiospor

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import java.net.URI
import java.util.Locale

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
        val lists = WioChannels.GROUPS.mapNotNull { groupName ->
            val channels = WioChannels.all.filter { it.group == groupName }
            if (channels.isEmpty()) null
            else HomePageList(
                name = groupName,
                list = channels.map { channelResult(it) },
                isHorizontalImages = true
            )
        }
        return newHomePageResponse(lists, false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val normQuery = WioChannels.normalize(query)
        if (normQuery.isBlank()) return emptyList()

        return WioChannels.all.filter { channel ->
            val normName = WioChannels.normalize(channel.name)
            val normStd = WioChannels.normalize(channel.standardTitle)
            val matchesAlias = channel.aliases.any { WioChannels.normalize(it).contains(normQuery) }

            normName.contains(normQuery) || normStd.contains(normQuery) || matchesAlias
        }.map { channelResult(it) }
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
            ?: throw ErrorLoadingException("Kanal bulunamadı.")

        return newLiveStreamLoadResponse(channel.name, url, channelUrl(channel)) {
            posterUrl = channel.logo
            plot = "${channel.name} • Kesintisiz Canlı Yayın ve Çoklu Alternatif Kaynak Desteği\n\n" +
                    "Bir yayını açtığınızda tüm çalışan kaynaklar (Selçuk, Taraftarium, İnat, Aslan, Domino vb.) " +
                    "aynı anda taranır ve oynatıcıda alternatif olarak listelenir."
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
            ?: throw ErrorLoadingException("Kanal bulunamadı.")

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
