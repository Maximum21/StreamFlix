plugins {
    id("streamflix.android.library")
    id("streamflix.android.hilt")
}

android {
    namespace = "com.asadraza.streamflix.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test) // junit comes from the plugin
}
