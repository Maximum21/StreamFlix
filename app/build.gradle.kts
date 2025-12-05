plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.gsm)
}

android {
    namespace = "com.asadraza.streamflix"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.asadraza.streamflix"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
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

            // Signing configuration
            // Add your keystore configuration here
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"

        // Enable explicit API mode for better encapsulation
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildFeatures {
        compose = true
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

    // ══════════════════════════════════════════════════════════
    //  Core Modules (Shared Infrastructure)
    // ══════════════════════════════════════════════════════════
    implementation(project(":ui"))           // UI components & theme
    implementation(project(":common"))       // Common utilities
    implementation(project(":model"))        // Data models
    implementation(project(":domain"))       // Use cases & repositories interfaces
    implementation(project(":data"))         // Repository implementations
    implementation(project(":network"))      // Network layer
    implementation(project(":database"))     // Local database

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
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ══════════════════════════════════════════════════════════
    //  Firebase
    // ══════════════════════════════════════════════════════════
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)

    // ══════════════════════════════════════════════════════════
    //  Coroutines
    // ══════════════════════════════════════════════════════════
    implementation(libs.bundles.coroutines)

    // ══════════════════════════════════════════════════════════
    //  Utilities
    // ══════════════════════════════════════════════════════════
    implementation(libs.timber)  // Logging

    // ══════════════════════════════════════════════════════════
    //  Desugaring (for older Android versions)
    // ══════════════════════════════════════════════════════════
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // ══════════════════════════════════════════════════════════
    //  Testing
    // ══════════════════════════════════════════════════════════
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)

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