import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
val secretPropertiesFile: File = rootProject.file("secrets.properties")
val sitProps = Properties()
secretPropertiesFile.inputStream().use { input ->
    sitProps.load(input)
}
android {
    namespace = "com.asadraza.streamflix.core.network"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        buildConfigField("String", "TMDB_API_KEY", sitProps.getProperty("TMDB_API_KEY"))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true  // This enables BuildConfig generation
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":model"))
    implementation(project(":common"))


    //Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    //Network
    implementation(libs.bundles.network)

    //Coroutines
    implementation(libs.bundles.coroutines)

    //Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}