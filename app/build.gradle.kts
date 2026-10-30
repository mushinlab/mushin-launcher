import com.android.build.api.artifact.SingleArtifact
import java.util.Properties

plugins {
    id("mushin.android.application")
    id("mushin.android.compose")
    alias(libs.plugins.baselineprofile)
}

val uploadKeystore = rootProject.file("keystore.properties").takeIf { it.isFile }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}
val debugSignedRelease = providers.gradleProperty("mushin.debugSignedRelease").orNull == "true"

android {
    namespace = "com.mushinlab.launcher"
    defaultConfig {
        applicationId = "com.mushinlab.launcher"
        versionCode = 9
        versionName = "0.6.0"
    }
    signingConfigs {
        if (uploadKeystore != null) {
            create("upload") {
                storeFile = rootProject.file(uploadKeystore.getProperty("storeFile"))
                storePassword = uploadKeystore.getProperty("storePassword")
                keyAlias = uploadKeystore.getProperty("keyAlias")
                keyPassword = uploadKeystore.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload")
                ?: signingConfigs.getByName("debug").takeIf { debugSignedRelease }
            vcsInfo.include = false
        }
    }
    buildFeatures {
        buildConfig = true
    }
    dependenciesInfo {
        includeInApk = false
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val manifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
        val variantName = variant.name
        tasks.register("check${variantName.replaceFirstChar(Char::uppercase)}Permissions") {
            inputs.file(manifest)
            doLast {
                val text = manifest.get().asFile.readText()
                val found = listOf("INTERNET", "QUERY_ALL_PACKAGES")
                    .map { "android.permission.$it" }
                    .filter { "\"$it\"" in text }
                check(found.isEmpty()) { "Forbidden permissions in $variantName manifest: $found" }
            }
        }
    }
}

baselineProfile {
    dexLayoutOptimization = true
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.search)
    implementation(projects.core.data)
    implementation(projects.core.apps)
    implementation(projects.core.ui)
    implementation(projects.feature.home)
    implementation(projects.feature.search)
    implementation(projects.feature.settings)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.coroutines.android)
    baselineProfile(projects.benchmark)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.espresso)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
