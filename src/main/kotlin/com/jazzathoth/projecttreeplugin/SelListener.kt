package com.jazzathoth.projecttreeplugin

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.concurrency.AppExecutorUtil

fun setSelListener(tree: PTView, project: Project) {
    tree.addTreeSelectionListener {
        val node = tree.lastSelectedPathComponent as? PTNode ?: return@addTreeSelectionListener
        val vf = when (node) {
            is PTNode.FileNode -> node.vf
            is PTNode.DirNode -> node.vf
            is PTNode.Root -> node.vf
        }

        if (!vf.isValid) { tree.psiFilePtr = null; tree.psiDirPtr = null; return@addTreeSelectionListener}

        ReadAction.nonBlocking<Pair<SmartPsiElementPointer<PsiDirectory>?, SmartPsiElementPointer<PsiFile>?>> {
            val psiMgr = PsiManager.getInstance(project)
            if (vf.isDirectory) {
                psiMgr.findDirectory(vf)?.let {dir ->
//                    SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it) to null
                    val dirPtr = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(dir)
                    Pair(dirPtr, null)
                }
            } else {
                psiMgr.findFile(vf)?.let {file ->
//                    null to SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it)
                    val filePtr = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(file)
                    val dir = file.containingDirectory
                    val dirPtr = dir?.let { SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it) }
                    Pair(dirPtr, filePtr)
                }
            }
        }
            .coalesceBy(tree)
            .expireWith(project)
            .finishOnUiThread(ModalityState.any()) { (dirPtr, filePtr) ->
                tree.psiDirPtr = dirPtr
                tree.psiFilePtr = filePtr
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }
}