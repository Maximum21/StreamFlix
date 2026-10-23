plugins {
    id("streamflix.android.library")
    id("streamflix.android.compose")
}

android {
    namespace = "com.asadraza.streamflix.core.ui"
}

dependencies {
    implementation(project(":model"))
    implementation(project(":common"))

    // Android - Material
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Paging
    implementation(libs.bundles.paging)

    // Coil
    api(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
