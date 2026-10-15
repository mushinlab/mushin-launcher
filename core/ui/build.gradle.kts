plugins {
    id("mushin.android.library")
    id("mushin.android.compose")
}

android {
    namespace = "com.mushinlab.launcher.core.ui"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
