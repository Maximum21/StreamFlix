import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import kotlinx.kover.gradle.plugin.dsl.KoverReportFiltersConfig

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.gsm) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

allprojects {
    configurations.all {
        resolutionStrategy {
            // A transitive constraint on kotlin-stdlib-common 1.8.x breaks resolution; align to the compiler version.
            force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.21")
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // detekt configuration (shared across all modules)
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true // start from detekt's defaults, override in our yml
        parallel = true
        baseline = file("detekt-baseline.xml")
    }

    // ktlint configuration (shared across all modules)
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true) // Android-specific rules
        ignoreFailures.set(false) // violations FAIL the build
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
        }
    }
}

// Generated code and composable UI are excluded: the gate measures logic, and screens are covered
// by Compose tests rather than line coverage. Shared by the merged report and the per-module gates.
val coverageFilters: KoverReportFiltersConfig.() -> Unit = {
    excludes {
        annotatedBy(
            "androidx.compose.runtime.Composable",
            "androidx.compose.ui.tooling.preview.Preview",
            "javax.annotation.processing.Generated",
            "dagger.internal.DaggerGenerated"
        )
        classes(
            "*.BuildConfig",
            "*.R",
            "*.R$*",
            "*ComposableSingletons*",
            "*_Factory*",
            "*_HiltModules*",
            "*_MembersInjector",
            "*Hilt_*",
            "*_Impl*",
            "dagger.hilt.*",
            "hilt_aggregated_deps.*",
            "*.di.*Module*",
            // Thin SDK adapters; their logic is exercised through fakes and the rules tests
            "*.source.firebase.*",
            "*.DataStoreCurrentProfileStore*",
            // Serializable DTOs and the Retrofit interface carry no logic
            "*.network.model.*",
            "*.network.api.*",
            "*.network.datasource.RemoteDataSource*"
        )
    }
}

kover {
    reports {
        filters(coverageFilters)
        verify {
            rule("Overall line coverage") {
                minBound(60)
            }
        }
    }
}

// Core logic modules are held to a higher bar than the app as a whole
subprojects {
    extensions.configure<KoverProjectExtension> {
        reports {
            filters(coverageFilters)
            if (name == "domain" || name == "data") {
                verify {
                    rule("Line coverage of $name") {
                        minBound(80)
                    }
                }
            }
        }
    }
}

dependencies {
    subprojects
        .filter { it.name != "testing" && it.name != "baselineprofile" }
        .forEach { kover(it) }
}

// Enforces the module dependency rule from ADR-001: dependencies point towards the core, and
// features never depend on each other.
val featureModules = setOf("auth", "home", "search", "detail", "player", "profile", "settings", "mylist")
val forbiddenDependencies = mapOf(
    "model" to setOf("common", "domain", "data", "network", "database", "ui") + featureModules,
    "common" to setOf("domain", "data", "network", "database", "ui") + featureModules,
    "domain" to setOf("data", "network", "database", "ui") + featureModules,
    "network" to setOf("domain", "data", "database", "ui") + featureModules,
    "database" to setOf("domain", "data", "network", "ui") + featureModules,
    "data" to setOf("ui") + featureModules,
    "ui" to setOf("domain", "data", "network", "database") + featureModules
) + featureModules.associateWith { feature ->
    featureModules - feature + setOf("data", "network", "database")
}

tasks.register("checkModuleGraph") {
    group = "verification"
    description = "Fails when a module depends on a module it must not depend on."
    doLast {
        val violations = subprojects.flatMap { module ->
            val forbidden = forbiddenDependencies[module.name].orEmpty()
            module.configurations
                .filter { it.name in setOf("api", "implementation", "compileOnly") }
                .flatMap { it.dependencies.withType<ProjectDependency>() }
                .map { it.name }
                .filter { it in forbidden }
                .map { "${module.name} -> $it" }
        }.distinct()
        check(violations.isEmpty()) { "Forbidden module dependencies:\n" + violations.joinToString("\n") }
    }
}

subprojects {
    tasks.matching { it.name == "check" }.configureEach { dependsOn(rootProject.tasks.named("checkModuleGraph")) }
}
