package com.github.rishabjaiswal.dartassetmanager.toolWindow


import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SVGLoader
import com.intellij.util.ui.JBImageIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.ImageIcon


object ImageUtils {
    fun loadImage(file: VirtualFile, targetWidth: Int, targetHeight: Int): Icon {
        return when (file.extension?.lowercase()) {
            "svg" -> loadSvgIcon(file, targetWidth, targetHeight)
            else -> loadRasterImage(file, targetWidth, targetHeight)
        }
    }

    private fun loadSvgIcon(file: VirtualFile, targetWidth: Int, targetHeight: Int): Icon {
        try {
            // Load SVG using SVGLoader
            val svg = SVGLoader.load(file.inputStream, 1.0f)
            return ImageIcon(svg)
        } catch (e: Exception) {
            e.printStackTrace()
            return createErrorIcon(targetWidth, targetHeight)
        }
    }

    private fun loadRasterImage(file: VirtualFile, targetWidth: Int, targetHeight: Int): Icon {
        try {
            val originalIcon = ImageIcon(file.path)
            val size = calculateTargetSize(
                originalIcon.iconWidth,
                originalIcon.iconHeight,
                targetWidth,
                targetHeight
            )

            val scaled = originalIcon.image.getScaledInstance(
                size.width,
                size.height,
                Image.SCALE_SMOOTH
            )
            return JBImageIcon(scaled)
        } catch (e: Exception) {
            e.printStackTrace()
            return createErrorIcon(targetWidth, targetHeight)
        }
    }

    private fun calculateTargetSize(originalWidth: Int, originalHeight: Int, maxWidth: Int, maxHeight: Int): Dimension {
        if (originalWidth <= 0 || originalHeight <= 0) {
            return Dimension(maxWidth, maxHeight)
        }

        val widthRatio = maxWidth.toDouble() / originalWidth
        val heightRatio = maxHeight.toDouble() / originalHeight
        val ratio = minOf(widthRatio, heightRatio)

        return Dimension(
            (originalWidth * ratio).toInt(),
            (originalHeight * ratio).toInt()
        )
    }

    private fun createErrorIcon(width: Int, height: Int): Icon {
        val scaledWidth = JBUI.scale(width)
        val scaledHeight = JBUI.scale(height)

        val image = UIUtil.createImage(
            null,
            scaledWidth,
            scaledHeight,
            BufferedImage.TYPE_INT_ARGB
        )

        val g2 = image.createGraphics()
        try {
            g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            )

            val errorMessage = "Error loading image"
            g2.font = JBUI.Fonts.label(12f)
            g2.color = UIUtil.getErrorForeground()

            val metrics = g2.fontMetrics
            val x = (scaledWidth - metrics.stringWidth(errorMessage)) / 2
            val y = (scaledHeight + metrics.height) / 2

            g2.drawString(errorMessage, x, y)
        } finally {
            g2.dispose()
        }

        return JBImageIcon(image)
    }

    fun isImageFile(extension: String?): Boolean {
        return extension?.lowercase() in listOf("png", "jpg", "jpeg", "gif", "bmp", "svg")
    }
}