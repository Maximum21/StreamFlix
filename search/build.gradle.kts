plugins {
    id("streamflix.android.library")
    id("streamflix.android.hilt")
    id("streamflix.android.compose")
}

android {
    namespace = "com.asadraza.streamflix.feature.search"
}

dependencies {
    implementation(project(":home"))
    implementation(project(":model"))
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":ui"))

    implementation(libs.hilt.navigation.compose)    //Hilt
    implementation(libs.bundles.paging)             //Paging

    //Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    //Coil
    implementation(libs.coil.compose)

    implementation(libs.gson)
}