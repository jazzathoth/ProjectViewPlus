package com.jazzathoth.projecttreeplugin

import com.intellij.ide.CopyPasteDelegator
import com.intellij.ide.IdeView
import com.intellij.ide.util.DeleteHandler
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.ui.treeStructure.Tree

class PTView(model: PTModel, val project: Project) : Tree(model), UiDataProvider {
    @Volatile var psiFilePtr: SmartPsiElementPointer<PsiFile>? = null
    @Volatile var psiDirPtr: SmartPsiElementPointer<PsiDirectory>? = null
    private val copyPaste = CopyPasteDelegator(project, this)

    override fun uiDataSnapshot(sink: DataSink) {
        val node = lastSelectedPathComponent as? PTNode ?: return
        val vf = when (node) {
            is PTNode.DirNode -> node.vf
            is PTNode.FileNode -> node.vf
            else -> null
        }

        if (vf == null) return
        sink[CommonDataKeys.PROJECT] = project
        sink[CommonDataKeys.VIRTUAL_FILE] = vf
        sink[CommonDataKeys.VIRTUAL_FILE_ARRAY] = arrayOf(vf)
        sink[CommonDataKeys.NAVIGATABLE] = PsiNavigationSupport
            .getInstance()
            .createNavigatable(project, vf, 0)

        sink[PlatformDataKeys.COPY_PROVIDER] = copyPaste.copyProvider
        sink[PlatformDataKeys.CUT_PROVIDER] = copyPaste.cutProvider
        sink[PlatformDataKeys.PASTE_PROVIDER] = copyPaste.pasteProvider

        sink[PlatformDataKeys.DELETE_ELEMENT_PROVIDER] = DeleteHandler.DefaultDeleteProvider()

        sink.lazy(LangDataKeys.PSI_ELEMENT_ARRAY) {
            val file = psiFilePtr?.element
            val dir = psiDirPtr?.element
            val psi = file ?: dir
            if (psi != null) arrayOf(psi) else null
        }

        psiFilePtr?.element?.let { psiFile ->
            sink.lazy(CommonDataKeys.PSI_FILE) { psiFile }
            sink.lazy(CommonDataKeys.PSI_ELEMENT) { psiFile }
        }

        psiDirPtr?.element?.let { psiDir ->
            sink.lazy(CommonDataKeys.PSI_ELEMENT) { psiDir }
        }

        val targetDir: PsiDirectory? = when {
            psiDirPtr?.element != null -> psiDirPtr!!.element
            psiFilePtr?.element?.containingDirectory != null -> psiFilePtr!!.element!!.containingDirectory
            else -> null
        }

        targetDir?.let { dir ->
            sink[LangDataKeys.IDE_VIEW] = PTIdeView( dir )
            sink.lazy(CommonDataKeys.PSI_ELEMENT) { dir }
        }
    }
}

private class PTIdeView(
    private val dir: PsiDirectory
) : IdeView {
    override fun getDirectories(): Array<out PsiDirectory> = arrayOf(dir)
    override fun getOrChooseDirectory(): PsiDirectory? = dir
    override fun selectElement(element: PsiElement) {
        (element as? Navigatable)?.navigate(true)
    }
}