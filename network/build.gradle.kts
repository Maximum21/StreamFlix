import java.util.Properties

plugins {
    id("streamflix.android.library")
    id("streamflix.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val secrets = Properties().apply {
    val file = rootProject.file("secrets.properties")
    if (file.exists()) file.inputStream().use(::load)
}

// Local file first, then the environment variable that CI provides
val tmdbToken = (secrets.getProperty("TMDB_ACCESS_TOKEN") ?: System.getenv("TMDB_ACCESS_TOKEN") ?: "")
    .trim()
    .removeSurrounding("\"")

if (tmdbToken.isEmpty()) {
    logger.warn("TMDB_ACCESS_TOKEN is not set; TMDB requests will fail with 401")
}

android {
    namespace = "com.asadraza.streamflix.core.network"
    defaultConfig {
        buildConfigField("String", "TMDB_ACCESS_TOKEN", "\"$tmdbToken\"")
    }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(project(":model"))
    implementation(project(":common"))
    //Hilt
    implementation(libs.hilt.navigation.compose)

    //Serialization
    implementation(libs.kotlinx.serialization.json)

    //Network
    implementation(libs.bundles.network)

    //Coroutine
    implementation(libs.bundles.coroutines)

    //Mockk
    testImplementation(libs.mockk)
}