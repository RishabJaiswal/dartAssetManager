package com.github.rishabjaiswal.dartassetmanager.toolWindow


import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.ui.JBUI
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import java.io.File
import kotlin.coroutines.CoroutineContext


class ImageListPanel : JBPanel<ImageListPanel>(BorderLayout()), CoroutineScope {
    private val listPanel = JBPanel<JBPanel<*>>()
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    private var currentLoadingJob: Job? = null

    init {
        listPanel.layout = BoxLayout(listPanel, BoxLayout.Y_AXIS)
        add(listPanel, BorderLayout.NORTH)
        border = JBUI.Borders.empty(10)
    }

    fun dispose() {
        job.cancel()
    }

    fun loadImagesFromPackage(directory: VirtualFile) {
        // Cancel any ongoing loading
        currentLoadingJob?.cancel()

        // Clear existing content
        listPanel.removeAll()
        showLoadingIndicator()

        // Start new loading job
        currentLoadingJob = launch {
            try {
                val imageFiles = collectImageFiles(directory)

                if (imageFiles.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        listPanel.removeAll()
                        displayNoImagesMessage()
                        listPanel.revalidate()
                        listPanel.repaint()
                    }
                    return@launch
                }

                // Process images in batches
                imageFiles.chunked(5).forEach { batch ->
                    val deferreds = batch.map { file ->
                        async(Dispatchers.IO) {
                            val icon = ImageUtils.loadImage(file, 200, 200)
                            file to icon
                        }
                    }

                    // Wait for the batch to complete and update UI
                    val results = deferreds.awaitAll()

                    withContext(Dispatchers.Main) {
                        if (!isActive) return@withContext // Check if still active

                        results.forEach { (file, icon) ->
                            addImageToList(file, icon, directory)
                            // Add separator except for last item
                            if (file != imageFiles.last()) {
                                listPanel.add(JSeparator())
                                listPanel.add(Box.createVerticalStrut(10))
                            }
                        }
                        listPanel.revalidate()
                        listPanel.repaint()
                    }
                }

                // Remove loading indicator after all images are loaded
                withContext(Dispatchers.Main) {
                    removeLoadingIndicator()
                }

            } catch (e: CancellationException) {
                // Handle cancellation
                withContext(Dispatchers.Main) {
                    listPanel.removeAll()
                    listPanel.revalidate()
                    listPanel.repaint()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showError("Error loading images: ${e.message}")
                }
            }
        }
    }

    private suspend fun collectImageFiles(directory: VirtualFile): List<VirtualFile> =
        withContext(Dispatchers.IO) {
            val files = mutableListOf<VirtualFile>()
            VfsUtilCore.iterateChildrenRecursively(directory, null) { file ->
                if (!isExcludedPath(file.path) && ImageUtils.isImageFile(file.extension)) {
                    files.add(file)
                }
                true
            }
            files.sortedBy { it.name }
        }

    private fun showLoadingIndicator() {
        val loadingLabel = JBLabel("Loading images...", SwingConstants.CENTER)
        loadingLabel.name = "loadingIndicator"
        listPanel.add(loadingLabel)
        listPanel.revalidate()
        listPanel.repaint()
    }

    private fun removeLoadingIndicator() {
        listPanel.components.firstOrNull { it.name == "loadingIndicator" }?.let {
            listPanel.remove(it)
            listPanel.revalidate()
            listPanel.repaint()
        }
    }

    private fun showError(message: String) {
        listPanel.removeAll()
        listPanel.add(JBLabel(message, SwingConstants.CENTER))
        listPanel.revalidate()
        listPanel.repaint()
    }

    private fun isExcludedPath(filePath: String): Boolean {
        return Constants.EXCLUDED_PATHS.any { filePath.contains(it) }
    }

    private fun displayNoImagesMessage() {
        val messageLabel = JBLabel("No images found in this package", SwingConstants.CENTER)
        messageLabel.border = JBUI.Borders.empty(20)
        listPanel.add(messageLabel)
    }

    private fun addImageToList(file: VirtualFile, icon: Icon, packageDir: VirtualFile) {
        // Create item panel with hover support
        val itemPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(10)
            background = UIUtil.getListBackground()
            isOpaque = true  // Required for background colors to work

            // Add hover effect
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    background = UIUtil.getListSelectionBackground(false)
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                    repaint()
                }

                override fun mouseExited(e: MouseEvent) {
                    background = UIUtil.getListBackground()
                    setCursor(Cursor.getDefaultCursor())
                    repaint()
                }
            })
        }

        // Image panel with subtle border
        val imageLabel = JLabel(icon).apply {
            preferredSize = Dimension(60, 60)
            border = JBUI.Borders.customLine(Color.DARK_GRAY, 1)
            background = UIUtil.getPanelBackground()
            isOpaque = true
        }

        // Description panel
        val descriptionPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(5, 15, 5, 5)
            isOpaque = false  // Make it transparent to show parent's hover effect
        }

        // File name label
        val nameLabel = JBLabel(file.name).apply {
            font = font.deriveFont(Font.BOLD)
            foreground = UIUtil.getLabelForeground()
            alignmentX = Component.LEFT_ALIGNMENT
        }

        // Path label
        val pathLabel = JBLabel(getRelativePath(file, packageDir)).apply {
            font = font.deriveFont(font.size - 2f)
            foreground = UIUtil.getContextHelpForeground()
            alignmentX = Component.LEFT_ALIGNMENT
        }

        // Assemble the components
        descriptionPanel.add(nameLabel)
        descriptionPanel.add(Box.createVerticalStrut(5))
        descriptionPanel.add(pathLabel)

        itemPanel.add(imageLabel, BorderLayout.WEST)
        itemPanel.add(descriptionPanel, BorderLayout.CENTER)

        listPanel.add(itemPanel)
    }

    private fun getRelativePath(file: VirtualFile, packageDir: VirtualFile): String {
        return file.path.removePrefix(packageDir.path).removePrefix(File.separator)
    }
}
