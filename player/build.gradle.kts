plugins {
    id("streamflix.android.library")
    id("streamflix.android.hilt")
    id("streamflix.android.compose")
}

android {
    namespace = "com.asadraza.streamflix.feature.player"
}

dependencies {
    implementation(project(":model"))
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":ui"))

    implementation(libs.androidx.core.ktx) // Core
    implementation(libs.androidx.appcompat)
    implementation(libs.material) // Material
    implementation(libs.hilt.navigation.compose) // Hilt

    implementation(libs.youtube.player) // Youtube Player

    androidTestImplementation(libs.androidx.junit) // JUnit
    androidTestImplementation(libs.androidx.espresso.core) // Espresso
}
