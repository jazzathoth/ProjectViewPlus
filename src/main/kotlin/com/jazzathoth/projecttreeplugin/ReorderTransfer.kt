package com.jazzathoth.projecttreeplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.TransferHandler
import javax.swing.tree.TreePath

class ReorderTransferHandler(
    private val tree: PTView,
    private val project: Project,
    private val root: PTNode.Root,
//    private val model: AsyncTreeModel
    private val model: PTModel
) : TransferHandler() {
    private val pathFlavor = DataFlavor(
        DataFlavor.javaJVMLocalObjectMimeType + ";class=" + TreePath::class.java.name,
        "TreePath")

    override fun getSourceActions(c: JComponent?) = MOVE

    override fun createTransferable(c: JComponent?): Transferable? {
        val path = tree.selectionPath ?: return null
        return object : Transferable {
            override fun getTransferDataFlavors(): Array<out DataFlavor?> {
                return arrayOf(pathFlavor)
            }

            override fun isDataFlavorSupported(p0: DataFlavor?): Boolean {
                return p0 == pathFlavor
            }

            override fun getTransferData(p0: DataFlavor?): Any {
                return path
            }
        }
    }

    override fun canImport(support: TransferSupport): Boolean {
        if (!support.isDrop || !support.isDataFlavorSupported(pathFlavor)) return false

        val from = support.transferable.getTransferData(pathFlavor) as TreePath
        val toLoc = support.dropLocation as JTree.DropLocation
        val toPath = toLoc.path

        if (toLoc.childIndex == -1) return false

        val parent = from.parentPath?.lastPathComponent ?: return false

        if (!(parent is PTNode.DirNode || parent is PTNode.Root)) return false

        val toParent: TreePath? = when {
            toPath != null -> toPath
            else -> null
        }
        val result = from.parentPath == toParent
        if (result && toPath != null && from.isDescendant(toPath)) return false

        return result
    }

    override fun importData(support: TransferSupport): Boolean {
        if (!canImport(support)) return false
        val from = support.transferable.getTransferData(pathFlavor) as TreePath
        val toLoc = support.dropLocation as JTree.DropLocation
//        val parent = from.parentPath.lastPathComponent as PTNode.DirNode
//        val list = parent.children
        val parent = from.parentPath.lastPathComponent as PTNode
        val list = when (parent) {
            is PTNode.DirNode -> parent.children
            is PTNode.Root -> parent.children
            else -> return false
        }

        val dragged = from.lastPathComponent as PTNode
        val oldIdx = list.indexOf(dragged)
        var newIdx = toLoc.childIndex
        if (newIdx < 0) newIdx = list.size
        if (oldIdx == -1) return false
        if (newIdx > oldIdx) newIdx --

        val refNode: TreePath? = toLoc.path

        if (refNode != null) {
            val vf = when (parent) {
                is PTNode.DirNode -> parent.vf
                is PTNode.Root -> parent.vf
                else -> return false
            }
            ApplicationManager.getApplication().invokeLater {

                list.removeAt(oldIdx)
                updateList(list, vf)
                model.treeRemEvt(refNode, oldIdx, dragged)

                list.add(newIdx, dragged)
                updateList(list, vf)
                model.treeAddEvt(refNode, newIdx, dragged)
            }
        }
        return true
    }

    private fun updateList(list: MutableList<PTNode>, lookupVf: VirtualFile) {
        val names = list.map {
            when (it) {
                is PTNode.DirNode -> it.vf.name
                is PTNode.FileNode -> it.vf.name
                else -> ""
            }
        }.toMutableList()

        val svc = project.projectTreeTool()
        val mo = svc.load()
        mo.dirs[dirKey(project, lookupVf)] = names
        svc.saveAsync(mo)
    }
}