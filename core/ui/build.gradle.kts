plugins {
    id("env.android.library")
    id("env.android.compose")
}

android {
    namespace = "com.bluecubelabs.env.core.ui"
}

dependencies {
    api(projects.core.model)
}
