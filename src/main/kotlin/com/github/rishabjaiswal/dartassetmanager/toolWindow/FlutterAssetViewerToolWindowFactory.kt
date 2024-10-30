package com.github.rishabjaiswal.dartassetmanager.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.*


class FlutterAssetViewerToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val assetViewerContent = contentFactory.createContent(
            FlutterAssetViewerPanel(project),
            "Images",
            false
        )
        toolWindow.contentManager.addContent(assetViewerContent)
    }
}

class FlutterAssetViewerPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val contentPanel = JPanel(BorderLayout())
    private val packageComboBox = ComboBox<PackageInfo>()
    private val packageService = PackageService(project)
    private val imageListPanel = ImageListPanel(project)

    init {
        setupUI()
        loadPackages()
    }

    private fun setupUI() {
        val toolbarPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        toolbarPanel.border = JBUI.Borders.empty(5)

        packageComboBox.apply {
            renderer = SimpleListCellRenderer.create("") { it?.name ?: "" }
            addItemListener { event ->
                if (event.stateChange == ItemEvent.SELECTED) {
                    (event.item as? PackageInfo)?.let { updateImageDisplay(it) }
                }
            }
        }

        toolbarPanel.add(JLabel("Select Package:"))
        toolbarPanel.add(packageComboBox)

        // Use JBScrollPane for better scrolling behavior
        val scrollPane = JBScrollPane(imageListPanel).apply {
            verticalScrollBar.unitIncrement = 16
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }

        contentPanel.add(toolbarPanel, BorderLayout.NORTH)
        contentPanel.add(scrollPane, BorderLayout.CENTER)

        add(contentPanel, BorderLayout.CENTER)
    }

    private fun loadPackages() {
        val packages = packageService.loadFlutterPackages()

        val model = DefaultComboBoxModel<PackageInfo>()
        packages.forEach { model.addElement(it) }
        packageComboBox.model = model

        if (packages.isEmpty()) {
            model.addElement(
                PackageInfo("No packages found", null, null, PubspecAssets())
            )
            packageComboBox.isEnabled = false
        } else {
            packageComboBox.selectedIndex = 0
            packages.first().let { imageListPanel.loadImagesFromPackage(it) }
        }
    }

    private fun updateImageDisplay(packageInfo: PackageInfo) {
        packageInfo.directory?.let {
            imageListPanel.loadImagesFromPackage(packageInfo)
        } ?: run {
            imageListPanel.removeAll()
            val messageLabel = JLabel("No packages available", SwingConstants.CENTER)
            messageLabel.border = JBUI.Borders.empty(20)
            imageListPanel.add(messageLabel)
            imageListPanel.revalidate()
            imageListPanel.repaint()
        }
    }
}