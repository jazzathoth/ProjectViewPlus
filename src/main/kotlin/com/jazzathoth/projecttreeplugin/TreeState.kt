//package com.jazzathoth.projecttreeplugin
//
//import com.intellij.ui.tree.AsyncTreeModel
//import javax.swing.JTree
//import javax.swing.tree.TreePath
//
//fun refreshTree(tree: JTree, model: AsyncTreeModel) {
//    val expanded = mutableListOf<TreePath>()
//
//    for (i in 0 until tree.rowCount) {
//        val path = tree.getPathForRow(i)
//        if (tree.isExpanded(path)) {
//            expanded.add(path)
//        }
//    }
//    model.reload()
//
//    for (path in expanded) {
//        tree.expandPath(path)
//    }
//}