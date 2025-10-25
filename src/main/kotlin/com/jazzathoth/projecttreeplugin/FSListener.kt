package com.jazzathoth.projecttreeplugin

import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.AppExecutorUtil
import com.jetbrains.rd.util.printlnError

fun setFileListener(project: Project, tree: Tree, root: PTNode.Root, model: PTModel) {
    val conn = project.messageBus.connect()
    conn.subscribe(VirtualFileManager.VFS_CHANGES, object: BulkFileListener {
        private var rebuildQd = false

        override fun after(ev: List<VFileEvent>) {
            println("[after] ${ev.joinToString { it.javaClass.simpleName+':'+it.path }}")
//            val fileIdx = ProjectFileIndex.getInstance(project)
//            val inProj = ev.any {e ->
//                val vf = when(e) {
//                    is VFileCreateEvent -> e.file
//                    is VFileDeleteEvent -> e.file
//                    is VFileMoveEvent -> e.file
//                    is VFilePropertyChangeEvent ->
//                        if (VirtualFile.PROP_NAME == e.propertyName) e.file else null
//                    is VFileCopyEvent -> e.file
//                    else -> null
//                }
//
//                vf != null && ReadAction.compute<Boolean, RuntimeException> {
//                    fileIdx.isInContent(vf) && !fileIdx.isExcluded(vf)
//                }
//            }

            val rootPath = root.vf.path
            val inProj = ev.any { it.path.startsWith(rootPath) }

            println("[after] inProj: $inProj")

            if (!inProj) return

            if (rebuildQd) {println("[after] skipped because busy"); return}
            rebuildQd = true

            println("[after] ready to rebuild tree")
            ReadAction.nonBlocking<Unit> {
                println("[after] starting root.build() in ReadAction.nonBlocking")
                root.build()
                println("[after] done with root.build()")
            }
                .inSmartMode(project)
                .expireWith(project)
                .finishOnUiThread(ModalityState.any()) {
                    rebuildQd = false
                    val state = TreeState.createOn(tree)
                    val selPath = tree.selectionPath
                    println("[after] ready to reload model")
                    model.reload()
                    println("[after] done with model.reload()")
                    state.applyTo(tree)
                    println("[after] applied state")
                    if (selPath != null) tree.selectionPath = selPath
                }
                .submit(AppExecutorUtil.getAppExecutorService())
        }
    })
}