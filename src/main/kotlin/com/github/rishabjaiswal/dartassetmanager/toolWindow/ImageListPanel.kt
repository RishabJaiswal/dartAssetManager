package com.github.rishabjaiswal.dartassetmanager.toolWindow


import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.ui.JBUI
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import kotlinx.coroutines.*
import java.awt.*
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
        val itemPanel = JBPanel<JBPanel<*>>(BorderLayout())
        itemPanel.border = JBUI.Borders.empty(10)

        // Image label
        val imageLabel = JLabel(icon)
        imageLabel.preferredSize = Dimension(50, 50)
        imageLabel.border = JBUI.Borders.empty(5)

        // Description panel
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
