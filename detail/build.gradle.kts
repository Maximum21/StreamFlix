plugins {
    id("streamflix.android.library")
    id("streamflix.android.hilt")
    id("streamflix.android.compose")
}

android {
    namespace = "com.asadraza.streamflix.feature.detail"
}

dependencies {
    implementation(project(":model"))
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":ui"))

    implementation(libs.hilt.navigation.compose) // Hilt
}
