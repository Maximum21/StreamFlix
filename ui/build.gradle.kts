plugins {
    id("streamflix.android.library")
    id("streamflix.android.compose")
}

android {
    namespace = "com.asadraza.streamflix.core.ui"
}

dependencies {
    implementation(project(":model"))

    //Android - Material
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    //Coil
    implementation(libs.coil.compose)
}