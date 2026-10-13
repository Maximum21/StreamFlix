plugins {
    id("streamflix.android.library")
    id("streamflix.android.hilt")
}
android { namespace = "com.asadraza.streamflix.core.domain" }
dependencies {
    implementation(project(":model"))
    implementation(project(":common"))

    //Coroutines
    implementation(libs.bundles.coroutines)

    //Paging
    implementation(libs.bundles.paging)

    //Testing (junit via plugin)
    testImplementation(libs.bundles.testing)   // turbine/mockk/coroutines-test (junit via plugin)
}