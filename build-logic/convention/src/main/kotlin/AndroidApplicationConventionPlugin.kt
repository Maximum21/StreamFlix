import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        with(pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
            apply("org.jetbrains.kotlin.plugin.compose")
        }

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()

            defaultConfig {
                minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
                targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                vectorDrawables { useSupportLibrary = true }
            }

            lint { ignoreTestSources = true }

            lint { ignoreTestSources = true }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
                isCoreLibraryDesugaringEnabled = true
            }

            buildFeatures {
                compose = true
                buildConfig = true
            }

            packaging {
                resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
            }
        }

        extensions.configure<ApplicationAndroidComponentsExtension> {
            beforeVariants(selector().withBuildType("release")) { it.enableUnitTest = false }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            jvmToolchain(17)
        }

        dependencies {
            add("coreLibraryDesugaring", libs.findLibrary("desugar-jdk-libs").get())
            add("testImplementation", libs.findBundle("testing").get())
            add("testImplementation", project(":testing"))
        }
    }
}
