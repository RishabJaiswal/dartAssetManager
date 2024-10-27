package com.github.rishabjaiswal.dartassetmanager.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.*
import java.awt.BorderLayout
import org.yaml.snakeyaml.Yaml
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore
import java.awt.Image
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBScrollPane
import java.awt.Component

class FlutterAssetViewerToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(FlutterAssetViewerPanel(project), "", false)
        toolWindow.contentManager.addContent(content)
    }

    private class FlutterAssetViewerPanel(private val project: Project) : JPanel(BorderLayout()) {
        private val assetsPanel: JPanel = JPanel()

        init {
            assetsPanel.layout = BoxLayout(assetsPanel, BoxLayout.Y_AXIS)
            val scrollPane = JBScrollPane(assetsPanel)
            add(scrollPane, BorderLayout.CENTER)
            loadFlutterPackages()
        }

        private fun loadFlutterPackages() {
            ProjectRootManager.getInstance(project).contentRoots.forEach { root ->
                searchForFlutterPackages(root)
            }
        }

        private fun searchForFlutterPackages(directory: VirtualFile) {
            VfsUtilCore.iterateChildrenRecursively(directory, null) { file ->
                if (file.name == "pubspec.yaml") {
                    processFlutterPackage(file.parent)
                }
                true
            }
        }

        private fun processFlutterPackage(packageDir: VirtualFile) {
            val pubspecFile = packageDir.findChild("pubspec.yaml")
            if (pubspecFile != null) {
                val yaml = Yaml()
                try {
                    val pubspecContent = String(pubspecFile.contentsToByteArray())
                    val pubspecMap = yaml.load<Map<String, Any>>(pubspecContent)
                    val packageName = pubspecMap["name"] as? String ?: "Unknown Package"
                    getPackageImages(packageDir, packageName)?.let { assetsPanel.add(it) }
                } catch (e: Exception) {
                    assetsPanel.add(JLabel("Error processing ${packageDir.name}: ${e.message}"))
                }
            }
        }

        private fun getPackageImages(directory: VirtualFile, packageName: String): Component? {

            val imagesContainer = TitledSeparator("Package: $packageName")
            val imageGrid = GridPanel()

            var totalImages = 0;

            VfsUtilCore.iterateChildrenRecursively(directory, null) { file ->
                val isFileNotFromFlutterAssets =
                    file.path.contains("/build/") || file.path.contains("/ios/") || file.path.contains("/.plugin_symlinks")
                if (isFileNotFromFlutterAssets.not() && file.extension?.lowercase() in listOf(
                        "png", "jpg", "jpeg", "gif", "bmp"
                    )
                ) {
                    imageGrid.addItem(getImage(file))
                    totalImages++;
                }
                true
            }
            imagesContainer.add(imageGrid)
            return (if (totalImages > 0) imagesContainer else null)
        }

        private fun getImage(file: VirtualFile): Component {
            try {
                val imageIcon = ImageIcon(file.path)
                val scaledIcon = scaleImage(imageIcon)
                val label = JLabel(scaledIcon)
                label.toolTipText = "${file.name} (${file.path})"
                return label
            } catch (e: Exception) {
                return JLabel("Error loading ${file.name}: ${e.message}")
            }
        }

        private fun scaleImage(icon: ImageIcon): ImageIcon {
            val maxWidth = 100
            val maxHeight = 100
            val img = icon.image
            var width = img.getWidth(null)
            var height = img.getHeight(null)

            if (width > maxWidth || height > maxHeight) {
                val ratio = width.toDouble() / height.toDouble()
                if (width > height) {
                    width = maxWidth
                    height = (width / ratio).toInt()
                } else {
                    height = maxHeight
                    width = (height * ratio).toInt()
                }
                val scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH)
                return ImageIcon(scaledImg)
            }

            return icon
        }
    }
}
