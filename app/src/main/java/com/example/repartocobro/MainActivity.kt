package com.example.repartocobro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.example.repartocobro.ui.theme.RepartoCobroTheme
import com.example.repartocobro.ui.AppScreen
import com.example.repartocobro.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        val success = try {
            task.getResult(ApiException::class.java)
            true
        } catch (e: ApiException) {
            false
        }
        viewModel.handleGoogleSignIn(success)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RepartoCobroTheme {
                val state by viewModel.uiState.collectAsState()

                // Lanzar Google Sign-In cuando se necesite
                LaunchedEffect(state.needsGoogleSignIn) {
                    if (state.needsGoogleSignIn) {
                        googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                    }
                }

                AppScreen(
                    state = state,
                    onLogin = { collectorId ->
                        state.collectors.firstOrNull { it.id == collectorId }?.let(viewModel::loginCollector)
                    },
                    onLogout = viewModel::logout,
                    onSelectStore = viewModel::selectStore,
                    onCloseStoreForm = viewModel::clearSelectedStore,
                    onSaveDelivered = viewModel::saveDelivered,
                    onSaveAllDelivered = viewModel::saveAllDelivered,
                    onSaveSales = viewModel::saveSales,
                    onMarkCollected = viewModel::markCollected,
                    onMarkPendingPayment = viewModel::markAsPendingPayment,
                    onCollectDebt = viewModel::collectPendingDebt,
                    onResetRoute = viewModel::resetCurrentRoute,
                    onExportPdf = viewModel::exportCurrentRoutePdf,
                    onDismissMessage = viewModel::clearMessage,
                    onRedeemCode = viewModel::redeemCode,
                    onToggleLicenseDialog = viewModel::toggleLicenseDialog,
                    onAcceptTerms = viewModel::acceptTerms
                )
            }
        }
    }
}