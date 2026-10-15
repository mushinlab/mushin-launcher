plugins {
    `kotlin-dsl`
}

group = "com.mushinlab.launcher.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "mushin.android.application"
            implementationClass = "com.mushinlab.launcher.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "mushin.android.library"
            implementationClass = "com.mushinlab.launcher.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "mushin.android.compose"
            implementationClass = "com.mushinlab.launcher.buildlogic.AndroidComposeConventionPlugin"
        }
        register("jvmLibrary") {
            id = "mushin.jvm.library"
            implementationClass = "com.mushinlab.launcher.buildlogic.JvmLibraryConventionPlugin"
        }
        register("moduleGraph") {
            id = "mushin.module-graph"
            implementationClass = "com.mushinlab.launcher.buildlogic.ModuleGraphConventionPlugin"
        }
        register("quality") {
            id = "mushin.quality"
            implementationClass = "com.mushinlab.launcher.buildlogic.QualityConventionPlugin"
        }
    }
}
