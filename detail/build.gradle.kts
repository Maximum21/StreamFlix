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

    implementation(libs.hilt.navigation.compose)    //Hilt

    implementation(libs.androidx.media3.exoplayer)  //Media3
    implementation(libs.androidx.media3.ui)
    implementation(libs.coil.compose)               //Coil
}