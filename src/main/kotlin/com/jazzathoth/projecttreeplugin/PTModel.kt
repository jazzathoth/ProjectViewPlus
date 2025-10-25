package com.jazzathoth.projecttreeplugin


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.BaseTreeModel
import javax.swing.tree.TreePath

// Filters you can tweak
private val EXCLUDED_DIRS = setOf(".git", "target", "node_modules", "__pycache__")

fun <T> readAction(block: () -> T): T {
    return ApplicationManager.getApplication().runReadAction( ThrowableComputable { block() })
}

fun dirKey(project: Project, vf: VirtualFile): String {
    val basePth = project.basePath ?: return ""
    val absPth = vf.path
    val relPth = if (absPth.startsWith(basePth)) {
        absPth.substring(basePth.length).trimStart('/')
    } else { absPth }
    return relPth.ifEmpty { "" }
}

private fun listChildren(project: Project, base: VirtualFile): List<PTNode> {
    val index = ProjectFileIndex.getInstance(project)
    val idxInContent: Boolean = readAction {
        index.isInContent(base)
    }
    val rootName: String = readAction {
        base.name
    }
    if (!idxInContent) return emptyList()
    val rootDir = PTNode.DirNode(project, base, rootName)
    rootDir.build(index)
    return rootDir.children
}

sealed class PTNode {
    class Root(private val project: Project) : PTNode() {
        lateinit var vf: VirtualFile
        val children = mutableListOf<PTNode>()
        val name: String get() = project.name

        fun build() {
//            val base = project.baseDir ?: project.projectFile?.parent
            val base: VirtualFile? = readAction {
                project.basePath?.let { path -> LocalFileSystem.getInstance().findFileByPath(path) }
                    ?: project.projectFile?.parent
            }
            if (base != null) {
                this.vf = base
                children.clear()
                children += listChildren(project, base)
            }
        }
        fun kids(): List<PTNode> = children
        override fun toString(): String = name
    }
    class DirNode(val project: Project, val vf: VirtualFile, val name: String) : PTNode() {
        val children = mutableListOf<PTNode>()

        fun build(index: ProjectFileIndex) {
            val childNodes = readAction {
                vf.children.orEmpty().mapNotNull { child ->
                    if (!child.isValid) return@mapNotNull null
                    if (!index.isInContent(child)) return@mapNotNull null

                    val childName = child.name
                    if (child.isDirectory && childName in EXCLUDED_DIRS) return@mapNotNull null

                    if (child.isDirectory) {
                        val dn = DirNode(project, child, childName)
                        dn.build(index)
                        dn
                    } else {
                        FileNode(child, childName)
                    }
                }
            }

            children.addAll(childNodes)
            children.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) {
                when (it) {
                    is DirNode -> it.name
                    is FileNode -> it.name
                    else -> ""
                }
            })
            applyOrder(project.projectTreeTool().load())
        }

        private fun applyOrder(order: PTFile) {
            val key = dirKey(project, vf)
            val list = order.dirs[key] ?: return
            val pos = list.withIndex().associate { it.value to it.index }

            children.sortWith { a, b ->
                val na = when (a) {
                    is DirNode -> a.vf.name
                    is FileNode -> a.vf.name
                    else -> ""
                }
                val nb = when (b) {
                    is DirNode -> b.vf.name
                    is FileNode -> b.vf.name
                    else -> ""
                }
                val ia = pos[na]
                val ib = pos[nb]
                when {
                    ia != null && ib != null -> ia.compareTo(ib)
                    ia != null -> -1
                    ib != null -> 1
                    else -> String.CASE_INSENSITIVE_ORDER.compare(na, nb)
                }
            }
        }
        override fun toString(): String = name
    }
    class FileNode(val vf: VirtualFile, val name: String) : PTNode() {
        override fun toString(): String = name
    }
}


class PTModel(private val root: PTNode.Root) : BaseTreeModel<PTNode>() {
    fun reload() = treeStructureChanged(null, null, null)

    fun treeChanged(parent: TreePath, oldIdx: Int, newIdx: Int, node: PTNode) {
        treeNodesRemoved(parent, intArrayOf(oldIdx), arrayOf(node))
        treeNodesInserted(parent, intArrayOf(newIdx), arrayOf(node))
//        treeStructureChanged(parent, null, null)
    }

    fun treeRemEvt(parent: TreePath, oldIdx: Int, node: PTNode) {
        treeNodesRemoved(parent, intArrayOf(oldIdx), arrayOf(node))
    }

    fun treeAddEvt(parent: TreePath, newIdx: Int, node: PTNode) {
        treeNodesInserted(parent, intArrayOf(newIdx), arrayOf(node))
    }

    override fun getRoot(): Any = root
    override fun getChildren(parent: Any?): List<PTNode> =
        when (parent) {
            is PTNode.Root -> parent.kids()
            is PTNode.DirNode -> parent.children
            else -> emptyList()
        }

    override fun getChild(parent: Any?, index: Int): Any? =
        when (parent) {
            is PTNode.Root -> parent.kids().getOrNull(index)
            is PTNode.DirNode -> parent.children.getOrNull(index)
            else -> null
        }
    override fun getChildCount(parent: Any?): Int =
        when (parent) {
            is PTNode.Root -> parent.kids().size
            is PTNode.DirNode -> parent.children.size
            else -> 0
        }
    override fun isLeaf(node: Any?): Boolean = node is PTNode.FileNode
    override fun getIndexOfChild(parent: Any?, child: Any?): Int =
        when (parent) {
            is PTNode.Root -> parent.kids().indexOf(child)
            is PTNode.DirNode -> parent.children.indexOf(child)
            else -> -1
        }
}