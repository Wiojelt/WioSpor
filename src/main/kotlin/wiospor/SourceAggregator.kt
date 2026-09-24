package wiospor

import android.app.ActivityManager
import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mapper
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicInteger

interface SourceWorker {
    val id: String
    val displayName: String
    suspend fun checkOnline(): Boolean
    suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean
}

class SourceAggregator(private val context: Context) {
    companion object {
        // Hızlı sonuç için önce taranan sağlayıcılar. Bu liste etkin kaynak sayısını
        // sınırlamaz; TV Box kapalıyken kullanıcı tarafından kapatılmayan tüm kaynaklar taranır.
        val DEFAULT_ENABLED_SOURCES = setOf(
            "beyazelma",
            "domino",
            "inat",
            "kralspor",
            "betmatiktv",
            "patron",
            "viontv"
        )
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wiospor_source_prefs", Context.MODE_PRIVATE)

    val customListManager = WioCustomListManager(context)

    data class HealthStatus(
        val totalCount: Int,
        val onlineCount: Int,
        val details: Map<String, Boolean>
    )

    private fun wrapLink(label: String, channel: WioChannel, link: ExtractorLink): ExtractorLink {
        return ExtractorLink(
            source = "WioSpor",
            // Kalite, kaynak adının parçası değil; CloudStream oynatıcı kalite
            // seçicisini link.quality alanından doldurur. Böylece her sağlayıcı
            // tek kaynak satırı olarak görünür.
            name = "[$label] ${channel.name}",
            url = link.url,
            referer = link.referer,
            quality = link.quality,
            type = link.type,
            headers = link.headers
        )
    }

    private val selcukResolver by lazy { turkspor.DomainResolver(prefs) }
    private val selcukArtwork by lazy { turkspor.ChannelArtwork(context) }
    private val selcukApi by lazy { turkspor.SelcukSports(selcukResolver, selcukArtwork) }

    private val taraftariumResolver by lazy { turkspor.taraftarium.DomainResolver(prefs) }
    private val taraftariumArtwork by lazy { turkspor.taraftarium.ChannelArtwork(context) }
    private val taraftariumApi by lazy { turkspor.taraftarium.Taraftarium24(taraftariumResolver, taraftariumArtwork) }

    private val inatResolver by lazy { turkspor.inat.DomainResolver(prefs) }
    private val inatArtwork by lazy { turkspor.inat.ChannelArtwork(context) }
    private val inatApi by lazy { turkspor.inat.InatTV(inatResolver, inatArtwork) }

    private val ardaResolver by lazy { turkspor.arda.DomainResolver(prefs) }
    private val ardaArtwork by lazy { turkspor.arda.ChannelArtwork(context) }
    private val ardaApi by lazy { turkspor.arda.ArdaSpor(ardaResolver, ardaArtwork) }

    private val mahsunResolver by lazy { turkspor.mahsun.DomainResolver(prefs) }
    private val mahsunArtwork by lazy { turkspor.mahsun.ChannelArtwork(context) }
    private val mahsunApi by lazy { turkspor.mahsun.MahsunSports(mahsunResolver, mahsunArtwork) }

    private val kralsporResolver by lazy { turkspor.kralspor.DomainResolver(prefs) }
    private val kralsporArtwork by lazy { turkspor.kralspor.ChannelArtwork(context) }
    private val kralsporApi by lazy { turkspor.kralspor.KralSporHD(kralsporResolver, kralsporArtwork) }

    private val crexResolver by lazy { turkspor.crex.DomainResolver(prefs) }
    private val crexArtwork by lazy { turkspor.crex.ChannelArtwork(context) }
    private val crexApi by lazy { turkspor.crex.Crex(crexResolver, crexArtwork) }

    private fun createSharedWorker(specKey: String, label: String): SourceWorker? {
        val spec = turkspor.shared.SourceSpec.all[specKey] ?: return null
        val resolver = turkspor.shared.DomainResolver(prefs, spec)
        val artwork = turkspor.shared.ChannelArtwork(context, spec.key)
        val api = turkspor.shared.SportsProvider(spec, resolver, artwork)

        return object : SourceWorker {
            override val id: String = specKey
            override val displayName: String = label

            override suspend fun checkOnline(): Boolean = runCatching {
                val channels = resolver.resolve().channels
                channels.isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(
                channel: WioChannel,
                callback: (ExtractorLink) -> Unit
            ): Boolean {
                val site = resolver.resolve()
                val match = site.channels.firstOrNull { WioChannels.matches(channel, it.title, it.id) }
                    ?: return false
                val stableUrl = "${spec.roots.first()}turkspor?id=${URLEncoder.encode(match.id, "UTF-8")}"
                var emitted = false
                try {
                    api.loadLinks(stableUrl, false, {}) { link ->
                        emitted = true
                        callback(wrapLink(label, channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        }
    }

    private val inatBoxCatalogue by lazy { turkspor.inatbox.InatCatalogue(prefs) }
    private val inatBoxArtwork by lazy { turkspor.shared.ChannelArtwork(context, "inatbox") }
    private val inatBoxApi by lazy { turkspor.inatbox.InatBox(inatBoxCatalogue, inatBoxArtwork) }

    private val domatesApi by lazy { turkspor.domates.DomatesTVProvider() }
    private val dominoApi by lazy { turkspor.domino.DominoTVProvider() }

    private val papazPrefs by lazy { context.getSharedPreferences("wiospor_papazsports", Context.MODE_PRIVATE) }
    private val papazArtwork by lazy { turkspor.shared.ChannelArtwork(context, "papazsports") }
    private val papazApi by lazy { turkspor.papazsports.PapazSportsProvider(papazPrefs, papazArtwork) }

    private val jestPrefs by lazy { context.getSharedPreferences("wiospor_jestyayin", Context.MODE_PRIVATE) }
    private val jestArtwork by lazy { turkspor.shared.ChannelArtwork(context, "jestyayin") }
    private val jestApi by lazy { turkspor.jestyayin.JestYayinProvider(jestPrefs, jestArtwork) }

    private var cachedPatronChannels: Pair<Long, List<Pair<String, String>>>? = null
    private var cachedPatronBaseUrl: Pair<Long, String>? = null

    private suspend fun getPatronBaseUrl(): String? {
        val now = System.currentTimeMillis()
        cachedPatronBaseUrl?.let { (ts, url) ->
            if (now - ts < 300_000L) return url
        }
        return try {
            val res = app.get("https://patronsports2.cfd/domain.php", referer = "https://patronsports2.cfd/", timeout = 8)
            if (res.code == 200) {
                val tree = mapper.readTree(res.text)
                val base = tree.path("baseurl").asText("").trimEnd('/') + "/"
                if (base.length > 1) {
                    cachedPatronBaseUrl = now to base
                    base
                } else null
            } else null
        } catch (_: Exception) { null }
    }

    private suspend fun getPatronChannels(): List<Pair<String, String>> {
        val now = System.currentTimeMillis()
        cachedPatronChannels?.let { (ts, list) ->
            if (now - ts < 120_000L) return list
        }
        return try {
            val res = app.get("https://patronsports2.cfd/channels.php", timeout = 10)
            if (res.code == 200) {
                val tree = mapper.readTree(res.text)
                val list = mutableListOf<Pair<String, String>>()
                for (node in tree) {
                    val mac = node.path("Mac").asText("")
                    val url = node.path("URL").asText("")
                    val id = url.substringAfter("id=", "").substringBefore("&")
                    if (mac.isNotEmpty() && id.isNotEmpty()) {
                        list.add(mac to id)
                    }
                }
                cachedPatronChannels = now to list
                list
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private val vionApi by lazy { dev.wiojelt.viontv.VionTVProvider() }

    val workers: List<SourceWorker> by lazy {
        val list = mutableListOf<SourceWorker>()

        // ============================================================
        // 1..5. ÖNERİLEN 5 SAĞLAYICI (Varsayılan Açık & En Öncelikli)
        // ============================================================

        // 1. BeyazElma
        createSharedWorker("beyazelma", "BeyazElma")?.let { list.add(it) }

        // 2. Domino TV
        list.add(object : SourceWorker {
            override val id: String = "domino"
            override val displayName: String = "Domino TV"

            override suspend fun checkOnline(): Boolean = runCatching {
                dominoApi.getChannels().isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                var emitted = false
                try {
                    val channels = dominoApi.getChannels()
                    val match = channels.firstOrNull { WioChannels.matches(channel, it.title, it.id.toString()) } ?: return false
                    dominoApi.loadLinks(match.pageUrl(), false, {}) { link ->
                        emitted = true
                        callback(wrapLink("Domino", channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // 3. İnat TV
        list.add(object : SourceWorker {
            override val id: String = "inat"
            override val displayName: String = "İnat TV"

            override suspend fun checkOnline(): Boolean = runCatching {
                inatResolver.resolve().channels.isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                val site = inatResolver.resolve()
                val match = site.channels.firstOrNull { WioChannels.matches(channel, it.title, it.id) } ?: return false
                val stableUrl = "${turkspor.inat.DomainResolver.GATEWAY}turkspor?id=${URLEncoder.encode(match.id, "UTF-8")}&title=${URLEncoder.encode(match.title, "UTF-8")}"
                var emitted = false
                try {
                    inatApi.loadLinks(stableUrl, false, {}) { link ->
                        emitted = true
                        callback(wrapLink("İnat TV", channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // 4. KralSporHD
        list.add(object : SourceWorker {
            override val id: String = "kralspor"
            override val displayName: String = "KralSporHD"

            override suspend fun checkOnline(): Boolean = runCatching {
                kralsporResolver.resolve().channels.isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                val site = kralsporResolver.resolve()
                val match = site.channels.firstOrNull { WioChannels.matches(channel, it.title, it.id) } ?: return false
                val stableUrl = "${turkspor.kralspor.DomainResolver.GATEWAY}turkspor?id=${URLEncoder.encode(match.id, "UTF-8")}&title=${URLEncoder.encode(match.title, "UTF-8")}"
                var emitted = false
                try {
                    kralsporApi.loadLinks(stableUrl, false, {}) { link ->
                        emitted = true
                        callback(wrapLink("KralSpor", channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // 5. BetmatikTV
        createSharedWorker("betmatiktv", "BetmatikTV")?.let { list.add(it) }

        // 6. PatronHD
        list.add(object : SourceWorker {
            override val id: String = "patron"
            override val displayName: String = "PatronHD"

            override suspend fun checkOnline(): Boolean = runCatching {
                getPatronBaseUrl() != null
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                var emitted = false
                try {
                    val channels = getPatronChannels()
                    val match = channels.firstOrNull { (mac, id) -> WioChannels.matches(channel, mac, id) }
                        ?: return false

                    val baseUrl = getPatronBaseUrl() ?: return false
                    val streamUrl = "${baseUrl}${match.second}/mono.m3u8"
                    val playbackHeaders = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Mobile)",
                        "Origin" to "https://patronsports2.cfd",
                        "Referer" to "https://patronsports2.cfd/",
                        "Sec-Fetch-Site" to "cross-site",
                        "Sec-Fetch-Mode" to "cors",
                        "Sec-Fetch-Dest" to "empty"
                    )

                    callback(
                        wrapLink(
                            "Patron",
                            channel,
                            newExtractorLink("PatronHD", channel.name, streamUrl, ExtractorLinkType.M3U8) {
                                referer = "https://patronsports2.cfd/"
                                headers = playbackHeaders
                                quality = Qualities.Unknown.value
                            }
                        )
                    )
                    emitted = true
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // 7. VİONTV
        list.add(object : SourceWorker {
            override val id: String = "viontv"
            override val displayName: String = "VİONTV"

            override suspend fun checkOnline(): Boolean = runCatching {
                vionApi.getChannels().isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                var emitted = false
                try {
                    val channels = vionApi.getChannels()
                    val matches = channels.filter { c ->
                        WioChannels.matches(channel, c.optString("name"), c.optString("id"))
                    }
                    for (match in matches) {
                        vionApi.loadLinks(match.toString(), false, {}) { link ->
                            emitted = true
                            callback(wrapLink("VİONTV", channel, link))
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // ============================================================
        // 8..17. DİĞER WEB SAĞLAYICILARI
        // ============================================================

        // 6. SelçukSports
        list.add(object : SourceWorker {
            override val id: String = "selcuk"
            override val displayName: String = "SelçukSports"

            override suspend fun checkOnline(): Boolean = runCatching {
                selcukResolver.resolve().channels.isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                val site = selcukResolver.resolve()
                val match = site.channels.firstOrNull { WioChannels.matches(channel, it.title, it.id) } ?: return false
                val stableUrl = "${turkspor.DomainResolver.GATEWAY}turkspor?id=${URLEncoder.encode(match.id, "UTF-8")}&title=${URLEncoder.encode(match.title, "UTF-8")}"
                var emitted = false
                try {
                    selcukApi.loadLinks(stableUrl, false, {}) { link ->
                        emitted = true
                        callback(wrapLink("Selçuk", channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // 7. Taraftarium24
        list.add(object : SourceWorker {
            override val id: String = "taraftarium"
            override val displayName: String = "Taraftarium24"

            override suspend fun checkOnline(): Boolean = runCatching {
                taraftariumResolver.resolve().channels.isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                val site = taraftariumResolver.resolve()
                val match = site.channels.firstOrNull { WioChannels.matches(channel, it.title, it.id) } ?: return false
                val stableUrl = "${turkspor.taraftarium.DomainResolver.GATEWAY}turkspor?id=${URLEncoder.encode(match.id, "UTF-8")}&title=${URLEncoder.encode(match.title, "UTF-8")}"
                var emitted = false
                try {
                    taraftariumApi.loadLinks(stableUrl, false, {}) { link ->
                        emitted = true
                        callback(wrapLink("Taraftarium", channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // 8. ArdaSpor
        list.add(object : SourceWorker {
            override val id: String = "arda"
            override val displayName: String = "ArdaSpor"

            override suspend fun checkOnline(): Boolean = runCatching {
                ardaResolver.resolve().channels.isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                val site = ardaResolver.resolve()
                val match = site.channels.firstOrNull { WioChannels.matches(channel, it.title, it.id) } ?: return false
                val stableUrl = "${turkspor.arda.DomainResolver.GATEWAY}turkspor?id=${URLEncoder.encode(match.id, "UTF-8")}&title=${URLEncoder.encode(match.title, "UTF-8")}"
                var emitted = false
                try {
                    ardaApi.loadLinks(stableUrl, false, {}) { link ->
                        emitted = true
                        callback(wrapLink("ArdaSpor", channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // 9. MahsunSports
        list.add(object : SourceWorker {
            override val id: String = "mahsun"
            override val displayName: String = "MahsunSports"

            override suspend fun checkOnline(): Boolean = runCatching {
                mahsunResolver.resolve().channels.isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                val site = mahsunResolver.resolve()
                val match = site.channels.firstOrNull { WioChannels.matches(channel, it.title, it.id) } ?: return false
                val stableUrl = "${turkspor.mahsun.DomainResolver.GATEWAY}turkspor?id=${URLEncoder.encode(match.id, "UTF-8")}&title=${URLEncoder.encode(match.title, "UTF-8")}"
                var emitted = false
                try {
                    mahsunApi.loadLinks(stableUrl, false, {}) { link ->
                        emitted = true
                        callback(wrapLink("Mahsun", channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // 10. Crex
        list.add(object : SourceWorker {
            override val id: String = "crex"
            override val displayName: String = "Crex"

            override suspend fun checkOnline(): Boolean = runCatching {
                crexResolver.resolve().channels.isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                val site = crexResolver.resolve()
                val match = site.channels.firstOrNull { WioChannels.matches(channel, it.title, it.id) } ?: return false
                val stableUrl = "${turkspor.crex.DomainResolver.GATEWAY}turkspor?id=${URLEncoder.encode(match.id, "UTF-8")}&title=${URLEncoder.encode(match.title, "UTF-8")}"
                var emitted = false
                try {
                    crexApi.loadLinks(stableUrl, false, {}) { link ->
                        emitted = true
                        callback(wrapLink("Crex", channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // 11. Domates TV
        list.add(object : SourceWorker {
            override val id: String = "domates"
            override val displayName: String = "Domates TV"

            override suspend fun checkOnline(): Boolean = runCatching {
                domatesApi.getChannels().isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                var emitted = false
                try {
                    val channels = domatesApi.getChannels()
                    val match = channels.firstOrNull { WioChannels.matches(channel, it.title, it.id) } ?: return false
                    domatesApi.loadLinks(match.pageUrl(), false, {}) { link ->
                        emitted = true
                        callback(wrapLink("Domates", channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // 12. İnterSporTV
        createSharedWorker("intersportv", "İnterSporTV")?.let { list.add(it) }

        // 13. MaçKeyfi
        createSharedWorker("mackeyfi", "MaçKeyfi")?.let { list.add(it) }

        // 14. ZbahisTV
        createSharedWorker("zbahistv", "ZbahisTV")?.let { list.add(it) }

        // 15. İnat Box
        list.add(object : SourceWorker {
            override val id: String = "inatbox"
            override val displayName: String = "İnat Box"

            override suspend fun checkOnline(): Boolean = runCatching {
                inatBoxCatalogue.resolve().channels.isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                val site = inatBoxCatalogue.resolve()
                val match = site.channels.firstOrNull { WioChannels.matches(channel, it.title, it.id) } ?: return false
                val stableUrl = "https://github.com/Wiojelt/TurkSpor/channel?id=${URLEncoder.encode(match.id, "UTF-8")}"
                var emitted = false
                try {
                    inatBoxApi.loadLinks(stableUrl, false, {}) { link ->
                        emitted = true
                        callback(wrapLink("İnat Box", channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // 16. PapazSports
        list.add(object : SourceWorker {
            override val id: String = "papazsports"
            override val displayName: String = "PapazSports"

            override suspend fun checkOnline(): Boolean = runCatching {
                papazApi.search("").isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                var emitted = false
                try {
                    val rows = papazApi.search("")
                    val match = rows.firstOrNull { WioChannels.matches(channel, it.name, it.url.substringAfterLast('#')) } ?: return false
                    papazApi.loadLinks(match.url, false, {}) { link ->
                        emitted = true
                        callback(wrapLink("Papaz", channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // 17. JestYayın
        list.add(object : SourceWorker {
            override val id: String = "jestyayin"
            override val displayName: String = "JestYayın"

            override suspend fun checkOnline(): Boolean = runCatching {
                jestApi.search("").isNotEmpty()
            }.getOrDefault(false)

            override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                var emitted = false
                try {
                    val rows = jestApi.search("")
                    val match = rows.firstOrNull { WioChannels.matches(channel, it.name, it.url) } ?: return false
                    val id = match.url.substringAfter("#jest:").substringBefore(":")
                    jestApi.loadLinks(id, false, {}) { link ->
                        emitted = true
                        callback(wrapLink("JestYayın", channel, link))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
                return emitted
            }
        })

        // ============================================================
        // 18..44. Aslan IPTV Listeleri (27 Adet Aktif Kaynak)
        // ============================================================
        val aslanRegistry = turkspor.aslan.AslanRegistry(
            context.getSharedPreferences("wiospor_aslan_registry", Context.MODE_PRIVATE)
        )
        turkspor.aslan.AslanSources.items.forEach { (sourceId, sourceTitle) ->
            val catalogue = turkspor.aslan.AslanCatalogue(
                context.getSharedPreferences("wiospor_aslan_$sourceId", Context.MODE_PRIVATE),
                sourceId,
                aslanRegistry
            )
            list.add(object : SourceWorker {
                override val id: String = "aslan_$sourceId"
                override val displayName: String = "Aslan • $sourceTitle"

                override suspend fun checkOnline(): Boolean = runCatching {
                    catalogue.resolve().channels.isNotEmpty()
                }.getOrDefault(false)

                override suspend fun fetchLinks(channel: WioChannel, callback: (ExtractorLink) -> Unit): Boolean {
                    var emitted = false
                    try {
                        val site = catalogue.resolve()
                        val match = site.channels.firstOrNull { WioChannels.matches(channel, it.title, it.id) }
                            ?: return false
                        val candidates = match.players.sortedByDescending {
                            turkspor.common.HlsQuality.hint(turkspor.aslan.AslanData.mapper.readTree(it).path("label").asText())
                        }
                        for (batch in candidates.take(6).chunked(3)) {
                            val links = coroutineScope {
                                batch.map { value ->
                                    async<List<ExtractorLink>?> {
                                        try {
                                            val row = turkspor.aslan.AslanData.mapper.readTree(value)
                                            val url = turkspor.aslan.AslanData.http(row.path("url").asText()) ?: return@async null
                                            val headers = mutableMapOf("User-Agent" to turkspor.DomainResolver.UA)
                                            row.path("headers").fields().forEach { (k, v) -> headers[k] = v.asText() }
                                            val response = turkspor.aslan.AslanHttp.get(url, headers, 8192)
                                            if (response.code == 200 && response.text.trimStart('\uFEFF', ' ', '\r', '\n').startsWith("#EXTM3U")) {
                                                turkspor.common.HlsQuality.links(
                                                    "WioSpor",
                                                    row.path("label").asText(channel.name),
                                                    response.url,
                                                    response.text,
                                                    headers["Referer"].orEmpty(),
                                                    headers
                                                )
                                            } else if (response.code in 200..299) {
                                                listOf(
                                                    ExtractorLink(
                                                        source = "WioSpor",
                                                        name = "[$sourceTitle] ${channel.name}",
                                                        url = url,
                                                        referer = headers["Referer"].orEmpty(),
                                                        quality = Qualities.Unknown.value,
                                                        type = ExtractorLinkType.M3U8,
                                                        headers = headers
                                                    )
                                                )
                                            } else null
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                }.awaitAll().filterNotNull().flatten()
                            }
                            links.forEach { link ->
                                emitted = true
                                callback(wrapLink(sourceTitle, channel, link))
                            }
                            if (links.isNotEmpty()) break
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) { }
                    return emitted
                }
            })
        }

        list
    }

    fun enableOnlyRecommendedSources() {
        val disabled = workers.map { it.id }.filter { it !in DEFAULT_ENABLED_SOURCES }.toSet()
        prefs.edit()
            .putStringSet("disabled_sources", disabled)
            .putBoolean("sources_initialized_v10", true)
            .apply()
    }

    fun enableAllSources() {
        prefs.edit()
            .putStringSet("disabled_sources", emptySet())
            .putBoolean("sources_initialized_v10", true)
            .apply()
    }

    /**
     * v22'nin otomatik "önerilen" profili yalnızca birkaç sağlayıcıyı açık
     * bırakıyordu. v24'te bu otomatik profil bir kez kaldırılır; kullanıcının
     * elle yaptığı farklı seçimlere dokunulmaz.
     */
    fun migrateLegacySourceProfile() {
        if (prefs.getBoolean("source_profile_migrated_v24", false)) return
        val editor = prefs.edit().putBoolean("source_profile_migrated_v24", true)
        if (prefs.getBoolean("sources_initialized_v10", false)) {
            val disabled = prefs.getStringSet("disabled_sources", emptySet()) ?: emptySet()
            val legacyRecommendedDisabled = workers.map { it.id }
                .filter { it !in DEFAULT_ENABLED_SOURCES }
                .toSet()
            if (disabled == legacyRecommendedDisabled) {
                editor.putStringSet("disabled_sources", emptySet())
            }
        }
        editor.apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed_v10", false)
    }

    fun setOnboardingCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean("onboarding_completed_v10", completed).apply()
    }

    fun isSourceEnabled(sourceId: String): Boolean {
        if (!prefs.getBoolean("sources_initialized_v10", false)) {
            // Yeni kurulumda normal mod bütün kaynakları tarar. Önceki davranış yalnızca
            // altı hızlı kaynağı açtığı için kaynak listesi gereksiz biçimde daralıyordu.
            return true
        }
        val disabledSet = prefs.getStringSet("disabled_sources", emptySet()) ?: emptySet()
        return sourceId !in disabledSet
    }

    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        if (!prefs.getBoolean("sources_initialized_v10", false)) {
            val disabled = mutableSetOf<String>()
            if (enabled) disabled.remove(sourceId) else disabled.add(sourceId)
            prefs.edit()
                .putStringSet("disabled_sources", disabled)
                .putBoolean("sources_initialized_v10", true)
                .apply()
            return
        }
        val disabledSet = (prefs.getStringSet("disabled_sources", emptySet()) ?: emptySet()).toMutableSet()
        if (enabled) {
            disabledSet.remove(sourceId)
        } else {
            disabledSet.add(sourceId)
        }
        prefs.edit().putStringSet("disabled_sources", disabledSet).apply()
    }

    /** Ayarlar kapatılırken yalnızca süreç içi çözümleme önbelleklerini bırakır. */
    fun clearRuntimeCaches() {
        cachedPatronChannels = null
        cachedPatronBaseUrl = null
    }

    fun isAutoDetectedTvOrLowRam(): Boolean {
        return runCatching {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val isLowRam = activityManager?.isLowRamDevice == true
            isTv || isLowRam
        }.getOrDefault(false)
    }

    fun isTvBoxMode(): Boolean {
        if (!prefs.contains("tv_box_mode")) {
            return isAutoDetectedTvOrLowRam()
        }
        return prefs.getBoolean("tv_box_mode", false)
    }

    fun setTvBoxMode(enabled: Boolean) {
        prefs.edit().putBoolean("tv_box_mode", enabled).apply()
    }

    suspend fun checkHealth(): HealthStatus = withContext(Dispatchers.IO) {
        val results = workers.map { worker ->
            async {
                worker.id to (runCatching { worker.checkOnline() }.getOrDefault(false))
            }
        }.awaitAll().toMap()
        val online = results.values.count { it }
        HealthStatus(workers.size, online, results)
    }

    suspend fun refreshWebDomains(): String = withContext(Dispatchers.IO) {
        val tasks = listOf(
            async { runCatching { selcukResolver.resolve(force = true); 1 }.getOrDefault(0) },
            async { runCatching { taraftariumResolver.resolve(force = true); 1 }.getOrDefault(0) },
            async { runCatching { inatResolver.resolve(force = true); 1 }.getOrDefault(0) },
            async { runCatching { ardaResolver.resolve(force = true); 1 }.getOrDefault(0) },
            async { runCatching { mahsunResolver.resolve(force = true); 1 }.getOrDefault(0) },
            async { runCatching { kralsporResolver.resolve(force = true); 1 }.getOrDefault(0) },
            async { runCatching { crexResolver.resolve(force = true); 1 }.getOrDefault(0) }
        ) + turkspor.shared.SourceSpec.all.values.map { spec ->
            async { runCatching { turkspor.shared.DomainResolver(prefs, spec).resolve(force = true); 1 }.getOrDefault(0) }
        }
        val refreshedCount = tasks.awaitAll().sum()
        "$refreshedCount web kaynağının güncel adresleri yenilendi."
    }

    suspend fun fetchAlternativeLinks(
        channel: WioChannel,
        callback: (ExtractorLink) -> Unit
    ): Boolean = coroutineScope {
        migrateLegacySourceProfile()
        val tvMode = isTvBoxMode()
        val activeWorkers = workers.filter { isSourceEnabled(it.id) }

        val foundCount = AtomicInteger(0)

        // Özel M3U listelerinden eşleşen yayınları öncelikle bağla
        runCatching {
            val customStreams = customListManager.getStreamsForChannel(channel)
            customStreams.forEach { stream ->
                foundCount.incrementAndGet()
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
        }

        if (!tvMode) {
            // ==========================================
            // NORMAL MOD (TV Box Modu KAPALI):
            // Kullanıcı kuralı: "tvbox modu kapalıysa zaten aktif tüm sağlayıcılardan tarama gerçekleşecek."
            // Tüm aktif kaynaklar eksiksiz taranır ve yüklenir.
            // Erken durdurma veya link kotası YOKTUR.
            // ==========================================
            val normalSemaphore = Semaphore(6)

            suspend fun runWorkerNormal(worker: SourceWorker, timeoutMs: Long) {
                try {
                    normalSemaphore.withPermit {
                        withTimeoutOrNull(timeoutMs) {
                            worker.fetchLinks(channel) { link ->
                                foundCount.incrementAndGet()
                                synchronized(callback) {
                                    callback(link)
                                }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    currentCoroutineContext().ensureActive()
                } catch (_: Exception) { }
            }

            // 1. Aşama: En öncelikli Tier 1 sağlayıcıları (DEFAULT_ENABLED_SOURCES: BeyazElma, Domino, İnat TV, KralSpor, Betmatik)
            val tier1 = activeWorkers.filter { it.id in DEFAULT_ENABLED_SOURCES }
            val tier2 = activeWorkers.filter { it.id !in DEFAULT_ENABLED_SOURCES }

            val tier1Jobs = tier1.map { worker ->
                async(Dispatchers.IO) { runWorkerNormal(worker, 6000L) }
            }
            tier1Jobs.awaitAll()

            // 2. Aşama: Kalan tüm aktif sağlayıcılar eksiksiz taranır
            val tier2Jobs = tier2.map { worker ->
                async(Dispatchers.IO) { runWorkerNormal(worker, 8000L) }
            }
            tier2Jobs.awaitAll()

            return@coroutineScope foundCount.get() > 0
        }

        // ==========================================
        // TV BOX MODU (Düşük RAM / TV Box Koruma Modu):
        // Kullanıcı kuralı: "her zaman 5 tane sağlayıcıyı kullanacak açık sağlayıcılardan.
        // bu yazdıklarım eklenti ilk yüklendiğinde aktif olarak gelsin.
        // başka eklentiler aktifleştirilirse onlar da eklensin ve içlerinden 5 tane rastgele seçilsin eğer tvbox modu açıksa."
        // Concurrency: 2 (Bellek taşmasını engellemek için)
        // ==========================================
        val targetWorkers = if (activeWorkers.size <= 5) {
            activeWorkers
        } else {
            activeWorkers.shuffled().take(5)
        }

        val tvSemaphore = Semaphore(2)
        val tvTargetQuota = 6

        suspend fun runWorkerTv(worker: SourceWorker, timeoutMs: Long) {
            if (foundCount.get() >= tvTargetQuota) return
            try {
                tvSemaphore.withPermit {
                    if (foundCount.get() >= tvTargetQuota) return@withPermit
                    withTimeoutOrNull(timeoutMs) {
                        worker.fetchLinks(channel) { link ->
                            val count = foundCount.incrementAndGet()
                            synchronized(callback) {
                                callback(link)
                            }
                            if (count >= tvTargetQuota) {
                                throw CancellationException("TV Box quota reached")
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                if (foundCount.get() >= tvTargetQuota) {
                    currentCoroutineContext().cancelChildren()
                } else {
                    currentCoroutineContext().ensureActive()
                }
            } catch (_: Exception) { }
        }

        val tvJobs = targetWorkers.map { worker ->
            async(Dispatchers.IO) { runWorkerTv(worker, 5000L) }
        }
        tvJobs.awaitAll()

        return@coroutineScope foundCount.get() > 0
    }
}
