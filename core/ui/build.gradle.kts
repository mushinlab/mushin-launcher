plugins {
    id("mushin.android.library")
    id("mushin.android.compose")
}

android {
    namespace = "com.mushinlab.launcher.core.ui"
}

dependencies {
    api(projects.core.model)
}
