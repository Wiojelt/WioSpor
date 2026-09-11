package wiospor

import java.text.Normalizer
import java.util.Locale

data class WioChannel(
    val id: String,
    val name: String,
    val group: String,
    val standardTitle: String,
    val aliases: List<String>,
    val logo: String = ""
)

object WioChannels {
    const val GROUP_MOR = "🟣 Mor Spor"
    const val GROUP_YESIL = "🟢 Yeşil Spor"
    const val GROUP_TURUNCU = "🟠 Turuncu Spor"
    const val GROUP_SARI = "🟡 Sarı Spor"
    const val GROUP_MAVI = "🔵 Mavi Spor"
    const val GROUP_YILDIZ = "⭐ Yıldız Spor"
    const val GROUP_ULUSAL = "📺 Ulusal Kanallar"
    const val GROUP_WORLD_TNT = "🌐 World TNT"
    const val GROUP_WORLD_SKY = "🌐 World Sky"
    const val GROUP_WORLD_DAZN = "🌐 World DAZN"
    const val GROUP_WORLD_CANAL = "🌐 World Canal+"
    const val GROUP_WORLD_ESPN = "🌐 World ESPN"
    const val GROUP_WORLD_OTHER = "🌐 World Diğer"

    val GROUPS = listOf(
        GROUP_MOR,
        GROUP_YESIL,
        GROUP_TURUNCU,
        GROUP_SARI,
        GROUP_MAVI,
        GROUP_YILDIZ,
        GROUP_ULUSAL,
        GROUP_WORLD_TNT,
        GROUP_WORLD_SKY,
        GROUP_WORLD_DAZN,
        GROUP_WORLD_CANAL,
        GROUP_WORLD_ESPN,
        GROUP_WORLD_OTHER
    )

    private const val BASE_LOGO = "https://raw.githubusercontent.com/Wiojelt/WioSpor/main/assets/banners/"

    val all: List<WioChannel> = listOf(
        // 🟣 Mor Spor (beIN Sports)
        WioChannel("mor_1", "🟣 Mor Spor 1", GROUP_MOR, "beIN Sports 1", listOf("beinsports1", "bein1", "beinsport1", "bein sports 1", "mor spor 1", "mor 1", "bein 1", "b1", "bs1", "beinsport 1", "spor 1", "sport 1"), "${BASE_LOGO}mor_1.png"),
        WioChannel("mor_2", "🟣 Mor Spor 2", GROUP_MOR, "beIN Sports 2", listOf("beinsports2", "bein2", "beinsport2", "bein sports 2", "mor spor 2", "mor 2", "bein 2", "b2", "bs2", "beinsport 2", "spor 2", "sport 2"), "${BASE_LOGO}mor_2.png"),
        WioChannel("mor_3", "🟣 Mor Spor 3", GROUP_MOR, "beIN Sports 3", listOf("beinsports3", "bein3", "beinsport3", "bein sports 3", "mor spor 3", "mor 3", "bein 3", "b3", "bs3", "beinsport 3", "spor 3", "sport 3"), "${BASE_LOGO}mor_3.png"),
        WioChannel("mor_4", "🟣 Mor Spor 4", GROUP_MOR, "beIN Sports 4", listOf("beinsports4", "bein4", "beinsport4", "bein sports 4", "mor spor 4", "mor 4", "bein 4", "b4", "bs4", "beinsport 4", "spor 4", "sport 4"), "${BASE_LOGO}mor_4.png"),
        WioChannel("mor_5", "🟣 Mor Spor 5", GROUP_MOR, "beIN Sports 5", listOf("beinsports5", "bein5", "beinsport5", "bein sports 5", "mor spor 5", "mor 5", "bein 5", "b5", "bs5", "beinsport 5", "spor 5", "sport 5"), "${BASE_LOGO}mor_5.png"),
        WioChannel("mor_max_1", "🟣 Mor Spor Max 1", GROUP_MOR, "beIN Sports Max 1", listOf("beinsportsmax1", "beinmax1", "beinsportmax1", "bein max 1", "mor max 1", "bein max1", "spor max 1", "sport max 1"), "${BASE_LOGO}mor_max_1.png"),
        WioChannel("mor_max_2", "🟣 Mor Spor Max 2", GROUP_MOR, "beIN Sports Max 2", listOf("beinsportsmax2", "beinmax2", "beinsportmax2", "bein max 2", "mor max 2", "bein max2", "spor max 2", "sport max 2"), "${BASE_LOGO}mor_max_2.png"),
        WioChannel("mor_haber", "🟣 Mor Spor Haber", GROUP_MOR, "beIN Sports Haber", listOf("beinsportshaber", "beinhaber", "beinsport haber", "mor spor haber", "mor haber", "bein haber", "spor haber", "sport haber"), "${BASE_LOGO}mor_haber.png"),

        // 🟢 Yeşil Spor (tabii Spor 1..6)
        WioChannel("yesil_1", "🟢 Yeşil Spor 1", GROUP_YESIL, "tabii Spor 1", listOf("tabiispor1", "tabiispor", "tabii1", "tabii spor 1", "yesil spor 1", "yesil 1", "tabi spor 1", "tabispor1", "tabi1"), "${BASE_LOGO}yesil_1.png"),
        WioChannel("yesil_2", "🟢 Yeşil Spor 2", GROUP_YESIL, "tabii Spor 2", listOf("tabiispor2", "tabii2", "tabii spor 2", "yesil spor 2", "yesil 2", "tabi spor 2", "tabispor2", "tabi2"), "${BASE_LOGO}yesil_2.png"),
        WioChannel("yesil_3", "🟢 Yeşil Spor 3", GROUP_YESIL, "tabii Spor 3", listOf("tabiispor3", "tabii3", "tabii spor 3", "yesil spor 3", "yesil 3", "tabi spor 3", "tabispor3", "tabi3"), "${BASE_LOGO}yesil_3.png"),
        WioChannel("yesil_4", "🟢 Yeşil Spor 4", GROUP_YESIL, "tabii Spor 4", listOf("tabiispor4", "tabii4", "tabii spor 4", "yesil spor 4", "yesil 4", "tabi spor 4", "tabispor4", "tabi4"), "${BASE_LOGO}yesil_4.png"),
        WioChannel("yesil_5", "🟢 Yeşil Spor 5", GROUP_YESIL, "tabii Spor 5", listOf("tabiispor5", "tabii5", "tabii spor 5", "yesil spor 5", "yesil 5", "tabi spor 5", "tabispor5", "tabi5"), "${BASE_LOGO}yesil_5.png"),
        WioChannel("yesil_6", "🟢 Yeşil Spor 6", GROUP_YESIL, "tabii Spor 6", listOf("tabiispor6", "tabii6", "tabii spor 6", "yesil spor 6", "yesil 6", "tabi spor 6", "tabispor6", "tabi6"), "${BASE_LOGO}yesil_6.png"),

        // 🟠 Turuncu Spor (Tivibu Spor 1..4)
        WioChannel("turuncu_1", "🟠 Turuncu Spor 1", GROUP_TURUNCU, "Tivibu Spor 1", listOf("tivibuspor", "tivibuspor1", "tivibu1", "tivibu spor 1", "turuncu spor 1", "turuncu 1"), "${BASE_LOGO}turuncu_1.png"),
        WioChannel("turuncu_2", "🟠 Turuncu Spor 2", GROUP_TURUNCU, "Tivibu Spor 2", listOf("tivibuspor2", "tivibu2", "tivibu spor 2", "turuncu spor 2", "turuncu 2"), "${BASE_LOGO}turuncu_2.png"),
        WioChannel("turuncu_3", "🟠 Turuncu Spor 3", GROUP_TURUNCU, "Tivibu Spor 3", listOf("tivibuspor3", "tivibu3", "tivibu spor 3", "turuncu spor 3", "turuncu 3"), "${BASE_LOGO}turuncu_3.png"),
        WioChannel("turuncu_4", "🟠 Turuncu Spor 4", GROUP_TURUNCU, "Tivibu Spor 4", listOf("tivibuspor4", "tivibu4", "tivibu spor 4", "turuncu spor 4", "turuncu 4"), "${BASE_LOGO}turuncu_4.png"),

        // 🟡 Sarı Spor (Exxen Spor 1..8)
        WioChannel("sari_1", "🟡 Sarı Spor 1", GROUP_SARI, "Exxen Spor 1", listOf("exxenspor1", "exxen1", "exxenspor", "exxen 1", "exxen sports 1", "sari spor 1", "sari 1"), "${BASE_LOGO}sari_1.png"),
        WioChannel("sari_2", "🟡 Sarı Spor 2", GROUP_SARI, "Exxen Spor 2", listOf("exxenspor2", "exxen2", "exxen 2", "exxen sports 2", "sari spor 2", "sari 2"), "${BASE_LOGO}sari_2.png"),
        WioChannel("sari_3", "🟡 Sarı Spor 3", GROUP_SARI, "Exxen Spor 3", listOf("exxenspor3", "exxen3", "exxen 3", "exxen sports 3", "sari spor 3", "sari 3"), "${BASE_LOGO}sari_3.png"),
        WioChannel("sari_4", "🟡 Sarı Spor 4", GROUP_SARI, "Exxen Spor 4", listOf("exxenspor4", "exxen4", "exxen 4", "exxen sports 4", "sari spor 4", "sari 4"), "${BASE_LOGO}sari_4.png"),
        WioChannel("sari_5", "🟡 Sarı Spor 5", GROUP_SARI, "Exxen Spor 5", listOf("exxenspor5", "exxen5", "exxen 5", "exxen sports 5", "sari spor 5", "sari 5"), "${BASE_LOGO}sari_5.png"),
        WioChannel("sari_6", "🟡 Sarı Spor 6", GROUP_SARI, "Exxen Spor 6", listOf("exxenspor6", "exxen6", "exxen 6", "exxen sports 6", "sari spor 6", "sari 6"), "${BASE_LOGO}sari_6.png"),
        WioChannel("sari_7", "🟡 Sarı Spor 7", GROUP_SARI, "Exxen Spor 7", listOf("exxenspor7", "exxen7", "exxen 7", "exxen sports 7", "sari spor 7", "sari 7"), "${BASE_LOGO}sari_7.png"),
        WioChannel("sari_8", "🟡 Sarı Spor 8", GROUP_SARI, "Exxen Spor 8", listOf("exxenspor8", "exxen8", "exxen 8", "exxen sports 8", "sari spor 8", "sari 8"), "${BASE_LOGO}sari_8.png"),

        // 🔵 Mavi Spor (S Sport 1..2 & Plus 1..5)
        WioChannel("mavi_1", "🔵 Mavi Spor 1", GROUP_MAVI, "S Sport", listOf("ssport", "ssport1", "s sport 1", "s sport", "mavi spor 1", "mavi 1", "ssports", "s sports"), "${BASE_LOGO}mavi_1.png"),
        WioChannel("mavi_2", "🔵 Mavi Spor 2", GROUP_MAVI, "S Sport 2", listOf("ssport2", "s sport 2", "mavi spor 2", "mavi 2", "ssports 2", "s sports 2", "ssports2"), "${BASE_LOGO}mavi_2.png"),
        WioChannel("mavi_plus_1", "🔵 Mavi Spor Plus 1", GROUP_MAVI, "S Sport Plus 1", listOf("ssportplus1", "splus1", "ssportplus", "splus", "s+1", "s+", "mavi plus 1", "ssport plus"), "${BASE_LOGO}mavi_plus_1.png"),
        WioChannel("mavi_plus_2", "🔵 Mavi Spor Plus 2", GROUP_MAVI, "S Sport Plus 2", listOf("ssportplus2", "splus2", "s+2", "mavi plus 2"), "${BASE_LOGO}mavi_plus_2.png"),
        WioChannel("mavi_plus_3", "🔵 Mavi Spor Plus 3", GROUP_MAVI, "S Sport Plus 3", listOf("ssportplus3", "splus3", "s+3", "mavi plus 3"), "${BASE_LOGO}mavi_plus_3.png"),
        WioChannel("mavi_plus_4", "🔵 Mavi Spor Plus 4", GROUP_MAVI, "S Sport Plus 4", listOf("ssportplus4", "splus4", "s+4", "mavi plus 4"), "${BASE_LOGO}mavi_plus_4.png"),
        WioChannel("mavi_plus_5", "🔵 Mavi Spor Plus 5", GROUP_MAVI, "S Sport Plus 5", listOf("ssportplus5", "splus5", "s+5", "mavi plus 5"), "${BASE_LOGO}mavi_plus_5.png"),

        // ⭐ Yıldız Spor (Eurosport 1..2 & Spor Smart 1..2)
        WioChannel("yildiz_euro_1", "⭐ Yıldız Spor 1", GROUP_YILDIZ, "Eurosport 1", listOf("eurosport1", "eurosport 1", "eurosport", "euro sport 1", "euro sports 1", "eurosports 1", "euro sport", "eu1", "es1", "yildiz spor 1", "yildiz 1"), "${BASE_LOGO}yildiz_euro_1.png"),
        WioChannel("yildiz_euro_2", "⭐ Yıldız Spor 2", GROUP_YILDIZ, "Eurosport 2", listOf("eurosport2", "eurosport 2", "euro sport 2", "euro sports 2", "eurosports 2", "euro sport 2", "eu2", "es2", "yildiz spor 2", "yildiz 2"), "${BASE_LOGO}yildiz_euro_2.png"),
        WioChannel("yildiz_smart_1", "⭐ Yıldız Smart 1", GROUP_YILDIZ, "Spor Smart", listOf("sporsmart", "sporsmart1", "smartspor", "smartspor1", "spor smart 1", "smart spor 1", "smart spor", "smart1", "sm1", "yildiz smart 1"), "${BASE_LOGO}yildiz_smart_1.png"),
        WioChannel("yildiz_smart_2", "⭐ Yıldız Smart 2", GROUP_YILDIZ, "Spor Smart 2", listOf("sporsmart2", "smartspor2", "spor smart 2", "smart spor 2", "smart2", "sm2", "yildiz smart 2"), "${BASE_LOGO}yildiz_smart_2.png"),

        // 📺 Ulusal Kanallar
        WioChannel("ulusal_trt_spor", "📺 TRT Spor", GROUP_ULUSAL, "TRT Spor", listOf("trtspor", "trtspor1", "trt spor"), "${BASE_LOGO}ulusal_trt_spor.png"),
        WioChannel("ulusal_trt_yildiz", "📺 TRT Spor Yıldız", GROUP_ULUSAL, "TRT Spor Yıldız", listOf("trtsporyildiz", "trtspor2", "trt spor yildiz"), "${BASE_LOGO}ulusal_trt_yildiz.png"),
        WioChannel("ulusal_aspor", "📺 A Spor", GROUP_ULUSAL, "A Spor", listOf("aspor", "a spor"), "${BASE_LOGO}ulusal_aspor.png"),
        WioChannel("ulusal_htspor", "📺 HT Spor", GROUP_ULUSAL, "HT Spor", listOf("htspor", "ht spor"), "${BASE_LOGO}ulusal_htspor.png"),
        WioChannel("ulusal_sportstv", "📺 Sports TV", GROUP_ULUSAL, "Sports TV", listOf("sportstv", "sports tv"), "${BASE_LOGO}ulusal_sportstv.png"),
        WioChannel("ulusal_fbtv", "📺 FB TV", GROUP_ULUSAL, "FB TV", listOf("fbtv", "fenerbahce tv"), "${BASE_LOGO}ulusal_fbtv.png"),
        WioChannel("ulusal_gstv", "📺 GS TV", GROUP_ULUSAL, "GS TV", listOf("gstv", "galatasaray tv"), "${BASE_LOGO}ulusal_gstv.png"),
        WioChannel("ulusal_trt1", "📺 TRT 1", GROUP_ULUSAL, "TRT 1", listOf("trt1", "trt 1"), "${BASE_LOGO}ulusal_trt1.png"),
        WioChannel("ulusal_atv", "📺 ATV", GROUP_ULUSAL, "ATV", listOf("atv"), "${BASE_LOGO}ulusal_atv.png"),
        WioChannel("ulusal_kanald", "📺 Kanal D", GROUP_ULUSAL, "Kanal D", listOf("kanald", "kanal d"), "${BASE_LOGO}ulusal_kanald.png"),
        WioChannel("ulusal_show", "📺 Show TV", GROUP_ULUSAL, "Show TV", listOf("showtv", "show tv", "show"), "${BASE_LOGO}ulusal_show.png"),
        WioChannel("ulusal_star", "📺 Star TV", GROUP_ULUSAL, "Star TV", listOf("startv", "star tv", "star"), "${BASE_LOGO}ulusal_star.png"),
        WioChannel("ulusal_tv8", "📺 TV8", GROUP_ULUSAL, "TV8", listOf("tv8"), "${BASE_LOGO}ulusal_tv8.png"),
        WioChannel("ulusal_tv85", "📺 TV8.5", GROUP_ULUSAL, "TV8.5", listOf("tv85", "tv8bucuk", "tv 8.5", "tv8,5"), "${BASE_LOGO}ulusal_tv85.png"),
        WioChannel("ulusal_now", "📺 NOW TV", GROUP_ULUSAL, "NOW TV", listOf("nowtv", "now tv", "now", "fox", "foxtv"), "${BASE_LOGO}ulusal_now.png"),
        WioChannel("ulusal_kanal7", "📺 Kanal 7", GROUP_ULUSAL, "Kanal 7", listOf("kanal7", "kanal 7"), "${BASE_LOGO}ulusal_kanal7.png"),
        WioChannel("ulusal_beyaz", "📺 Beyaz TV", GROUP_ULUSAL, "Beyaz TV", listOf("beyaztv", "beyaz tv"), "${BASE_LOGO}ulusal_beyaz.png"),
        WioChannel("ulusal_a2", "📺 A2", GROUP_ULUSAL, "A2", listOf("a2", "a2tv"), "${BASE_LOGO}ulusal_a2.png"),
        WioChannel("ulusal_haberturk", "📺 HaberTürk", GROUP_ULUSAL, "HaberTürk", listOf("haberturk", "haber turk"), "${BASE_LOGO}ulusal_haberturk.png"),
        WioChannel("ulusal_ntv", "📺 NTV", GROUP_ULUSAL, "NTV", listOf("ntv"), "${BASE_LOGO}ulusal_ntv.png"),
        WioChannel("ulusal_cnnturk", "📺 CNN Türk", GROUP_ULUSAL, "CNN Türk", listOf("cnnturk", "cnn turk"), "${BASE_LOGO}ulusal_cnnturk.png"),
        WioChannel("ulusal_sozcu", "📺 Sözcü TV", GROUP_ULUSAL, "Sözcü TV", listOf("sozcutv", "sozcu tv", "sozcu"), "${BASE_LOGO}ulusal_sozcu.png"),
        WioChannel("ulusal_trthaber", "📺 TRT Haber", GROUP_ULUSAL, "TRT Haber", listOf("trthaber", "trt haber"), "${BASE_LOGO}ulusal_trthaber.png"),

        // 🌐 World TNT
        WioChannel("world_tnt_1", "🌐 World TNT Sports 1", GROUP_WORLD_TNT, "TNT Sports 1", listOf("tntsports1", "tnt1", "tnt sports 1", "bt sport 1"), "${BASE_LOGO}world_tnt_1.png"),
        WioChannel("world_tnt_2", "🌐 World TNT Sports 2", GROUP_WORLD_TNT, "TNT Sports 2", listOf("tntsports2", "tnt2", "tnt sports 2", "bt sport 2"), "${BASE_LOGO}world_tnt_2.png"),
        WioChannel("world_tnt_3", "🌐 World TNT Sports 3", GROUP_WORLD_TNT, "TNT Sports 3", listOf("tntsports3", "tnt3", "tnt sports 3", "bt sport 3"), "${BASE_LOGO}world_tnt_3.png"),
        WioChannel("world_tnt_4", "🌐 World TNT Sports 4", GROUP_WORLD_TNT, "TNT Sports 4", listOf("tntsports4", "tnt4", "tnt sports 4", "bt sport 4"), "${BASE_LOGO}world_tnt_4.png"),

        // 🌐 World Sky
        WioChannel("world_sky_calcio", "🌐 World Sky Sport Calcio", GROUP_WORLD_SKY, "Sky Sport Calcio", listOf("skycalcio", "sky sport calcio"), "${BASE_LOGO}world_sky_calcio.png"),
        WioChannel("world_sky_austria_1", "🌐 World Sky Sport Austria 1", GROUP_WORLD_SKY, "Sky Sport Austria 1", listOf("skysportaustria1", "sky sport austria 1"), "${BASE_LOGO}world_sky_austria_1.png"),
        WioChannel("world_sky_austria_2", "🌐 World Sky Sport Austria 2", GROUP_WORLD_SKY, "Sky Sport Austria 2", listOf("skysportaustria2", "sky sport austria 2"), "${BASE_LOGO}world_sky_austria_2.png"),
        WioChannel("world_sky_uno", "🌐 World Sky Sport Uno", GROUP_WORLD_SKY, "Sky Sport Uno", listOf("skyuno", "sky sport uno"), "${BASE_LOGO}world_sky_uno.png"),

        // 🌐 World DAZN
        WioChannel("world_dazn_1", "🌐 World DAZN 1", GROUP_WORLD_DAZN, "DAZN 1", listOf("dazn1", "dazn 1"), "${BASE_LOGO}world_dazn_1.png"),
        WioChannel("world_dazn_2", "🌐 World DAZN 2", GROUP_WORLD_DAZN, "DAZN 2", listOf("dazn2", "dazn 2"), "${BASE_LOGO}world_dazn_2.png"),

        // 🌐 World Canal+
        WioChannel("world_canal_sport", "🌐 World Canal+ Sport", GROUP_WORLD_CANAL, "Canal+ Sport", listOf("canalsport", "canalplus sport", "canal+ sport"), "${BASE_LOGO}world_canal_sport.png"),
        WioChannel("world_canal_foot", "🌐 World Canal+ Foot", GROUP_WORLD_CANAL, "Canal+ Foot", listOf("canalfoot", "canalplus foot", "canal+ foot"), "${BASE_LOGO}world_canal_foot.png"),
        WioChannel("world_canal_live_1", "🌐 World Canal+ Live 1", GROUP_WORLD_CANAL, "Canal+ Live 1", listOf("canallive1", "canal+ live 1"), "${BASE_LOGO}world_canal_live_1.png"),
        WioChannel("world_canal_extra_1", "🌐 World Canal+ Extra 1", GROUP_WORLD_CANAL, "Canal+ Extra 1", listOf("canalextra1", "canal+ extra 1"), "${BASE_LOGO}world_canal_extra_1.png"),

        // 🌐 World ESPN
        WioChannel("world_espn_1", "🌐 World ESPN 1", GROUP_WORLD_ESPN, "ESPN 1", listOf("espn1", "espn 1", "espn"), "${BASE_LOGO}world_espn_1.png"),
        WioChannel("world_espn_2", "🌐 World ESPN 2", GROUP_WORLD_ESPN, "ESPN 2", listOf("espn2", "espn 2"), "${BASE_LOGO}world_espn_2.png"),

        // 🌐 World Diğer
        WioChannel("world_ziggo_1", "🌐 World Ziggo Sport 1", GROUP_WORLD_OTHER, "Ziggo Sport 1", listOf("ziggosport1", "ziggo sport 1", "ziggo sport"), "${BASE_LOGO}world_ziggo_1.png"),
        WioChannel("world_ziggo_2", "🌐 World Ziggo Sport 2", GROUP_WORLD_OTHER, "Ziggo Sport 2", listOf("ziggosport2", "ziggo sport 2"), "${BASE_LOGO}world_ziggo_2.png"),
        WioChannel("world_movistar_liga", "🌐 World M+ Liga de Campeones", GROUP_WORLD_OTHER, "M+ Liga de Campeones", listOf("movistarligadecampeones", "liga de campeones", "m+ liga"), "${BASE_LOGO}world_movistar_liga.png"),
        WioChannel("world_sporttv_1", "🌐 World Sport TV 1", GROUP_WORLD_OTHER, "Sport TV 1", listOf("sporttv1", "sport tv 1"), "${BASE_LOGO}world_sporttv_1.png"),
        WioChannel("world_digisport_1", "🌐 World Digi Sport 1", GROUP_WORLD_OTHER, "Digi Sport 1", listOf("digisport1", "digi sport 1"), "${BASE_LOGO}world_digisport_1.png"),
        WioChannel("world_cosmote_1", "🌐 World Cosmote Sport 1", GROUP_WORLD_OTHER, "Cosmote Sport 1", listOf("cosmotesport1", "cosmote sport 1"), "${BASE_LOGO}world_cosmote_1.png"),
        WioChannel("world_arena_1", "🌐 World Arena Sport 1", GROUP_WORLD_OTHER, "Arena Sport 1", listOf("arenasport1", "arena sport 1"), "${BASE_LOGO}world_arena_1.png")
    )

    private val channelMap = all.associateBy { it.id }

    fun byId(id: String): WioChannel? = channelMap[id]

    fun normalize(raw: String): String {
        var s = raw.lowercase(Locale.ROOT).trim()
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ş', 's')
            .replace('ö', 'o')
            .replace('ç', 'c')
            .replace('İ', 'i')
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return s.replace(Regex("[^a-z0-9]"), "")
    }

    private fun cleanCandidate(raw: String): String {
        var s = raw.lowercase(Locale.ROOT).trim()
            // Strip tags like [TR], [GB], [MENA], [FHD], [HD], (1080p), etc.
            .replace(Regex("""\[[^\]]*]|\([^)]*\)"""), " ")
            // Strip common prefixes
            .replace(Regex("""^(?:tr|de|en|ru|az|fr|es|it|nl|pt|gb|uk|us|vip|gold|net|atom|mahsun|inadina|pasizle|selcuk|selçuk|canli|yayin|andro|deathless|soner bozkurt yerli kanallar)[:|\-\s_]+"""), " ")
            .replace(Regex("""\b(?:7/24|24/7|724|247)\b"""), " ")
            .replace(Regex("""\b(?:1080p|720p|480p|360p|fhd|uhd|hd|sd|hevc|4k|2k|50fps|60fps)\b"""), " ")
            .replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u')
            .replace('ş', 's').replace('ö', 'o').replace('ç', 'c')
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        s = s.replace(Regex("""\bsports\b"""), "sport")
        return s.replace(Regex("[^a-z0-9]+"), " ").trim()
    }

    fun noSpaces(s: String): String = s.replace(Regex("[^a-z0-9]"), "")

    fun matches(channel: WioChannel, candidateTitle: String, candidateId: String = ""): Boolean {
        val normTitle = normalize(candidateTitle)
        val normId = normalize(candidateId)
        val normStd = normalize(channel.standardTitle)

        // Exact match with standard title
        if (normTitle == normStd || normId == normStd) return true

        val cleanedCandidate = cleanCandidate(candidateTitle)
        val normCleaned = normalize(cleanedCandidate)

        if (normCleaned == normStd) return true

        // Keyword guard for special designations: max, haber, plus, yildiz
        val stdLower = channel.standardTitle.lowercase(Locale.ROOT)
        val isStdMax = "max" in stdLower
        val isStdHaber = "haber" in stdLower
        val isStdPlus = "plus" in stdLower || "+" in stdLower
        val isStdYildiz = "yildiz" in stdLower || "yıldız" in stdLower

        val candLower = candidateTitle.lowercase(Locale.ROOT)
        val isCandMax = "max" in candLower
        val isCandHaber = "haber" in candLower
        val isCandPlus = "plus" in candLower || "+" in candLower
        val isCandYildiz = "yildiz" in candLower || "yıldız" in candLower

        if (isStdMax != isCandMax) return false
        if (isStdHaber != isCandHaber) return false
        if (isStdPlus != isCandPlus) return false
        if (isStdYildiz != isCandYildiz) return false

        // Extract numbers from both
        val stdNumber = Regex("""\b([0-9]+)\b""").findAll(channel.standardTitle).map { it.groupValues[1] }.lastOrNull()
        val candNumber = Regex("""\b([0-9]+)\b""").findAll(cleanedCandidate).map { it.groupValues[1] }.lastOrNull()

        if (stdNumber != null) {
            // Channel has a number (e.g. 1 in beIN 1). Candidate MUST have the same number!
            if (candNumber != null && candNumber != stdNumber) return false
            if (candNumber == null && !normTitle.endsWith(stdNumber)) {
                if (!Regex("""${stdNumber}(?:fhd|hd|sd|hevc|4k|p|$)""").containsMatchIn(candLower)) {
                    return false
                }
            }
        } else {
            // Channel has NO number (e.g. S Sport, beIN Haber, TRT Spor). Candidate should not have an unrelated number.
            if (candNumber != null && candNumber !in listOf("1", "24", "7")) return false
        }

        // Full check context combines title, cleaned title, and id
        val fullCand = "$normTitle $normCleaned $normId"

        // Strict Brand conflict check
        val isEurosport = channel.standardTitle.contains("Eurosport", ignoreCase = true)
        val isBein = channel.standardTitle.contains("beIN", ignoreCase = true)
        val isTabii = channel.standardTitle.contains("tabii", ignoreCase = true)
        val isTivibu = channel.standardTitle.contains("Tivibu", ignoreCase = true)
        val isExxen = channel.standardTitle.contains("Exxen", ignoreCase = true)
        val isSSport = channel.standardTitle.contains("S Sport", ignoreCase = true)
        val isSmart = channel.standardTitle.contains("Smart", ignoreCase = true)

        // Eurosport channel MUST have euro/eu/es token and NO other brand
        if (isEurosport) {
            val hasEuro = listOf("euro", "eu1", "eu2", "es1", "es2").any { it in fullCand }
            if (!hasEuro) return false
            if (listOf("tivibu", "tabii", "tabi", "exxen", "bein", "smart", "ssport", "s sport", "trt").any { it in fullCand }) return false
        }

        if (isBein && listOf("tivibu", "tabii", "tabi", "exxen", "ssport", "s sport", "smart", "trt", "aspor", "htspor", "euro", "dazn", "sky", "tnt").any { it in fullCand }) {
            return false
        }
        if (isTabii && listOf("tivibu", "bein", "exxen", "ssport", "smart", "euro").any { it in fullCand }) {
            return false
        }
        if (isTivibu && listOf("tabii", "tabi", "bein", "exxen", "ssport", "smart", "euro").any { it in fullCand }) {
            return false
        }
        if (isExxen && listOf("tivibu", "tabii", "tabi", "bein", "ssport", "smart", "euro").any { it in fullCand }) {
            return false
        }
        if (isSSport && listOf("tivibu", "tabii", "tabi", "exxen", "bein", "smart", "euro").any { it in fullCand }) {
            return false
        }
        if (isSmart && listOf("tivibu", "tabii", "tabi", "exxen", "bein", "ssport", "euro").any { it in fullCand }) {
            return false
        }

        // Space-insensitive exact match
        val nsCleaned = noSpaces(normCleaned)
        val nsStd = noSpaces(normStd)
        val nsTitle = noSpaces(normTitle)
        val nsId = noSpaces(normId)
        if (nsCleaned == nsStd || nsTitle == nsStd || nsId == nsStd) return true

        // Check against aliases
        for (alias in channel.aliases) {
            val normAlias = normalize(alias)
            val nsAlias = noSpaces(normAlias)
            if (normTitle == normAlias || normId == normAlias || normCleaned == normAlias) return true
            if (nsCleaned == nsAlias || nsTitle == nsAlias || nsId == nsAlias) return true
            if (normAlias.length >= 4) {
                if (normTitle.contains(normAlias) || normCleaned.contains(normAlias) || normId.contains(normAlias)) {
                    return true
                }
            }
            if (nsAlias.length >= 4) {
                if (nsCleaned.contains(nsAlias) || nsTitle.contains(nsAlias) || nsId.contains(nsAlias)) {
                    return true
                }
            }
        }

        if (normStd.length >= 4 && (normTitle.contains(normStd) || normCleaned.contains(normStd))) {
            return true
        }

        return false
    }
}
