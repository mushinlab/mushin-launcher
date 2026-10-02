package com.bluecubelabs.env.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

class ModuleGraphConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register("checkModuleGraph", CheckModuleGraphTask::class.java) {
            group = "verification"
            description = "Checks module dependency rules from spec D.2"
            rootDir.set(target.layout.projectDirectory)
            buildFiles.from(
                target.fileTree(target.projectDir) {
                    include("**/build.gradle.kts")
                    exclude("build-logic/**", "**/build/**", "build.gradle.kts")
                },
            )
        }
    }
}

abstract class CheckModuleGraphTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val buildFiles: ConfigurableFileCollection

    @get:Internal
    abstract val rootDir: DirectoryProperty

    @TaskAction
    fun check() {
        val root = rootDir.get().asFile
        val modules = buildFiles.files.associate { file -> modulePath(root, file) to file.readText() }
        val violations = modules.flatMap { (module, script) -> violationsOf(module, script) }
        if (violations.isNotEmpty()) {
            throw GradleException("Module graph violations:\n" + violations.joinToString("\n"))
        }
        logger.lifecycle("Module graph OK: ${modules.size} modules")
    }

    private fun modulePath(root: File, file: File): String = ":" + file.parentFile.relativeTo(root).path.replace(File.separatorChar, ':')

    private fun violationsOf(module: String, script: String): List<String> {
        val deps = PROJECT_REF.findAll(script).map { ":" + it.groupValues[1].replace('.', ':') }.toSet()
        val result = mutableListOf<String>()
        val pureJvm = module == ":core:model" || module == ":core:search"
        if (pureJvm && !script.contains("env.jvm.library")) result += "$module must be a pure JVM module"
        if (module == ":core:model" && deps.isNotEmpty()) result += "$module must not depend on $deps"
        if (module == ":core:search" && (deps - ":core:model").isNotEmpty()) result += "$module may depend only on :core:model"
        if (module.startsWith(":core:")) {
            deps.filter { it.startsWith(":feature:") || it == ":app" }.forEach { result += "$module -> $it" }
        }
        if (module.startsWith(":feature:")) {
            deps.filter { it.startsWith(":feature:") || it == ":app" }.forEach { result += "$module -> $it" }
        }
        return result
    }

    private companion object {
        val PROJECT_REF = Regex("""projects\.([A-Za-z.]+)""")
    }
}
