package wiospor

import android.app.ActivityManager
import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
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
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wiospor_source_prefs", Context.MODE_PRIVATE)

    data class HealthStatus(
        val totalCount: Int,
        val onlineCount: Int,
        val details: Map<String, Boolean>
    )

    private fun wrapLink(label: String, channel: WioChannel, link: ExtractorLink): ExtractorLink {
        val quality = if (link.quality in listOf(360, 480, 540, 576, 720, 1080, 1440, 2160)) " • ${link.quality}p" else ""
        return ExtractorLink(
            source = "WioSpor",
            name = "[$label] ${channel.name}$quality",
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

    val workers: List<SourceWorker> by lazy {
        val list = mutableListOf<SourceWorker>()

        // 1. BeyazElma (first priority)
        createSharedWorker("beyazelma", "BeyazElma")?.let { list.add(it) }

        // 2. SelçukSports
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

        // 2. Taraftarium24
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

        // 4. ArdaSpor
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

        // 5. MahsunSports
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

        // 6. KralSporHD
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

        // 7. Crex
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

        // 8. İnterSporTV
        createSharedWorker("intersportv", "İnterSporTV")?.let { list.add(it) }

        // 9. MaçKeyfi
        createSharedWorker("mackeyfi", "MaçKeyfi")?.let { list.add(it) }

        // 10. ZbahisTV
        createSharedWorker("zbahistv", "ZbahisTV")?.let { list.add(it) }

        // 11. BetmatikTV
        createSharedWorker("betmatiktv", "BetmatikTV")?.let { list.add(it) }

        // 12. İnat Box
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

        // 14. Domates TV
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

        // 15. Domino TV
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

        // 16..42. Aslan IPTV Listeleri (27 Adet Aktif Kaynak)
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
                                    async {
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

    fun isSourceEnabled(sourceId: String): Boolean {
        val disabledSet = prefs.getStringSet("disabled_sources", emptySet()) ?: emptySet()
        return sourceId !in disabledSet
    }

    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        val disabledSet = (prefs.getStringSet("disabled_sources", emptySet()) ?: emptySet()).toMutableSet()
        if (enabled) {
            disabledSet.remove(sourceId)
        } else {
            disabledSet.add(sourceId)
        }
        prefs.edit().putStringSet("disabled_sources", disabledSet).apply()
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
        val tvMode = isTvBoxMode()
        val maxConcurrency = if (tvMode) 2 else 3
        val targetLinkCount = if (tvMode) 6 else 10
        val semaphore = Semaphore(maxConcurrency)

        val activeWorkers = workers.filter { isSourceEnabled(it.id) }

        // Tier 1: 1080p IPTV sağlayıcılar — ultra hızlı açılış
        val tier1Ids = setOf("beyazelma", "domino", "domates")
        // Tier 2 Öncelikli: Hızlı ve en stabil popüler web kaynakları
        val tier2PriorityIds = setOf("selcuk", "taraftarium", "inat", "mahsun", "arda")
        // Tier 2 Diğerleri
        val tier2OtherIds = setOf("kralspor", "crex", "intersportv", "mackeyfi", "zbahistv", "betmatiktv", "inatbox")

        val tier1 = activeWorkers.filter { it.id in tier1Ids }
        val tier2Priority = activeWorkers.filter { it.id in tier2PriorityIds }
        val tier2Other = activeWorkers.filter { it.id in tier2OtherIds }
        val tier3 = activeWorkers.filter { it.id !in tier1Ids && it.id !in tier2PriorityIds && it.id !in tier2OtherIds }

        val foundCount = AtomicInteger(0)

        suspend fun runWorker(worker: SourceWorker, timeoutMs: Long) {
            if (foundCount.get() >= targetLinkCount) return
            try {
                semaphore.withPermit {
                    if (foundCount.get() >= targetLinkCount) return@withPermit
                    withTimeoutOrNull(timeoutMs) {
                        worker.fetchLinks(channel) { link ->
                            val count = foundCount.incrementAndGet()
                            synchronized(callback) {
                                callback(link)
                            }
                            if (count >= targetLinkCount) {
                                throw CancellationException("Target link quota reached")
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                if (foundCount.get() >= targetLinkCount) {
                    currentCoroutineContext().cancelChildren()
                } else {
                    currentCoroutineContext().ensureActive()
                }
            } catch (_: Exception) { }
        }

        // FAZ 1: Tier 1 (IPTV) ve Öncelikli Tier 2 (Web)
        // En kaliteli ve hızlı kaynaklar önden taranır
        val phase1Workers = tier1 + tier2Priority
        val phase1Jobs = phase1Workers.map { worker ->
            async(Dispatchers.IO) { runWorker(worker, if (tvMode) 5000L else 7000L) }
        }
        phase1Jobs.awaitAll()

        // Erken Durdurma (Short-circuit): İlk fazdan yeterli link bulunduysa (TV'de 3, normalde 5)
        // arka plandaki diğer 30+ kaynağı ve AslanTV listelerini HİÇ tarama!
        if (foundCount.get() >= (if (tvMode) 3 else 5)) {
            return@coroutineScope true
        }

        // FAZ 2: Kalan Tier 2 Web Kaynakları (Yalnızca ilk faz yetersizse)
        val phase2Jobs = tier2Other.map { worker ->
            async(Dispatchers.IO) { runWorker(worker, if (tvMode) 5000L else 7000L) }
        }
        phase2Jobs.awaitAll()

        if (foundCount.get() >= (if (tvMode) 2 else 4)) {
            return@coroutineScope true
        }

        // FAZ 3 (Yedek Kurtarıcı): Sadece yeterli link gelmediyse AslanTV listeleri devreye girer
        // TV modunda 27 listeyi birden taramak yerine en popüler/ilgili ilk 5 listeyi tarar
        val aslanCandidates = if (tvMode) tier3.take(5) else tier3
        val phase3Jobs = aslanCandidates.map { worker ->
            async(Dispatchers.IO) { runWorker(worker, 6000L) }
        }
        phase3Jobs.awaitAll()

        foundCount.get() > 0
    }
}
