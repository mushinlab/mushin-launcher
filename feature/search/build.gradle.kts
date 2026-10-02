plugins {
    id("env.android.library")
    id("env.android.compose")
}

android {
    namespace = "com.bluecubelabs.env.feature.search"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(projects.core.search)
}
