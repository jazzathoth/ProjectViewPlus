package com.jazzathoth.projecttreeplugin

import com.intellij.openapi.project.Project

fun Project.projectTreeTool(): PTFileService =
    this.getService(PTFileService::class.java)