plugins {
    id("env.android.library")
    alias(libs.plugins.benchmark)
}

android {
    namespace = "com.bluecubelabs.env.core.search.bench"
    defaultConfig {
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
    }
    testBuildType = "release"
    buildTypes {
        release {
            isDefault = true
        }
    }
}

dependencies {
    androidTestImplementation(projects.core.search)
    androidTestImplementation(libs.androidx.benchmark.micro)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit)
}
