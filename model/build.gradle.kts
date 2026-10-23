plugins {
    id("streamflix.android.library")
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "com.asadraza.streamflix.core.model" }
dependencies {
    implementation(libs.androidx.core.ktx)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Testing (junit via plugin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
