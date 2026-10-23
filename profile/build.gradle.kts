plugins {
    id("streamflix.android.library")
    id("streamflix.android.hilt")
    id("streamflix.android.compose")
}

android {
    namespace = "com.asadraza.streamflix.feature.profile"
}

dependencies {
    implementation(project(":model"))
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.hilt.navigation.compose)

    androidTestImplementation(libs.androidx.junit)
}
