import org.gradle.api.tasks.Sync

version = 10

cloudstream {
    setRepo("Wiojelt/WioSpor")
    iconUrl = "https://raw.githubusercontent.com/Wiojelt/WioSpor/main/assets/logo.png?v=3"
    description = "Tüm canlı TV ve spor kanalları tek eklentide. Renk grupları ve alternatif yayın kaynakları."
    language = "tr"
    status = 3
    tvTypes = listOf("Live")
    requiresResources = false
}

val generatedBundleSources = layout.buildDirectory.dir("generated/wiosporBundle/kotlin")

val prepareBundleSources by tasks.registering(Sync::class) {
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
        "shared",
        "shared-filter",
    ).forEach { source ->
        from(rootProject.file("$source/src/main/kotlin"))
    }
    filteringCharset = "UTF-8"
    filter { line: String ->
        when (line.trim()) {
            "@CloudstreamPlugin",
            "import com.lagradost.cloudstream3.plugins.CloudstreamPlugin" -> null
            else -> line
        }
    }
}

android {
    sourceSets.getByName("main").kotlin.directories += generatedBundleSources.get().asFile.path
}

tasks.named("preBuild").configure {
    dependsOn(prepareBundleSources)
}
