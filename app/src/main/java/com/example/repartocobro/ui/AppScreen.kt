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
    onSaveDelivered: (Int, List<com.example.repartocobro.model.StoreProduct>) -> Unit,
    onSaveAllDelivered: (Map<Int, List<com.example.repartocobro.model.StoreProduct>>) -> Unit,
    onMarkCollected: (Int, List<com.example.repartocobro.model.StoreProduct>, String?) -> Unit,
    onMarkPendingPayment: (Int, List<com.example.repartocobro.model.StoreProduct>, String?) -> Unit,
    onCollectDebt: (Int) -> Unit,
    onResetRoute: () -> Unit, onExportPdf: () -> Unit,
    onDismissMessage: () -> Unit,
    onRedeemCode: (String) -> Unit, onToggleLicenseDialog: () -> Unit,
    onAcceptTerms: () -> Unit = {},
    // Admin CRUD
    onAddProduct: (String, Int) -> Unit = { _, _ -> },
    onDeleteProduct: (Int) -> Unit = {},
    onAddCollector: (String) -> Unit = { _ -> },
    onDeleteCollector: (Int) -> Unit = {},
    onAddStore: (String, Int) -> Unit = { _, _ -> },
    onDeleteStore: (Int) -> Unit = {}
) {
    var showAdminScreen by remember { mutableStateOf(false) }

    Scaffold(containerColor = LightBackground) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (!state.licenseStatus.isActive) {
                LicenseLockScreen(state.licenseStatus, state.isValidatingLicense, onRedeemCode)
            } else if (showAdminScreen) {
                AdminScreen(state, onClose = { showAdminScreen = false }, onAddProduct, onDeleteProduct, onAddCollector, onDeleteCollector, onAddStore, onDeleteStore)
            } else if (state.selectedCollector == null) {
                LoginContent(state, onLogin, onOpenAdmin = { showAdminScreen = true }, onToggleLicenseDialog, onAcceptTerms)
            } else {
                RouteContent(state, onSelectStore, onCloseStoreForm, onSaveDelivered,
                    onSaveAllDelivered, onMarkCollected, onMarkPendingPayment,
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
