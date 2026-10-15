package com.mushinlab.launcher.buildlogic

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        extensions.configure<KtlintExtension> {
            version.set(libs.findVersion("ktlint").get().requiredVersion)
            android.set(true)
            filter {
                exclude { it.file.path.contains("/build/") }
            }
        }
        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            parallel = true
        }
        configurations.matching { it.name == "detekt" }.configureEach {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin") {
                    useVersion(DETEKT_KOTLIN_VERSION)
                }
            }
        }
    }

    private companion object {
        const val DETEKT_KOTLIN_VERSION = "2.0.21"
    }
}
