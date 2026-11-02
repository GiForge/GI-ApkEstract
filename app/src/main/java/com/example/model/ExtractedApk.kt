package com.example.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExtractedApk(
    val file: File,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val packageName: String? = null,
    val appName: String? = null,
    val versionName: String? = null
) {
    val formattedSize: String
        get() = AppInfo.formatFileSize(sizeBytes)

    val formattedDate: String
        get() {
            if (lastModified <= 0) return "-"
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }
}
