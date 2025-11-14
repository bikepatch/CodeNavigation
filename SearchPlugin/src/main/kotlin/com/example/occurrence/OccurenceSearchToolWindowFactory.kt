package com.example.occurrence

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.*

class OccurrenceSearchToolWindowFactory :
    ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val panel = OccurrenceSearchPanel(project)
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

private data class OccurrenceView(
    val relativePath: String,
    val line: Int,
    val offset: Int
)

private class OccurrenceListCellRenderer :
    ColoredListCellRenderer<OccurrenceView>() {

    override fun customizeCellRenderer(
        list: JList<out OccurrenceView>,
        value: OccurrenceView?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        if (value == null) return
        append(value.relativePath, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        append("   ")
        append("${value.line}:${value.offset}", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
    }
}

class OccurrenceSearchPanel(
    private val project: Project
) : JPanel(BorderLayout()), Disposable {

    private val directoryField = JBTextField().apply {
        columns = 35
        text = project.basePath ?: ""
        toolTipText = "Full path to directory to search"
    }

    private val textField = JBTextField().apply {
        columns = 25
        toolTipText = "Text to search, e.g. println("
    }

    private val startButton = JButton("Start search")
    private val cancelButton = JButton("Cancel search").apply {
        isEnabled = false
    }

    private val resultsModel = DefaultListModel<OccurrenceView>()
    private val resultsList = JBList(resultsModel).apply {
        cellRenderer = OccurrenceListCellRenderer()
        visibleRowCount = 15
        emptyText.text = "Results will appear here as they are found"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var searchJob: Job? = null

    init {
        val inputPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8, 8, 8, 8)
        }

        val dirPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = JBUI.Borders.empty(0, 0, 4, 0)
            add(JLabel("Directory: "))
            add(Box.createHorizontalStrut(4))
            add(directoryField)
        }

        val textPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = JBUI.Borders.empty(0, 0, 8, 0)
            add(JLabel("Search for: "))
            add(Box.createHorizontalStrut(4))
            add(textField)
        }

        val buttonPanel = JPanel().apply {
            border = JBUI.Borders.empty(4, 0, 0, 0)
            add(startButton)
            add(Box.createHorizontalStrut(8))
            add(cancelButton)
        }

        inputPanel.add(dirPanel)
        inputPanel.add(textPanel)
        inputPanel.add(buttonPanel)

        val scrollPane = JBScrollPane(resultsList).apply {
            minimumSize = Dimension(200, 250)
            border = JBUI.Borders.empty(4, 8, 8, 8)
        }

        add(inputPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)

        startButton.addActionListener { startSearch() }
        cancelButton.addActionListener { cancelSearch() }

        Disposer.register(project, this)
    }

    private fun startSearch() {
        val dirText = directoryField.text.trim()
        val query = textField.text.trim()

        if (dirText.isEmpty()) {
            Messages.showWarningDialog(project, "Please enter a directory path.", "Occurrence Search")
            return
        }
        if (query.isEmpty()) {
            Messages.showWarningDialog(project, "Please enter text to search.", "Occurrence Search")
            return
        }

        val path: Path = try {
            Paths.get(dirText)
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "Invalid directory path:\n$dirText", "Occurrence Search")
            return
        }

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            Messages.showErrorDialog(project, "Directory does not exist or is not a directory:\n$dirText", "Occurrence Search")
            return
        }

        searchJob?.cancel()

        resultsModel.clear()
        setUiRunning(true)

        searchJob = scope.launch {
            try {
                searchForTextOccurrences(query, path).collect { occ ->
                    val relative = try {
                        path.relativize(occ.file).toString()
                    } catch (_: IllegalArgumentException) {
                        occ.file.toString()
                    }
                    val view = OccurrenceView(
                        relativePath = relative,
                        line = occ.line,
                        offset = occ.offset
                    )
                    SwingUtilities.invokeLater {
                        resultsModel.addElement(view)
                    }
                }
            } catch (_: CancellationException) {
                // user cancelled – ignore
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "Error during search: ${e.message}",
                        "Occurrence Search"
                    )
                }
            } finally {
                SwingUtilities.invokeLater {
                    setUiRunning(false)
                }
            }
        }
    }

    private fun cancelSearch() {
        searchJob?.cancel()
    }

    private fun setUiRunning(running: Boolean) {
        startButton.isEnabled = !running
        cancelButton.isEnabled = running
        directoryField.isEnabled = !running
        textField.isEnabled = !running
    }

    override fun dispose() {
        scope.cancel()
    }
}