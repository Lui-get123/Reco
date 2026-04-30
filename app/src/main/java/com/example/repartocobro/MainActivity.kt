package com.example.repartocobro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.repartocobro.ui.theme.RepartoCobroTheme
import com.example.repartocobro.ui.AppScreen
import com.example.repartocobro.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RepartoCobroTheme {
                val state by viewModel.uiState.collectAsState()
                AppScreen(
                    state = state,
                    onLogin = { collectorId ->
                        state.collectors.firstOrNull { it.id == collectorId }?.let(viewModel::loginCollector)
                    },
                    onSelectStore = viewModel::selectStore,
                    onCloseStoreForm = viewModel::clearSelectedStore,
                    onSaveDelivered = viewModel::saveDelivered,
                    onSaveSales = viewModel::saveSales,
                    onMarkCollected = viewModel::markCollected,
                    onResetRoute = viewModel::resetCurrentRoute,
                    onExportPdf = viewModel::exportCurrentRoutePdf,
                    onDismissMessage = viewModel::clearMessage,
                    onRedeemCode = viewModel::redeemCode,
                    onToggleLicenseDialog = viewModel::toggleLicenseDialog
                )
            }
        }
    }
}