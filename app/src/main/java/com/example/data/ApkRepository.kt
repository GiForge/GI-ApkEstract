package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.model.AppFilter
import com.example.model.AppInfo
import com.example.model.AppSortOrder
import com.example.model.BatchProgress
import com.example.model.ExtractedApk
import com.example.model.ExtractionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ApkRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun getExtractionDirectory(): File {
        // Preferred location: Public Downloads folder / ApkExtractor
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val target = File(downloadsDir, "ApkExtractor")
        if (!target.exists()) {
            target.mkdirs()
        }
        if (target.exists() && target.canWrite()) {
            return target
        }
        // Fallback: App specific external files dir
        val fallback = context.getExternalFilesDir("ApkExtractor")
            ?: File(context.filesDir, "ApkExtractor")
        if (!fallback.exists()) {
            fallback.mkdirs()
        }
        return fallback
    }

    suspend fun getInstalledApps(
        filter: AppFilter = AppFilter.ALL,
        searchQuery: String = "",
        sortOrder: AppSortOrder = AppSortOrder.NAME_ASC
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            packageManager.getInstalledPackages(0)
        }

        val appList = mutableListOf<AppInfo>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val sourceDir = appInfo.sourceDir ?: continue
            val apkFile = File(sourceDir)
            if (!apkFile.exists()) continue

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            // Apply filter
            when (filter) {
                AppFilter.USER -> if (isSystem) continue
                AppFilter.SYSTEM -> if (!isSystem) continue
                AppFilter.ALL -> { /* include all */ }
            }

            val appName = try {
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg.packageName
            }

            // Apply search query
            if (searchQuery.isNotBlank()) {
                val query = searchQuery.lowercase().trim()
                val matchName = appName.lowercase().contains(query)
                val matchPackage = pkg.packageName.lowercase().contains(query)
                if (!matchName && !matchPackage) continue
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }

            val minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appInfo.minSdkVersion
            } else {
                0
            }

            val splits = appInfo.splitSourceDirs?.toList() ?: emptyList()

            appList.add(
                AppInfo(
                    packageName = pkg.packageName,
                    appName = appName,
                    versionName = pkg.versionName ?: "1.0",
                    versionCode = versionCode,
                    sourceDir = sourceDir,
                    splitSourceDirs = splits,
                    apkSizeBytes = apkFile.length(),
                    isSystemApp = isSystem,
                    firstInstallTime = pkg.firstInstallTime,
                    lastUpdateTime = pkg.lastUpdateTime,
                    minSdkVersion = minSdkVersion,
                    targetSdkVersion = appInfo.targetSdkVersion
                )
            )
        }

        // Apply sorting
        when (sortOrder) {
            AppSortOrder.NAME_ASC -> appList.sortedBy { it.appName.lowercase() }
            AppSortOrder.NAME_DESC -> appList.sortedByDescending { it.appName.lowercase() }
            AppSortOrder.SIZE_DESC -> appList.sortedByDescending { it.apkSizeBytes }
            AppSortOrder.SIZE_ASC -> appList.sortedBy { it.apkSizeBytes }
            AppSortOrder.DATE_DESC -> appList.sortedByDescending { it.lastUpdateTime }
        }
    }

    suspend fun extractSingleApk(app: AppInfo): ExtractionResult = withContext(Dispatchers.IO) {
        val sourceFile = File(app.sourceDir)
        if (!sourceFile.exists()) {
            return@withContext ExtractionResult(
                success = false,
                savedFile = null,
                message = "File APK sorgente non trovato per ${app.appName}",
                app = app
            )
        }

        val targetDir = getExtractionDirectory()
        val destFile = File(targetDir, app.safeFileName)

        try {
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }
            ExtractionResult(
                success = true,
                savedFile = destFile,
                message = "APK salvato in: ${destFile.name}",
                app = app
            )
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                savedFile = null,
                message = "Errore durante l'estrazione: ${e.localizedMessage}",
                app = app
            )
        }
    }

    suspend fun extractMultipleApks(
        apps: List<AppInfo>,
        onProgress: (BatchProgress) -> Unit
    ): List<ExtractionResult> = withContext(Dispatchers.IO) {
        val total = apps.size
        val results = mutableListOf<ExtractionResult>()
        val extractedFiles = mutableListOf<File>()
        var successCount = 0
        var failedCount = 0

        apps.forEachIndexed { index, app ->
            onProgress(
                BatchProgress(
                    current = index + 1,
                    total = total,
                    currentAppName = app.appName,
                    isFinished = false,
                    successCount = successCount,
                    failedCount = failedCount,
                    extractedFiles = extractedFiles.toList()
                )
            )

            val res = extractSingleApk(app)
            results.add(res)
            if (res.success && res.savedFile != null) {
                successCount++
                extractedFiles.add(res.savedFile)
            } else {
                failedCount++
            }
        }

        onProgress(
            BatchProgress(
                current = total,
                total = total,
                currentAppName = "",
                isFinished = true,
                successCount = successCount,
                failedCount = failedCount,
                extractedFiles = extractedFiles.toList()
            )
        )

        results
    }

    suspend fun getExtractedApks(): List<ExtractedApk> = withContext(Dispatchers.IO) {
        val dir = getExtractionDirectory()
        val files = dir.listFiles { file -> file.isFile && file.extension.equals("apk", ignoreCase = true) }
            ?: emptyArray()

        files.map { file ->
            var pkgName: String? = null
            var vName: String? = null
            var label: String? = null

            try {
                val archiveInfo = packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                if (archiveInfo != null) {
                    pkgName = archiveInfo.packageName
                    vName = archiveInfo.versionName
                    archiveInfo.applicationInfo?.sourceDir = file.absolutePath
                    archiveInfo.applicationInfo?.publicSourceDir = file.absolutePath
                    label = archiveInfo.applicationInfo?.let { packageManager.getApplicationLabel(it).toString() }
                }
            } catch (e: Exception) {
                // Ignore parsing errors for custom files
            }

            ExtractedApk(
                file = file,
                name = file.name,
                path = file.absolutePath,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                packageName = pkgName,
                appName = label ?: file.nameWithoutExtension,
                versionName = vName
            )
        }.sortedByDescending { it.lastModified }
    }

    suspend fun deleteExtractedApk(apk: ExtractedApk): Boolean = withContext(Dispatchers.IO) {
        try {
            apk.file.delete()
        } catch (e: Exception) {
            false
        }
    }

    fun shareViaBluetooth(file: File): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val btIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.android.bluetooth")
        }

        return try {
            val resolved = btIntent.resolveActivity(packageManager)
            if (resolved != null) {
                val chooser = Intent.createChooser(btIntent, "Invia via Bluetooth")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                true
            } else {
                shareGeneral(file, title = "Invia APK (Bluetooth o altre app)")
                false
            }
        } catch (e: Exception) {
            shareGeneral(file, title = "Invia APK (Bluetooth o altre app)")
            false
        }
    }

    fun shareMultipleViaBluetooth(files: List<File>): Boolean {
        if (files.isEmpty()) return false
        val uris = ArrayList<Uri>(files.map {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
        })

        val btIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/vnd.android.package-archive"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.android.bluetooth")
        }

        return try {
            val resolved = btIntent.resolveActivity(packageManager)
            if (resolved != null) {
                val chooser = Intent.createChooser(btIntent, "Invia ${files.size} APK via Bluetooth")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                true
            } else {
                shareMultipleGeneral(files, title = "Invia ${files.size} APK")
                false
            }
        } catch (e: Exception) {
            shareMultipleGeneral(files, title = "Invia ${files.size} APK")
            false
        }
    }

    fun shareGeneral(file: File, title: String = "Condividi APK") {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareMultipleGeneral(files: List<File>, title: String = "Condividi APK") {
        if (files.isEmpty()) return
        val uris = ArrayList<Uri>(files.map {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
        })

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/vnd.android.package-archive"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun installApk(file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openAppSettings(packageName: String) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // fallback
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
