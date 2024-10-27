package com.github.rishabjaiswal.dartassetmanager.toolWindow

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.ui.JBUI
import javax.swing.*
import java.awt.*

class ImageGridPanel : JPanel(GridLayout(0, 4, 10, 10)) {

    init {
        border = JBUI.Borders.empty(10)
    }

    fun loadImagesFromPackage(directory: VirtualFile) {
        removeAll()

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
            imageFiles.forEach { displayImage(it) }
        }

        revalidate()
        repaint()
    }

    private fun displayNoImagesMessage() {
        val messageLabel = JLabel("No images found in this package", SwingConstants.CENTER)
        messageLabel.border = JBUI.Borders.empty(20)
        add(messageLabel)
    }

    private fun displayImage(file: VirtualFile) {
        try {
            val imageIcon = ImageIcon(file.path)
            val scaledIcon = ImageUtils.scaleImage(imageIcon, 200, 200)
            val label = createImageLabel(scaledIcon, file)

            val wrapper = JPanel(BorderLayout())
            wrapper.add(label, BorderLayout.CENTER)
            wrapper.border = JBUI.Borders.empty(5)

            add(wrapper)
        } catch (e: Exception) {
            add(JLabel("Error loading ${file.name}"))
        }
    }

    private fun createImageLabel(icon: ImageIcon, file: VirtualFile): JLabel {
        return JLabel(icon).apply {
            horizontalAlignment = SwingConstants.CENTER
            text = file.name
            horizontalTextPosition = SwingConstants.CENTER
            verticalTextPosition = SwingConstants.BOTTOM
            toolTipText = "${file.name} (${file.path})"
        }
    }
}