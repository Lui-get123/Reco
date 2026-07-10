package com.example.repartocobro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.repartocobro.ui.components.TopAlertBanner
import com.example.repartocobro.ui.theme.LightBackground
import com.example.repartocobro.viewmodel.AppUiState

/* ── Root Orchestrator ── */
@Composable
fun AppScreen(
    state: AppUiState, onLogin: (Int) -> Unit, onLogout: () -> Unit,
    onSelectStore: (Int) -> Unit, onCloseStoreForm: () -> Unit,
    onSaveDelivered: (Int, Int, Int) -> Unit,
    onSaveAllDelivered: (Map<Int, Pair<Int, Int>>) -> Unit,
    onSaveSales: (Int, Int, Int) -> Unit, onMarkCollected: (Int, Int, Int, String?) -> Unit,
    onMarkPendingPayment: (Int, Int, Int, String?) -> Unit,
    onCollectDebt: (Int) -> Unit,
    onResetRoute: () -> Unit, onExportPdf: () -> Unit,
    onDismissMessage: () -> Unit,
    onRedeemCode: (String) -> Unit, onToggleLicenseDialog: () -> Unit,
    onAcceptTerms: () -> Unit = {}
) {
    Scaffold(containerColor = LightBackground) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (!state.licenseStatus.isActive) {
                LicenseLockScreen(state.licenseStatus, state.isValidatingLicense, onRedeemCode)
            } else if (state.selectedCollector == null) {
                LoginContent(state, onLogin, onToggleLicenseDialog, onAcceptTerms)
            } else {
                RouteContent(state, onSelectStore, onCloseStoreForm, onSaveDelivered,
                    onSaveAllDelivered, onSaveSales, onMarkCollected, onMarkPendingPayment,
                    onCollectDebt, onResetRoute,
                    onExportPdf, onToggleLicenseDialog, onLogout)
            }
            if (state.showLicenseDialog && state.licenseStatus.isActive) {
                LicenseCodeDialog(state.isValidatingLicense, onToggleLicenseDialog, onRedeemCode)
            }
            Column(Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .statusBarsPadding().padding(top = 4.dp).zIndex(10f)) {
                TopAlertBanner(state.message, onDismissMessage)
            }
        }
    }
}
