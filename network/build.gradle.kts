import java.util.Properties

plugins {
    id("streamflix.android.library")
    id("streamflix.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val secretProps = Properties().apply {
    rootProject.file("secrets.properties").inputStream().use { load(it) }
}

android {
    namespace = "com.asadraza.streamflix.core.network"
    defaultConfig {
        buildConfigField("String", "TMDB_ACCESS_TOKEN", secretProps.getProperty("TMDB_ACCESS_TOKEN"))
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