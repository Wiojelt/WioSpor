import org.gradle.api.tasks.Sync

version = 20

cloudstream {
    setRepo("Wiojelt/WioSpor")
    iconUrl = "https://raw.githubusercontent.com/Wiojelt/WioSpor/main/assets/logo.png?v=3"
    description = "Tüm canlı TV ve spor kanalları tek eklentide. Renk grupları ve alternatif yayın kaynakları."
    language = "tr"
    status = 3
    tvTypes = listOf("Live")
    requiresResources = false
}

val syncCommonUi by tasks.registering {
    doLast {
        val src = rootProject.file("../TurkSinema-Source/common/src/main/kotlin/dev/wiojelt/turksinema/common/WioCoreSettingsDialog.kt")
        val dst = rootProject.file("common/src/main/kotlin/turkspor/common/WioCoreSettingsDialog.kt")
        if (src.exists()) {
            val srcText = src.readText(Charsets.UTF_8)
            val expectedDstText = srcText.replace("package dev.wiojelt.turksinema.common", "package turkspor.common")
            if (!dst.exists() || dst.readText(Charsets.UTF_8) != expectedDstText) {
                dst.parentFile.mkdirs()
                dst.writeText(expectedDstText, Charsets.UTF_8)
                logger.lifecycle("✓ Otomatik senkronize edildi: WioCoreSettingsDialog.kt (TurkSinema -> TurkSpor)")
            }
        }
    }
}

val generatedBundleSources = layout.buildDirectory.dir("generated/wiosporBundle/kotlin")

val prepareBundleSources by tasks.registering(Sync::class) {
    dependsOn(syncCommonUi)
    into(generatedBundleSources)
    listOf(
        "SelcukSports",
        "KralSporHD",
        "Taraftarium24",
        "InatTV",
        "Crex",
        "MahsunSports",
        "ArdaSpor",
        "MacKeyfi",
        "ZbahisTV",
        "InterSporTV",
        "BeyazElma",
        "InatBox",
        "AslanTV",
        "Streamed",
        "DomatesTV",
        "DominoTV",
        "PapazSports",
        "JestYayin",
        "shared",
        "shared-filter",
    ).forEach { source ->
        from(rootProject.file("$source/src/main/kotlin"))
    }
    exclude("turkspor/papazsports/PapazSportsPlugin.kt")
    exclude("turkspor/jestyayin/JestYayinPlugin.kt")
    filteringCharset = "UTF-8"
    filter { line: String ->
        line.replace("@CloudstreamPlugin", "")
            .replace("import com.lagradost.cloudstream3.plugins.CloudstreamPlugin", "")
    }
}

android {
    sourceSets.getByName("main").kotlin.directories += generatedBundleSources.get().asFile.path
}

tasks.named("preBuild").configure {
    dependsOn(prepareBundleSources)
}
