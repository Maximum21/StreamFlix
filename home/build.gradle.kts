plugins {
    id("streamflix.android.library")
    id("streamflix.android.hilt")
    id("streamflix.android.compose")
}
android { namespace = "com.asadraza.streamflix.feature.home" }
dependencies {
    implementation(project(":model"))
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":ui"))

    // Viewmodel
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.navigation.compose)

    // Coroutine
    implementation(libs.bundles.coroutines)

    // Paging
    implementation(libs.bundles.paging)
}
