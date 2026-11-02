package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppFilter
import com.example.model.AppInfo
import com.example.model.AppSortOrder
import com.example.ui.components.AppListItem

@Composable
fun InstalledAppsScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    searchQuery: String,
    currentFilter: AppFilter,
    currentSort: AppSortOrder,
    selectedPackages: Set<String>,
    onSearchChange: (String) -> Unit,
    onFilterChange: (AppFilter) -> Unit,
    onSortChange: (AppSortOrder) -> Unit,
    onToggleSelect: (String) -> Unit,
    onExtractApp: (AppInfo) -> Unit,
    onBluetoothApp: (AppInfo) -> Unit,
    onDetailsApp: (AppInfo) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSortMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("installed_apps_screen")
    ) {
        // Search & Sort bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_field"),
                placeholder = {
                    Text(
                        "Cerca nome o pacchetto...",
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cerca",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Cancella testo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Sort Menu Button
            Box {
                IconButton(
                    onClick = { isSortMenuOpen = true },
                    modifier = Modifier.testTag("btn_sort")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Ordina app",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = isSortMenuOpen,
                    onDismissRequest = { isSortMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Nome (A - Z)") },
                        onClick = {
                            onSortChange(AppSortOrder.NAME_ASC)
                            isSortMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nome (Z - A)") },
                        onClick = {
                            onSortChange(AppSortOrder.NAME_DESC)
                            isSortMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Dimensione (Più grande)") },
                        onClick = {
                            onSortChange(AppSortOrder.SIZE_DESC)
                            isSortMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Dimensione (Più piccolo)") },
                        onClick = {
                            onSortChange(AppSortOrder.SIZE_ASC)
                            isSortMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ultimo aggiornamento") },
                        onClick = {
                            onSortChange(AppSortOrder.DATE_DESC)
                            isSortMenuOpen = false
                        }
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("btn_refresh")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Ricarica elenco",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Filter Chips row (All, User, System)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = currentFilter == AppFilter.USER,
                onClick = { onFilterChange(AppFilter.USER) },
                label = { Text("App Utente") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = currentFilter == AppFilter.ALL,
                onClick = { onFilterChange(AppFilter.ALL) },
                label = { Text("Tutte") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = currentFilter == AppFilter.SYSTEM,
                onClick = { onFilterChange(AppFilter.SYSTEM) },
                label = { Text("Sistema") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Count Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${apps.size} applicazioni trovate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (selectedPackages.isNotEmpty()) {
                Text(
                    text = "${selectedPackages.size} selezionate",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // App List or Loading/Empty state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scansione applicazioni installate...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (apps.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nessuna applicazione trovata",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Prova a modificare i filtri o la chiave di ricerca.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
                ) {
                    items(
                        items = apps,
                        key = { it.packageName }
                    ) { app ->
                        AppListItem(
                            app = app,
                            isSelected = selectedPackages.contains(app.packageName),
                            isSelectionMode = selectedPackages.isNotEmpty(),
                            onToggleSelect = { onToggleSelect(app.packageName) },
                            onExtractClick = { onExtractApp(app) },
                            onBluetoothClick = { onBluetoothApp(app) },
                            onDetailsClick = { onDetailsApp(app) }
                        )
                    }
                }
            }
        }
    }
}
