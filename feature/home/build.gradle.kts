plugins {
    id("env.android.library")
    id("env.android.compose")
}

android {
    namespace = "com.bluecubelabs.env.feature.home"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.ui)
}
