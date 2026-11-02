package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppInfo
import com.example.ui.components.AppDetailBottomSheet
import com.example.ui.components.BatchSelectionBar
import com.example.ui.components.ExtractionProgressDialog
import com.example.viewmodel.ApkViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ApkViewModel,
    modifier: Modifier = Modifier
) {
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val currentSort by viewModel.currentSort.collectAsStateWithLifecycle()
    val selectedPackages by viewModel.selectedPackageNames.collectAsStateWithLifecycle()

    val extractedApks by viewModel.extractedApks.collectAsStateWithLifecycle()
    val isLoadingExtracted by viewModel.isLoadingExtracted.collectAsStateWithLifecycle()
    val extractionDirectory by viewModel.extractionDirectory.collectAsStateWithLifecycle()

    val batchProgress by viewModel.batchProgress.collectAsStateWithLifecycle()
    val selectedAppForDetails by viewModel.selectedAppForDetails.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(com.example.R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.testTag("btn_help")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Guida e informazioni",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Filled.Apps else Icons.Outlined.Apps,
                            contentDescription = "App Installate"
                        )
                    },
                    label = { Text("App Installate") },
                    modifier = Modifier.testTag("nav_installed_tab")
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = {
                        currentTab = 1
                        viewModel.loadExtractedApks()
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (extractedApks.isNotEmpty()) {
                                    Badge {
                                        Text("${extractedApks.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == 1) Icons.Filled.FolderZip else Icons.Outlined.FolderZip,
                                contentDescription = "APK Estratti"
                            )
                        }
                    },
                    label = { Text("APK Estratti") },
                    modifier = Modifier.testTag("nav_extracted_tab")
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> {
                    InstalledAppsScreen(
                        apps = installedApps,
                        isLoading = isLoadingApps,
                        searchQuery = searchQuery,
                        currentFilter = currentFilter,
                        currentSort = currentSort,
                        selectedPackages = selectedPackages,
                        onSearchChange = viewModel::setSearchQuery,
                        onFilterChange = viewModel::setFilter,
                        onSortChange = viewModel::setSort,
                        onToggleSelect = viewModel::toggleAppSelection,
                        onExtractApp = viewModel::extractSingle,
                        onBluetoothApp = viewModel::shareSingleBluetooth,
                        onDetailsApp = viewModel::selectAppForDetails,
                        onRefresh = viewModel::loadInstalledApps
                    )
                }
                1 -> {
                    ExtractedApksScreen(
                        extractedApks = extractedApks,
                        isLoading = isLoadingExtracted,
                        extractionDirectory = extractionDirectory,
                        onBluetoothClick = viewModel::shareExtractedBluetooth,
                        onShareClick = viewModel::shareExtractedGeneral,
                        onInstallClick = viewModel::installExtracted,
                        onDeleteClick = viewModel::deleteExtracted,
                        onRefresh = viewModel::loadExtractedApks,
                        onShowToast = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }
            }

            // Floating Batch Selection Bar on Tab 0
            if (currentTab == 0 && selectedPackages.isNotEmpty()) {
                BatchSelectionBar(
                    selectedCount = selectedPackages.size,
                    totalCount = installedApps.size,
                    onSelectAll = viewModel::selectAll,
                    onDeselectAll = viewModel::deselectAll,
                    onExtractSelected = viewModel::extractSelected,
                    onBluetoothShareSelected = viewModel::shareMultipleBluetoothSelected,
                    onGeneralShareSelected = viewModel::shareMultipleGeneralSelected,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    // App Detail Bottom Sheet
    selectedAppForDetails?.let { app ->
        AppDetailBottomSheet(
            app = app,
            sheetState = sheetState,
            onDismiss = { viewModel.selectAppForDetails(null) },
            onExtract = { viewModel.extractSingle(app) },
            onShareBluetooth = { viewModel.shareSingleBluetooth(app) },
            onShareGeneral = { viewModel.shareSingleGeneral(app) },
            onLaunchApp = { viewModel.launchApp(app.packageName) },
            onOpenSettings = { viewModel.openAppSettings(app.packageName) },
            onShowToast = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    // Batch / Single Extraction Progress Dialog
    batchProgress?.let { progress ->
        ExtractionProgressDialog(
            progress = progress,
            onDismiss = viewModel::dismissProgressDialog,
            onShareBluetooth = viewModel::shareExtractedFilesBluetooth,
            onShareGeneral = viewModel::shareExtractedFilesGeneral,
            onNavigateToExtracted = {
                currentTab = 1
                viewModel.loadExtractedApks()
            }
        )
    }

    // Help & Information Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "Come Funziona",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. Estrazione Singola o Multipla:\nSeleziona una o più app toccando la casella di controllo o tenendo premuto su un'app, poi premi 'Estrai' per salvare i file APK nella memoria del dispositivo.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2. Condivisione via Bluetooth:\nPuoi inviare direttamente l'APK via Bluetooth toccando l'icona Bluetooth su un'app o selezionando più app contemporaneamente.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "3. Gestione APK Estratti:\nNella scheda 'APK Estratti' puoi vedere tutti i file esportati, visualizzare la dimensione, aprirli, installarli o eliminarli.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Ho Capito")
                }
            }
        )
    }
}
