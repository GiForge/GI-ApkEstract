package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApkRepository
import com.example.model.AppFilter
import com.example.model.AppInfo
import com.example.model.AppSortOrder
import com.example.model.BatchProgress
import com.example.model.ExtractedApk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ApkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ApkRepository(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(true)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFilter = MutableStateFlow(AppFilter.USER)
    val currentFilter: StateFlow<AppFilter> = _currentFilter.asStateFlow()

    private val _currentSort = MutableStateFlow(AppSortOrder.NAME_ASC)
    val currentSort: StateFlow<AppSortOrder> = _currentSort.asStateFlow()

    private val _selectedPackageNames = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackageNames: StateFlow<Set<String>> = _selectedPackageNames.asStateFlow()

    private val _extractedApks = MutableStateFlow<List<ExtractedApk>>(emptyList())
    val extractedApks: StateFlow<List<ExtractedApk>> = _extractedApks.asStateFlow()

    private val _isLoadingExtracted = MutableStateFlow(false)
    val isLoadingExtracted: StateFlow<Boolean> = _isLoadingExtracted.asStateFlow()

    private val _extractionDirectory = MutableStateFlow(repository.getExtractionDirectory().absolutePath)
    val extractionDirectory: StateFlow<String> = _extractionDirectory.asStateFlow()

    private val _batchProgress = MutableStateFlow<BatchProgress?>(null)
    val batchProgress: StateFlow<BatchProgress?> = _batchProgress.asStateFlow()

    private val _selectedAppForDetails = MutableStateFlow<AppInfo?>(null)
    val selectedAppForDetails: StateFlow<AppInfo?> = _selectedAppForDetails.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        loadInstalledApps()
        loadExtractedApks()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            try {
                val apps = repository.getInstalledApps(
                    filter = _currentFilter.value,
                    searchQuery = _searchQuery.value,
                    sortOrder = _currentSort.value
                )
                _installedApps.value = apps
            } catch (e: Exception) {
                _snackbarMessage.value = "Errore durante il caricamento app: ${e.localizedMessage}"
            } finally {
                _isLoadingApps.value = false
            }
        }
    }

    fun loadExtractedApks() {
        viewModelScope.launch {
            _isLoadingExtracted.value = true
            try {
                _extractedApks.value = repository.getExtractedApks()
                _extractionDirectory.value = repository.getExtractionDirectory().absolutePath
            } catch (e: Exception) {
                _snackbarMessage.value = "Errore caricamento APK estratti: ${e.localizedMessage}"
            } finally {
                _isLoadingExtracted.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadInstalledApps()
    }

    fun setFilter(filter: AppFilter) {
        _currentFilter.value = filter
        loadInstalledApps()
    }

    fun setSort(sort: AppSortOrder) {
        _currentSort.value = sort
        loadInstalledApps()
    }

    fun toggleAppSelection(packageName: String) {
        val current = _selectedPackageNames.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedPackageNames.value = current
    }

    fun selectAll() {
        _selectedPackageNames.value = _installedApps.value.map { it.packageName }.toSet()
    }

    fun deselectAll() {
        _selectedPackageNames.value = emptySet()
    }

    fun selectAppForDetails(app: AppInfo?) {
        _selectedAppForDetails.value = app
    }

    fun extractSingle(app: AppInfo) {
        viewModelScope.launch {
            _batchProgress.value = BatchProgress(
                current = 1,
                total = 1,
                currentAppName = app.appName,
                isFinished = false
            )
            val result = repository.extractSingleApk(app)
            _batchProgress.value = BatchProgress(
                current = 1,
                total = 1,
                currentAppName = app.appName,
                isFinished = true,
                successCount = if (result.success) 1 else 0,
                failedCount = if (result.success) 0 else 1,
                extractedFiles = if (result.savedFile != null) listOf(result.savedFile) else emptyList()
            )
            loadExtractedApks()
            _snackbarMessage.value = if (result.success) {
                "APK estratto con successo in: ${result.savedFile?.name}"
            } else {
                result.message
            }
        }
    }

    fun extractSelected() {
        val selectedPackages = _selectedPackageNames.value
        val appsToExtract = _installedApps.value.filter { selectedPackages.contains(it.packageName) }
        if (appsToExtract.isEmpty()) {
            _snackbarMessage.value = "Nessuna app selezionata"
            return
        }

        viewModelScope.launch {
            val results = repository.extractMultipleApks(appsToExtract) { progress ->
                _batchProgress.value = progress
            }
            loadExtractedApks()
            val successful = results.count { it.success }
            _snackbarMessage.value = "$successful di ${appsToExtract.size} APK estratti con successo!"
        }
    }

    fun shareSingleBluetooth(app: AppInfo) {
        viewModelScope.launch {
            // First extract the APK to guarantee a clean file in destination folder
            val result = repository.extractSingleApk(app)
            if (result.success && result.savedFile != null) {
                loadExtractedApks()
                val sent = repository.shareViaBluetooth(result.savedFile)
                if (!sent) {
                    _snackbarMessage.value = "Apertura condivisione per ${app.appName}..."
                }
            } else {
                _snackbarMessage.value = "Impossibile preparare l'APK: ${result.message}"
            }
        }
    }

    fun shareMultipleBluetoothSelected() {
        val selectedPackages = _selectedPackageNames.value
        val appsToShare = _installedApps.value.filter { selectedPackages.contains(it.packageName) }
        if (appsToShare.isEmpty()) {
            _snackbarMessage.value = "Nessuna app selezionata da condividere"
            return
        }

        viewModelScope.launch {
            val results = repository.extractMultipleApks(appsToShare) { progress ->
                _batchProgress.value = progress
            }
            loadExtractedApks()
            val extractedFiles = results.mapNotNull { it.savedFile }
            if (extractedFiles.isNotEmpty()) {
                val sent = repository.shareMultipleViaBluetooth(extractedFiles)
                if (!sent) {
                    _snackbarMessage.value = "Apertura condivisione multipla per ${extractedFiles.size} APK..."
                }
            } else {
                _snackbarMessage.value = "Errore durante l'estrazione degli APK selezionati"
            }
        }
    }

    fun shareSingleGeneral(app: AppInfo) {
        viewModelScope.launch {
            val result = repository.extractSingleApk(app)
            if (result.success && result.savedFile != null) {
                loadExtractedApks()
                repository.shareGeneral(result.savedFile, "Condividi ${app.appName} APK")
            } else {
                _snackbarMessage.value = "Impossibile preparare l'APK: ${result.message}"
            }
        }
    }

    fun shareMultipleGeneralSelected() {
        val selectedPackages = _selectedPackageNames.value
        val appsToShare = _installedApps.value.filter { selectedPackages.contains(it.packageName) }
        if (appsToShare.isEmpty()) {
            _snackbarMessage.value = "Nessuna app selezionata"
            return
        }

        viewModelScope.launch {
            val results = repository.extractMultipleApks(appsToShare) { progress ->
                _batchProgress.value = progress
            }
            loadExtractedApks()
            val extractedFiles = results.mapNotNull { it.savedFile }
            if (extractedFiles.isNotEmpty()) {
                repository.shareMultipleGeneral(extractedFiles, "Condividi ${extractedFiles.size} APK")
            } else {
                _snackbarMessage.value = "Errore durante l'estrazione degli APK"
            }
        }
    }

    fun shareExtractedBluetooth(apk: ExtractedApk) {
        val sent = repository.shareViaBluetooth(apk.file)
        if (!sent) {
            _snackbarMessage.value = "Apertura condivisione per ${apk.name}..."
        }
    }

    fun shareExtractedGeneral(apk: ExtractedApk) {
        repository.shareGeneral(apk.file, "Condividi ${apk.name}")
    }

    fun deleteExtracted(apk: ExtractedApk) {
        viewModelScope.launch {
            val success = repository.deleteExtractedApk(apk)
            if (success) {
                _snackbarMessage.value = "File eliminato: ${apk.name}"
                loadExtractedApks()
            } else {
                _snackbarMessage.value = "Impossibile eliminare ${apk.name}"
            }
        }
    }

    fun installExtracted(apk: ExtractedApk) {
        val started = repository.installApk(apk.file)
        if (!started) {
            _snackbarMessage.value = "Impossibile avviare l'installazione del file APK"
        }
    }

    fun openAppSettings(packageName: String) {
        repository.openAppSettings(packageName)
    }

    fun launchApp(packageName: String) {
        val launched = repository.launchApp(packageName)
        if (!launched) {
            _snackbarMessage.value = "Impossibile aprire l'app (nessuna attività principale)"
        }
    }

    fun shareExtractedFilesBluetooth(files: List<File>) {
        val sent = repository.shareMultipleViaBluetooth(files)
        if (!sent) {
            _snackbarMessage.value = "Apertura condivisione per ${files.size} APK..."
        }
    }

    fun shareExtractedFilesGeneral(files: List<File>) {
        repository.shareMultipleGeneral(files, "Condividi ${files.size} APK")
    }

    fun dismissProgressDialog() {
        _batchProgress.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
