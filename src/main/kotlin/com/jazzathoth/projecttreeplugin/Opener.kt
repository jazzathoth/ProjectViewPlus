package com.jazzathoth.projecttreeplugin

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.Tree
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.tree.TreePath


object Opener {
    fun attach(tree: Tree, project: Project) {
        tree.inputMap.put(KeyStroke.getKeyStroke("ENTER"), "pt-open-or-toggle")

        tree.actionMap.put("pt-open-or-toggle", object : AbstractAction() {
            override fun actionPerformed(p0: ActionEvent?) {
                val path = tree.selectionPath ?: return
                handlePath(tree, project, path)
            }
        })

        tree.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    val path = tree.getPathForLocation(e.x, e.y) ?: return
                    handlePath(tree, project, path, toggleDirs = false)
                }
            }
        })
    }

    private fun handlePath(tree: Tree, project: Project, path: TreePath, toggleDirs: Boolean = true) {
        val node = path.lastPathComponent as? PTNode ?: return
        when (node) {
            is PTNode.FileNode -> {
                WriteCommandAction.runWriteCommandAction(project) {
                    OpenFileDescriptor(project, node.vf).navigate(true)
                }
            }
            is PTNode.DirNode -> if (toggleDirs) {
                if (tree.isExpanded(path)) tree.collapsePath(path) else tree.expandPath(path)
            }
            else -> ""
        }
    }
}