plugins {
    id("streamflix.android.library")
    id("streamflix.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.asadraza.streamflix.core.data"
}

dependencies {
    // Modules
    implementation(project(":model"))
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":network"))
    implementation(project(":database"))

    //Coroutines
    implementation(libs.bundles.coroutines)

    //Serialization
    implementation(libs.kotlinx.serialization.json)

    // Paging
    implementation(libs.bundles.paging)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // DataStore
    implementation(libs.bundles.datastore)

    // Testing (junit via plugin)
    testImplementation(libs.bundles.testing)
}