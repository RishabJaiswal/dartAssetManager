package com.github.rishabjaiswal.dartassetmanager.toolWindow


data class PubspecAssets(
    val assets: List<String> = emptyList()
) {
    fun isAssetBundled(relativePath: String): Boolean {
        return assets.any { pattern ->
            when {
                // Directory pattern (ends with /)
                pattern.endsWith("/") -> relativePath.startsWith(pattern)
                // Specific file pattern
                else -> relativePath == pattern || relativePath.startsWith("$pattern/")
            }
        }
    }
}