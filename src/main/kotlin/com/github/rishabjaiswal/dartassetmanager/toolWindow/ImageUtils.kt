package com.github.rishabjaiswal.dartassetmanager.toolWindow

import java.awt.Image
import javax.swing.ImageIcon

object ImageUtils {
    fun scaleImage(icon: ImageIcon, targetWidth: Int, targetHeight: Int): ImageIcon {
        val img = icon.image
        var width = img.getWidth(null)
        var height = img.getHeight(null)

        if (width <= 0 || height <= 0) {
            return icon
        }

        if (width > targetWidth || height > targetHeight) {
            val widthRatio = targetWidth.toDouble() / width
            val heightRatio = targetHeight.toDouble() / height
            val ratio = minOf(widthRatio, heightRatio)

            width = (width * ratio).toInt()
            height = (height * ratio).toInt()

            val scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH)
            return ImageIcon(scaledImg)
        }

        return icon
    }

    fun isImageFile(extension: String?): Boolean {
        return extension?.toLowerCase() in listOf("png", "jpg", "jpeg", "gif", "bmp", "svg")
    }
}