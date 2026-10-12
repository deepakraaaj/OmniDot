package com.omnidot.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(val label: String, val icon: ImageVector) {
    WORKFLOWS("Workflows", Icons.Rounded.Bolt),
    GOALS("Goals", Icons.Rounded.Flag),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

@Composable
fun OmniDotApp(
    initialTab: AppTab,
    workflows: @Composable (PaddingValues, SnackbarHostState, (Boolean) -> Unit) -> Unit,
    goals: @Composable (PaddingValues, SnackbarHostState) -> Unit,
    settings: @Composable (PaddingValues, SnackbarHostState) -> Unit,
) {
    var tab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    var hideBar by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!hideBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            AppTab.WORKFLOWS -> workflows(padding, snackbar) { hideBar = it }
            AppTab.GOALS -> goals(padding, snackbar)
            AppTab.SETTINGS -> settings(padding, snackbar)
        }
    }
}
