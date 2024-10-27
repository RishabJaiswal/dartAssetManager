package com.github.rishabjaiswal.dartassetmanager.toolWindow

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import com.intellij.ui.scale.JBUIScale
import javax.swing.border.EmptyBorder
import com.intellij.util.ui.UIUtil
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

class GridPanel : JPanel() {
    private companion object {
        const val COLUMNS = 4
        const val DEFAULT_GAP = 10
    }

    // Panel that will contain all the grid items
    private val contentPanel = JPanel().apply {
        layout = GridBagLayout()
        border = JBUI.Borders.empty(DEFAULT_GAP)
    }

    // Current row and column for adding items
    private var currentRow = 0
    private var currentCol = 0

    init {
        layout = BorderLayout()

        // Wrap content in scroll pane
        val scrollPane = JBScrollPane(contentPanel).apply {
            border = null
            viewport.background = UIUtil.getPanelBackground()
        }
        add(scrollPane, BorderLayout.CENTER)

        // Add component listener to handle resize
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                revalidateGrid()
            }
        })
    }

    /**
     * Adds a component to the grid.
     * @param component The component to add
     * @param fillWidth Whether the component should fill the cell width
     */
    fun addItem(component: Component, fillWidth: Boolean = true) {
        val constraints = GridBagConstraints().apply {
            gridx = currentCol
            gridy = currentRow
            fill = if (fillWidth) GridBagConstraints.BOTH else GridBagConstraints.NONE
            anchor = GridBagConstraints.CENTER
            weightx = 1.0
            weighty = 1.0
            insets = JBUI.insets(DEFAULT_GAP / 2)
        }

        contentPanel.add(component, constraints)

        // Update position for next item
        currentCol++
        if (currentCol >= COLUMNS) {
            currentCol = 0
            currentRow++
        }

        revalidateGrid()
    }

    /**
     * Adds multiple components to the grid.
     * @param components List of components to add
     */
    fun addItems(components: List<JComponent>) {
        components.forEach { addItem(it) }
    }

    /**
     * Clears all items from the grid.
     */
    fun clear() {
        contentPanel.removeAll()
        currentRow = 0
        currentCol = 0
        revalidateGrid()
    }

    /**
     * Sets the gap between grid items.
     * @param gap The gap size in pixels
     */
    fun setGap(gap: Int) {
        contentPanel.border = JBUI.Borders.empty(gap)
        contentPanel.components.forEach { component ->
            (component.doLayout() as? GridBagLayout)?.getConstraints(component)?.insets = JBUI.insets(gap / 2)
        }
        revalidateGrid()
    }

    private fun revalidateGrid() {
        contentPanel.revalidate()
        contentPanel.repaint()
    }
}