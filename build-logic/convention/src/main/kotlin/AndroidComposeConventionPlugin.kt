import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<LibraryExtension> {
            buildFeatures { compose = true }
            // Robolectric needs merged resources to resolve strings and themes in Compose tests
            testOptions { unitTests.isIncludeAndroidResources = true }
        }

        dependencies {
            val bom = libs.findLibrary("androidx-compose-bom").get()
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("implementation", libs.findBundle("compose").get())
            add("implementation", libs.findLibrary("androidx-material3").get())
            add("implementation", libs.findLibrary("compose-material-icons-extended").get())
            add("implementation", libs.findLibrary("androidx-activity-compose").get())

            add("testImplementation", platform(bom))
            add("testImplementation", libs.findLibrary("androidx-ui-test-junit4").get())
            add("testImplementation", libs.findLibrary("robolectric").get())
            add("testImplementation", libs.findLibrary("androidx-test-core").get())
            add("debugImplementation", libs.findLibrary("androidx-ui-test-manifest").get())
        }
    }
}