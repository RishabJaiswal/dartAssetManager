package com.github.rishabjaiswal.dartassetmanager.toolWindow


import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.ui.JBUI
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.*
import javax.swing.*
import java.io.File

class ImageListPanel : JBPanel<ImageListPanel>(BorderLayout()) {
    private val listPanel = JBPanel<JBPanel<*>>()

    init {
        listPanel.layout = BoxLayout(listPanel, BoxLayout.Y_AXIS)
        add(listPanel, BorderLayout.NORTH)
        border = JBUI.Borders.empty(10)
    }

    fun loadImagesFromPackage(directory: VirtualFile) {
        listPanel.removeAll()

        val imageFiles = mutableListOf<VirtualFile>()

        VfsUtilCore.iterateChildrenRecursively(directory, null) { file ->
            if (ImageUtils.isImageFile(file.extension)) {
                imageFiles.add(file)
            }
            true
        }

        imageFiles.sortBy { it.name }

        if (imageFiles.isEmpty()) {
            displayNoImagesMessage()
        } else {
            imageFiles.forEach { file ->
                displayImageListItem(file, directory)
                // Add separator after each item except the last one
                if (file != imageFiles.last()) {
                    listPanel.add(JSeparator())
                    listPanel.add(Box.createVerticalStrut(10))
                }
            }
        }

        revalidate()
        repaint()
    }

    private fun displayNoImagesMessage() {
        val messageLabel = JBLabel("No images found in this package", SwingConstants.CENTER)
        messageLabel.border = JBUI.Borders.empty(20)
        listPanel.add(messageLabel)
    }

    private fun displayImageListItem(file: VirtualFile, packageDir: VirtualFile) {
        try {
            // Create container for the list item
            val itemPanel = JBPanel<JBPanel<*>>(BorderLayout())
            itemPanel.border = JBUI.Borders.empty(10)

            // Create and configure image panel
            val scaledIcon = ImageUtils.loadImage(file, 200, 200)
            val imageLabel = JLabel(scaledIcon)
            imageLabel.preferredSize = Dimension(200, 200)
            imageLabel.border = JBUI.Borders.empty(5)

            // Create description panel
            val descriptionPanel = JBPanel<JBPanel<*>>()
            descriptionPanel.layout = BoxLayout(descriptionPanel, BoxLayout.Y_AXIS)
            descriptionPanel.border = JBUI.Borders.empty(5, 15, 5, 5)
            descriptionPanel.alignmentY = Component.TOP_ALIGNMENT

            // File name label (bold, black)
            val nameLabel = JBLabel(file.name).apply {
                font = font.deriveFont(Font.BOLD)
                foreground = Color.BLACK
                alignmentX = Component.LEFT_ALIGNMENT
            }

            // Relative path label (smaller, gray)
            val relativePath = getRelativePath(file, packageDir)
            val pathLabel = JBLabel(relativePath).apply {
                font = font.deriveFont(font.size - 2f)
                foreground = Color.GRAY
                alignmentX = Component.LEFT_ALIGNMENT
            }

            // Add components to description panel
            descriptionPanel.add(nameLabel)
            descriptionPanel.add(Box.createVerticalStrut(5))
            descriptionPanel.add(pathLabel)

            // Add image and description to item panel
            itemPanel.add(imageLabel, BorderLayout.WEST)
            itemPanel.add(descriptionPanel, BorderLayout.CENTER)

            // Add item panel to list
            listPanel.add(itemPanel)

        } catch (e: Exception) {
            listPanel.add(JBLabel("Error loading ${file.name}: ${e.message}"))
        }
    }

    private fun getRelativePath(file: VirtualFile, packageDir: VirtualFile): String {
        return file.path.removePrefix(packageDir.path).removePrefix(File.separator)
    }
}
