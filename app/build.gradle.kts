import java.util.Properties

plugins {
    id("streamflix.android.application")
    id("streamflix.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.gsm)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.baselineprofile)
}

// Signing material comes from keystore.properties locally (gitignored) or from the environment in CI
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingValue(property: String, environmentVariable: String): String? =
    (keystoreProperties.getProperty(property) ?: System.getenv(environmentVariable))
        ?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "SIGNING_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "SIGNING_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "SIGNING_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "SIGNING_KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { it != null }

android {
    namespace = "com.asadraza.streamflix"

    testOptions { unitTests.isIncludeAndroidResources = true }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.asadraza.streamflix"
        // CI derives these from the run number and the release tag; local builds keep the defaults
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.0.0"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Without signing material the build produces an unsigned artifact (fine for CI checks)
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    // ══════════════════════════════════════════════════════════
    //  Feature Modules (Presentation Layer)
    // ══════════════════════════════════════════════════════════
    implementation(project(":auth"))
    implementation(project(":home"))
    implementation(project(":search"))
    implementation(project(":detail"))
    implementation(project(":player"))
    implementation(project(":profile"))
    implementation(project(":settings"))
    implementation(project(":mylist"))

    // ══════════════════════════════════════════════════════════
    //  Core Modules (Shared Infrastructure)
    // ══════════════════════════════════════════════════════════
    implementation(project(":ui")) // UI components & theme
    implementation(project(":common")) // Common utilities
    implementation(project(":model")) // Data models
    implementation(project(":domain")) // Use cases & repositories interfaces
    implementation(project(":data")) // Repository implementations
    implementation(project(":network")) // Network layer
    implementation(project(":database")) // Local database

    // ══════════════════════════════════════════════════════════
    //  AndroidX Core
    // ══════════════════════════════════════════════════════════
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // ══════════════════════════════════════════════════════════
    //  Compose
    // ══════════════════════════════════════════════════════════
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.compose.material.icons.extended)

    // ══════════════════════════════════════════════════════════
    //  Navigation
    // ══════════════════════════════════════════════════════════
    implementation(libs.androidx.compose.navigation)
    implementation(libs.kotlinx.serialization.json)

    // ══════════════════════════════════════════════════════════
    //  Dependency Injection - Hilt
    // ══════════════════════════════════════════════════════════
    implementation(libs.hilt.navigation.compose)

    // ══════════════════════════════════════════════════════════
    //  Firebase
    // ══════════════════════════════════════════════════════════
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    releaseImplementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    // ══════════════════════════════════════════════════════════
    //  Coroutines
    // ══════════════════════════════════════════════════════════
    implementation(libs.bundles.coroutines)

    // ══════════════════════════════════════════════════════════
    //  Utilities
    // ══════════════════════════════════════════════════════════
    implementation(libs.timber) // Logging

    // ══════════════════════════════════════════════════════════
    //  Testing
    // ══════════════════════════════════════════════════════════
    testImplementation(libs.truth)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    kspTest(libs.hilt.compiler)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    // ══════════════════════════════════════════════════════════
    //  Debug Tools
    // ══════════════════════════════════════════════════════════
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)
}
