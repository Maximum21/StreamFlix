pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StreamFlix"
include(":app")
include(":network")
include(":database")
include(":data")
include(":domain")
include(":ui")
include(":common")
include(":model")
include(":home")
include(":search")
include(":detail")
include(":player")
include(":profile")
include(":auth")
