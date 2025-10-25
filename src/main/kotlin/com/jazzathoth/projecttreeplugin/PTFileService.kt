package com.jazzathoth.projecttreeplugin

import com.google.gson.GsonBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class PTFileService(private val project: Project) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val filename = ".projecttree.json"

    @Volatile private var ptfile: PTFile? = null

    fun load(): PTFile {
        ptfile?.let { return it }
        val base = project.basePath ?: return PTFile()
        val vf = LocalFileSystem.getInstance()
            .findFileByPath(Path.of(base, filename).toString())
        if (vf != null && vf.exists()) {
            vf.inputStream.use { ins ->
                BufferedReader(InputStreamReader(ins, StandardCharsets.UTF_8)).use {
                    val text = it.readText()
                    val json = try {
                        gson.fromJson(text, PTFile::class.java).also { mo -> ptfile = mo }
                    } catch (e: Exception) {
                        null
                    }
                    return (json ?: PTFile()).also { mo -> ptfile = mo}
                }
            }
        }
        return PTFile().also { ptfile = it }
    }

    fun saveAsync(updated: PTFile) {
        ptfile = updated
        val base = project.basePath ?: return
        ApplicationManager.getApplication().executeOnPooledThread {
            val nioPath: Path = Path.of(base, filename)

            var fileVf: VirtualFile? = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(nioPath)

            if (fileVf == null) {
                var tempVf: VirtualFile? = null
                ApplicationManager.getApplication().invokeAndWait {
                    WriteAction.run<RuntimeException> {
                        val parentDir = VfsUtil.createDirectories(nioPath.parent.toString())
                        tempVf = parentDir.findChild(nioPath.fileName.toString())
                            ?: parentDir.createChildData(this@PTFileService, nioPath.fileName.toString())
                    }
                }
                fileVf = tempVf
            }



            val text = gson.toJson(updated)

            ApplicationManager.getApplication().invokeAndWait {
                WriteAction.run<RuntimeException> {
                    VfsUtil.saveText(fileVf!!, text)
                }
            }
            fileVf!!.refresh(false, false)

        }
    }
}