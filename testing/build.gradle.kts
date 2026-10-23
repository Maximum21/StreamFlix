plugins {
    id("streamflix.android.library")
}
android { namespace = "com.asadraza.streamflix.core.testing" }
dependencies {
    api(project(":model"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
