plugins {
    id("streamflix.android.library")
    id("streamflix.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.asadraza.streamflix.core.database"

    buildFeatures {
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":common"))

    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    //Gson
    implementation(libs.gson)

    //Hilt
    implementation(libs.hilt.navigation.compose)
}