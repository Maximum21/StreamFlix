plugins {
    id("streamflix.android.library")
}

android {
    namespace = "com.asadraza.streamflix.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Network
    implementation(libs.retrofit)

    //Coroutines
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)   // junit comes from the plugin
}