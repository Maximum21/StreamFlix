import java.util.Properties

plugins {
    id("streamflix.android.library")
    id("streamflix.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val secrets =
    Properties().apply {
        val file = rootProject.file("secrets.properties")
        if (file.exists()) file.inputStream().use(::load)
    }

// Local file first, then the environment variable that CI provides
fun clean(raw: String?): String = (raw ?: "").trim().removeSurrounding("\"").trim()

// First non-empty value wins: local file, then the CI environment variable
val tmdbToken =
    listOf(secrets.getProperty("TMDB_ACCESS_TOKEN"), System.getenv("TMDB_ACCESS_TOKEN"))
        .map(::clean)
        .firstOrNull { it.isNotEmpty() }
        .orEmpty()

if (tmdbToken.isEmpty()) {
    logger.warn("TMDB_ACCESS_TOKEN is not set; TMDB requests will fail with 401")
}

val escapedToken = tmdbToken.replace("\\", "\\\\").replace("\"", "\\\"")

// A release build without a token would ship an app that cannot reach TMDB
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        check(tmdbToken.isNotEmpty()) {
            "TMDB_ACCESS_TOKEN is missing: set it in secrets.properties or as an environment variable"
        }
    }
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
    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Network
    implementation(libs.bundles.network)

    // Coroutine
    implementation(libs.bundles.coroutines)

    testImplementation(libs.okhttp.mockwebserver)
}
