pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
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

rootProject.name = "env-launcher"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    ":app",
    ":core:model",
    ":core:search",
    ":core:search:bench",
    ":core:data",
    ":core:apps",
    ":core:ui",
    ":feature:home",
    ":feature:search",
    ":feature:settings",
    ":benchmark",
)
