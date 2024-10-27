package com.github.rishabjaiswal.dartassetmanager.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore
import org.yaml.snakeyaml.Yaml

class PackageService(private val project: Project) {

    val EXCLUDED_PATHS = listOf("/build/", "/ios/", "/.plugin_symlinks/")

    fun loadFlutterPackages(): List<PackageInfo> {
        val packages = mutableListOf<PackageInfo>()

        ProjectRootManager.getInstance(project).contentRoots.forEach { root ->
            VfsUtilCore.iterateChildrenRecursively(root, null) { file ->
                if (!isExcludedPath(file.path) && file.name == "pubspec.yaml") {
                    processPackage(file)?.let { packages.add(it) }
                }
                true
            }
        }

        return packages.sortedBy { it.name }
    }

    private fun isExcludedPath(filePath: String): Boolean {
        return EXCLUDED_PATHS.any { filePath.contains(it) }
    }

    private fun processPackage(pubspecFile: VirtualFile): PackageInfo? {
        try {
            val yaml = Yaml()
            val pubspecContent = String(pubspecFile.contentsToByteArray())
            val pubspecMap = yaml.load<Map<String, Any>>(pubspecContent)
            val packageName = pubspecMap["name"] as? String ?: return null

            return PackageInfo(
                name = packageName,
                directory = pubspecFile.parent,
                pubspecFile = pubspecFile
            )
        } catch (e: Exception) {
            return null
        }
    }
}