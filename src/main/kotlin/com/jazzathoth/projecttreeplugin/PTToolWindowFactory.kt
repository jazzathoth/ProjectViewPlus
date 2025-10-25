package com.jazzathoth.projecttreeplugin

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import javax.swing.DropMode
import javax.swing.JPanel
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeSelectionModel

class PTToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())

        val root = PTNode.Root(project)
        val model = PTModel(root)
//        val model = AsyncTreeModel(ptModel, project)
        val tree = PTView(model, project)

        val renderer = DefaultTreeCellRenderer()
        tree.cellRenderer = renderer
        tree.showsRootHandles = true
        tree.isRootVisible = true
        tree.toggleClickCount = 2
        tree.dragEnabled = true
        tree.dropMode = DropMode.INSERT
        tree.transferHandler = ReorderTransferHandler(tree, project, root, model)
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.putClientProperty("JTree.dragEnabled", true)


        Opener.attach(tree, project)

        PopupHandler.installPopupMenu(
            tree,
            "ProjectViewPopupMenu",
            ActionPlaces.PROJECT_VIEW_POPUP
        )

        val scroll = ScrollPaneFactory.createScrollPane(tree)
        panel.add(scroll, BorderLayout.CENTER)

        val content = toolWindow.contentManager.factory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)

        // Build the tree off the UI thread
        ApplicationManager.getApplication().executeOnPooledThread {
            root.build()
            ApplicationManager.getApplication().invokeLater {
                model.reload()
                expandTop(tree)
            }
        }

        setFileListener(project, tree, root, model)
        setSelListener(tree, project)
    }

    private fun expandTop(tree: Tree) {
        for (i in 0 until tree.rowCount.coerceAtMost(30)) tree.expandRow(i)
    }
}

