package wiospor

import org.junit.Assert.*
import org.junit.Test

class WioSporTest {

    @Test
    fun testChannelGroupsExist() {
        assertEquals(8, WioChannels.GROUPS.size)
        assertEquals(84, WioChannels.all.size)

        val groupsPresent = WioChannels.all.map { it.group }.distinct()
        for (g in WioChannels.GROUPS) {
            assertTrue("Grup $g kanal içermeli", groupsPresent.contains(g))
        }
    }

    @Test
    fun testMorSporMatching() {
        val mor1 = WioChannels.byId("mor_1")
        assertNotNull(mor1)
        assertTrue(WioChannels.matches(mor1!!, "beIN Sports 1"))
        assertTrue(WioChannels.matches(mor1, "beIN Sports 1 HD"))
        assertTrue(WioChannels.matches(mor1, "selcukbeinsports1"))
        assertTrue(WioChannels.matches(mor1, "VIP: beIN 1"))
        assertTrue(WioChannels.matches(mor1, "[TR] beIN SPORTS 1 FHD"))

        // Must NOT match other channels
        assertFalse(WioChannels.matches(mor1, "beIN Sports 2"))
        assertFalse(WioChannels.matches(mor1, "beIN Sports Max 1"))
        assertFalse(WioChannels.matches(mor1, "beIN Sports Haber"))

        val morMax1 = WioChannels.byId("mor_max_1")
        assertNotNull(morMax1)
        assertTrue(WioChannels.matches(morMax1!!, "beIN Sports Max 1"))
        assertTrue(WioChannels.matches(morMax1, "beIN Max 1"))
        assertFalse(WioChannels.matches(morMax1, "beIN Sports 1"))
        assertFalse(WioChannels.matches(morMax1, "beIN Sports Max 2"))

        val morHaber = WioChannels.byId("mor_haber")
        assertNotNull(morHaber)
        assertTrue(WioChannels.matches(morHaber!!, "beIN Sports Haber"))
        assertFalse(WioChannels.matches(morHaber, "beIN Sports 1"))
    }

    @Test
    fun testYesilTuruncuSariMaviMatching() {
        val yesil1 = WioChannels.byId("yesil_1")
        assertNotNull(yesil1)
        assertTrue(WioChannels.matches(yesil1!!, "tabii Spor 1"))
        assertFalse(WioChannels.matches(yesil1, "tabii Spor 2"))

        val turuncu1 = WioChannels.byId("turuncu_1")
        assertNotNull(turuncu1)
        assertTrue(WioChannels.matches(turuncu1!!, "Tivibu Spor 1"))
        assertFalse(WioChannels.matches(turuncu1, "Tivibu Spor 2"))

        val sari1 = WioChannels.byId("sari_1")
        assertNotNull(sari1)
        assertTrue(WioChannels.matches(sari1!!, "Exxen Spor 1"))
        assertTrue(WioChannels.matches(sari1, "Exxen 1"))
        assertFalse(WioChannels.matches(sari1, "Exxen Spor 2"))

        val mavi1 = WioChannels.byId("mavi_1")
        assertNotNull(mavi1)
        assertTrue(WioChannels.matches(mavi1!!, "S Sport"))
        assertTrue(WioChannels.matches(mavi1, "S Sport 1"))
        assertFalse(WioChannels.matches(mavi1, "S Sport 2"))
        assertFalse(WioChannels.matches(mavi1, "S Sport Plus 1"))

        val maviPlus1 = WioChannels.byId("mavi_plus_1")
        assertNotNull(maviPlus1)
        assertTrue(WioChannels.matches(maviPlus1!!, "S Sport Plus 1"))
        assertFalse(WioChannels.matches(maviPlus1, "S Sport 1"))
        assertFalse(WioChannels.matches(maviPlus1, "S Sport Plus 2"))
    }

    @Test
    fun testYildizUlusalWorldMatching() {
        val euro1 = WioChannels.byId("yildiz_euro_1")
        assertNotNull(euro1)
        assertTrue(WioChannels.matches(euro1!!, "Eurosport 1"))
        assertFalse(WioChannels.matches(euro1, "Eurosport 2"))

        val smart1 = WioChannels.byId("yildiz_smart_1")
        assertNotNull(smart1)
        assertTrue(WioChannels.matches(smart1!!, "Spor Smart"))
        assertTrue(WioChannels.matches(smart1, "Smart Spor 1"))
        assertFalse(WioChannels.matches(smart1, "Spor Smart 2"))

        val trtSpor = WioChannels.byId("ulusal_trt_spor")
        assertNotNull(trtSpor)
        assertTrue(WioChannels.matches(trtSpor!!, "TRT Spor"))
        assertFalse(WioChannels.matches(trtSpor, "TRT Spor Yıldız"))
        assertFalse(WioChannels.matches(trtSpor, "TRT 1"))

        val aspor = WioChannels.byId("ulusal_aspor")
        assertNotNull(aspor)
        assertTrue(WioChannels.matches(aspor!!, "A Spor"))
        assertTrue(WioChannels.matches(aspor, "A Spor HD"))

        val tnt1 = WioChannels.byId("world_tnt_1")
        assertNotNull(tnt1)
        assertTrue(WioChannels.matches(tnt1!!, "TNT Sports 1"))
        assertTrue(WioChannels.matches(tnt1, "TNT Sports 1 [GB]"))
        assertFalse(WioChannels.matches(tnt1, "TNT Sports 2"))
    }

    @Test
    fun testCustomLogoUrls() {
        for (channel in WioChannels.all) {
            assertTrue(
                "Kanal ${channel.id} WioSpor logo URL içermeli",
                channel.logo.startsWith("https://raw.githubusercontent.com/Wiojelt/WioSpor/main/assets/channels/")
            )
            assertTrue(channel.logo.endsWith(".png"))
        }
    }

    @Test
    fun testDominoAndDomatesMatching() {
        val mor1 = WioChannels.byId("mor_1")!!
        assertTrue(WioChannels.matches(mor1, "Spor 1 -A"))
        assertTrue(WioChannels.matches(mor1, "Spor 1-B"))
        assertTrue(WioChannels.matches(mor1, "Sport 1"))
        assertFalse(WioChannels.matches(mor1, "Tivibu Spor 1"))
        assertFalse(WioChannels.matches(mor1, "Tabi Spor 1"))

        val yesil1 = WioChannels.byId("yesil_1")!!
        assertTrue(WioChannels.matches(yesil1, "Tabi Spor 1"))
        assertTrue(WioChannels.matches(yesil1, "Tabi Spor 1-A"))
        assertFalse(WioChannels.matches(yesil1, "Spor 1 -A"))

        val smart1 = WioChannels.byId("yildiz_smart_1")!!
        assertTrue(WioChannels.matches(smart1, "SMART SPOR"))
        assertTrue(WioChannels.matches(smart1, "SMART SPOR-A"))

        val mavi1 = WioChannels.byId("mavi_1")!!
        assertTrue(WioChannels.matches(mavi1, "S SPORTS"))
        assertTrue(WioChannels.matches(mavi1, "S SPORTS -A"))
    }
}
