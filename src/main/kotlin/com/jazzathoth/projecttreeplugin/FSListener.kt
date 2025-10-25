package com.jazzathoth.projecttreeplugin

import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.AppExecutorUtil

fun setFileListener(project: Project, tree: Tree, root: PTNode.Root, model: PTModel) {
    val conn = project.messageBus.connect()
    conn.subscribe(VirtualFileManager.VFS_CHANGES, object: BulkFileListener {
        private var rebuildQd = false

        override fun after(ev: List<VFileEvent>) {

            val rootPath = root.vf.path
            val inProj = ev.any { it.path.startsWith(rootPath) }


            if (!inProj) return

            if (rebuildQd) return
            rebuildQd = true

            ReadAction.nonBlocking<Unit> {
                root.build()
            }
                .inSmartMode(project)
                .expireWith(project)
                .finishOnUiThread(ModalityState.any()) {
                    rebuildQd = false
                    val state = TreeState.createOn(tree)
                    val selPath = tree.selectionPath
                    model.reload()
                    state.applyTo(tree)
                    if (selPath != null) tree.selectionPath = selPath
                }
                .submit(AppExecutorUtil.getAppExecutorService())
        }
    })
}