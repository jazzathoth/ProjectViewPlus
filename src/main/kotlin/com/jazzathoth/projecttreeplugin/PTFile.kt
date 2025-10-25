package com.jazzathoth.projecttreeplugin

data class PTFile(
    val version: Int = 1,
    val dirs: MutableMap<String, MutableList<String>> = mutableMapOf()
)