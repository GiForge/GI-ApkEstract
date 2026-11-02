package com.example.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val sourceDir: String,
    val splitSourceDirs: List<String> = emptyList(),
    val apkSizeBytes: Long,
    val isSystemApp: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val minSdkVersion: Int,
    val targetSdkVersion: Int
) {
    val formattedSize: String
        get() = formatFileSize(apkSizeBytes)

    val formattedLastUpdate: String
        get() = formatDate(lastUpdateTime)

    val formattedInstallTime: String
        get() = formatDate(firstInstallTime)

    val safeFileName: String
        get() {
            val cleanName = appName.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            val cleanVersion = versionName.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            return "${cleanName}_v${cleanVersion}.apk"
        }

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
                else -> "$bytes B"
            }
        }

        fun formatDate(timestamp: Long): String {
            if (timestamp <= 0) return "-"
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}

enum class AppFilter {
    ALL,
    USER,
    SYSTEM
}

enum class AppSortOrder {
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC,
    SIZE_ASC,
    DATE_DESC
}

data class ExtractionResult(
    val success: Boolean,
    val savedFile: File?,
    val message: String,
    val app: AppInfo
)

data class BatchProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentAppName: String = "",
    val isFinished: Boolean = false,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val extractedFiles: List<File> = emptyList()
) {
    val progressFraction: Float
        get() = if (total > 0) current.toFloat() / total.toFloat() else 0f
}
