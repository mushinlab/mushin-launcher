plugins {
    id("mushin.android.library")
    id("mushin.android.compose")
}

android {
    namespace = "com.mushinlab.launcher.feature.settings"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(projects.core.search)
}
