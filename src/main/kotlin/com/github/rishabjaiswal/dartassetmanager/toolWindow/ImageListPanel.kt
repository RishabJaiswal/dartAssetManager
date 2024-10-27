package com.github.rishabjaiswal.dartassetmanager.toolWindow


import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.coroutines.CoroutineContext

class ImageListPanel(private val project: Project) : JBPanel<ImageListPanel>(BorderLayout()), CoroutineScope {
    companion object {
        private const val IMAGE_SIZE = 60
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    private val listPanel = JBPanel<JBPanel<*>>()
    private val searchField = SearchTextField().apply {
        textEditor.emptyText.text = "Search images..."
    }
    private val toolbarPanel = JPanel(FlowLayout(FlowLayout.LEFT))
    private var currentLoadingJob: Job? = null
    private var searchJob: Job? = null
    private var allImageFiles = mutableListOf<VirtualFile>()
    private var currentPackageDir: VirtualFile? = null

    init {
        setupUI()
        setupSearch()
    }

    fun dispose() {
        searchJob?.cancel()
        job.cancel()
    }

    private fun setupUI() {
        // Setup toolbar with search
        toolbarPanel.apply {
            border = JBUI.Borders.empty(8)
            background = UIUtil.getPanelBackground()
            add(searchField)
        }

        // Setup list panel with Wrapper to prevent stretching
        listPanel.apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(10)
        }

        // Create a wrapper panel that aligns to the top
        val wrapperPanel = JPanel(BorderLayout()).apply {
            add(listPanel, BorderLayout.NORTH)  // Add listPanel to the NORTH to prevent stretching
        }

        // Add components to main panel
        add(toolbarPanel, BorderLayout.NORTH)
        add(JBScrollPane(wrapperPanel), BorderLayout.CENTER)
    }

    private fun setupSearch() {
        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = debounceSearch()
            override fun removeUpdate(e: DocumentEvent) = debounceSearch()
            override fun changedUpdate(e: DocumentEvent) = debounceSearch()
        })
    }

    fun loadImagesFromPackage(directory: VirtualFile) {
        currentPackageDir = directory

        // Cancel any ongoing loading
        currentLoadingJob?.cancel()

        // Clear existing content
        listPanel.removeAll()
        allImageFiles.clear()
        showLoadingIndicator()

        // Start new loading job
        currentLoadingJob = launch {
            try {
                allImageFiles = collectImageFiles(directory).toMutableList()
                filterImages()
            } catch (e: CancellationException) {
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

    private fun debounceSearch() {
        searchJob?.cancel() // Cancel any ongoing search
        searchJob = launch {
            delay(500) // Wait for 500ms
            filterImages()
        }
    }

    private fun filterImages() {
        val searchText = searchField.text.trim().lowercase()
        listPanel.removeAll()

        launch {
            try {
                // Show loading state immediately for better UX
                withContext(Dispatchers.Main) {
                    if (searchText.isNotEmpty()) {
                        showLoadingIndicator()
                    }
                }

                val filteredFiles = if (searchText.isEmpty()) {
                    allImageFiles
                } else {
                    allImageFiles.filter { file ->
                        file.name.lowercase().contains(searchText) ||
                                getRelativePath(file, currentPackageDir!!).lowercase().contains(searchText)
                    }
                }

                withContext(Dispatchers.Main) {
                    listPanel.removeAll()
                }

                if (filteredFiles.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        showNoImagesMessage(if (searchText.isEmpty()) "No images found" else "No matching images found")
                    }
                    return@launch
                }

                // Process images in batches
                filteredFiles.chunked(5).forEach { batch ->
                    val deferreds = batch.map { file ->
                        async(Dispatchers.IO) {
                            val icon = ImageUtils.loadImage(file, IMAGE_SIZE, IMAGE_SIZE)
                            file to icon
                        }
                    }

                    val results = deferreds.awaitAll()

                    withContext(Dispatchers.Main) {
                        if (!isActive) return@withContext

                        results.forEach { (file, icon) ->
                            // Wrap each item in a Wrapper to maintain its natural height
                            val itemWrapper = Wrapper(addImageToList(file, icon, currentPackageDir!!))
                            listPanel.add(itemWrapper)
                            if (file != filteredFiles.last()) {
                                listPanel.add(Box.createVerticalStrut(1))
                                listPanel.add(JSeparator())
                                listPanel.add(Box.createVerticalStrut(1))
                            }
                        }
                        listPanel.revalidate()
                        listPanel.repaint()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showError("Error filtering images: ${e.message}")
                }
            }
        }
    }

    private fun addImageToList(file: VirtualFile, icon: Icon, packageDir: VirtualFile): JPanel {
        val itemPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(8)
            background = UIUtil.getListBackground()
            isOpaque = true

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

                override fun mouseClicked(e: MouseEvent) {
                    // Open image in Android Studio's editor
                    FileEditorManager.getInstance(project).openFile(file, true)
                }
            })
        }

        // Image label
        val imageLabel = JLabel(icon).apply {
            preferredSize = Dimension(IMAGE_SIZE, IMAGE_SIZE)
            minimumSize = Dimension(IMAGE_SIZE, IMAGE_SIZE)
            maximumSize = Dimension(IMAGE_SIZE, IMAGE_SIZE)
            border = JBUI.Borders.customLine(UIUtil.TRANSPARENT_COLOR, 1)
            background = UIUtil.getPanelBackground()
            isOpaque = true
        }

        // Description panel
        val descriptionPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(2, 15, 2, 5)
            isOpaque = false
            alignmentY = Component.CENTER_ALIGNMENT
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

        descriptionPanel.add(nameLabel)
        descriptionPanel.add(Box.createVerticalStrut(4))
        descriptionPanel.add(pathLabel)

        val imagePanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            isOpaque = false
            add(imageLabel, BorderLayout.CENTER)
        }

        itemPanel.add(imagePanel, BorderLayout.WEST)
        itemPanel.add(descriptionPanel, BorderLayout.CENTER)

        return itemPanel
    }

    private fun showLoadingIndicator() {
        val loadingLabel = JBLabel("Loading images...", SwingConstants.CENTER)
        loadingLabel.name = "loadingIndicator"
        listPanel.add(loadingLabel)
        listPanel.revalidate()
        listPanel.repaint()
    }

    private fun showNoImagesMessage(message: String) {
        val messageLabel = JBLabel(message, SwingConstants.CENTER)
        messageLabel.border = JBUI.Borders.empty(20)
        listPanel.add(messageLabel)
        listPanel.revalidate()
        listPanel.repaint()
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

    private fun getRelativePath(file: VirtualFile, packageDir: VirtualFile): String {
        return file.path.removePrefix(packageDir.path).removePrefix(File.separator)
    }
}
