plugins {
    `kotlin-dsl`
}

group = "com.bluecubelabs.env.buildlogic"

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
            id = "env.android.application"
            implementationClass = "com.bluecubelabs.env.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "env.android.library"
            implementationClass = "com.bluecubelabs.env.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "env.android.compose"
            implementationClass = "com.bluecubelabs.env.buildlogic.AndroidComposeConventionPlugin"
        }
        register("jvmLibrary") {
            id = "env.jvm.library"
            implementationClass = "com.bluecubelabs.env.buildlogic.JvmLibraryConventionPlugin"
        }
        register("moduleGraph") {
            id = "env.module-graph"
            implementationClass = "com.bluecubelabs.env.buildlogic.ModuleGraphConventionPlugin"
        }
        register("quality") {
            id = "env.quality"
            implementationClass = "com.bluecubelabs.env.buildlogic.QualityConventionPlugin"
        }
    }
}
