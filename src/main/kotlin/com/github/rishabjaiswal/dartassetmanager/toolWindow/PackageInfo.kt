package com.github.rishabjaiswal.dartassetmanager.toolWindow

import com.intellij.openapi.vfs.VirtualFile

data class PackageInfo(
    val name: String,
    val directory: VirtualFile?,
    val pubspecFile: VirtualFile?
) {
    override fun toString(): String = name
}
