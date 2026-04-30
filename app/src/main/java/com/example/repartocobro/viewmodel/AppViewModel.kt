package com.example.repartocobro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.repartocobro.data.AppDatabaseHelper
import com.example.repartocobro.data.SupabaseLicenseRepository
import com.example.repartocobro.data.StoreRepository
import com.example.repartocobro.model.Collector
import com.example.repartocobro.model.LicenseStatus
import com.example.repartocobro.model.Route
import com.example.repartocobro.model.Store
import com.example.repartocobro.pdf.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppUiState(
    val collectors: List<Collector> = emptyList(),
    val selectedCollector: Collector? = null,
    val route: Route? = null,
    val stores: List<Store> = emptyList(),
    val selectedStoreId: Int? = null,
    val message: String? = null,
    val licenseStatus: LicenseStatus = LicenseStatus(
        isActive = false,
        expirationDate = null,
        daysRemaining = 0
    ),
    val showLicenseDialog: Boolean = false,
    val isValidatingLicense: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dbHelper = AppDatabaseHelper(application)
    private val repository = StoreRepository(dbHelper)
    private val supabaseLicenseRepo = SupabaseLicenseRepository(dbHelper)
    private val pdfExporter = PdfExporter()

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        checkLicense()
        loadCollectors()
    }

    /* ───────────── Licencia (Supabase) ───────────── */

    fun checkLicense() {
        val status = supabaseLicenseRepo.getLicenseStatus()
        _uiState.update { it.copy(licenseStatus = status) }
    }

    fun redeemCode(code: String) {
        // Evitar múltiples validaciones simultáneas
        if (_uiState.value.isValidatingLicense) return

        _uiState.update { it.copy(isValidatingLicense = true, message = null) }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                supabaseLicenseRepo.redeemCode(code)
            }

            when (result) {
                is SupabaseLicenseRepository.RedeemResult.Success -> {
                    val newStatus = supabaseLicenseRepo.getLicenseStatus()
                    _uiState.update {
                        it.copy(
                            licenseStatus = newStatus,
                            showLicenseDialog = false,
                            isValidatingLicense = false,
                            message = "✅ Licencia activada por ${newStatus.daysRemaining} días (vence: ${result.newExpiration})"
                        )
                    }
                }
                is SupabaseLicenseRepository.RedeemResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isValidatingLicense = false,
                            message = result.message
                        )
                    }
                }
            }
        }
    }

    fun toggleLicenseDialog() {
        _uiState.update { it.copy(showLicenseDialog = !it.showLicenseDialog) }
    }

    /* ───────────── Collectors / Routes ───────────── */

    private fun loadCollectors() {
        _uiState.update { it.copy(collectors = repository.getCollectors()) }
    }

    fun loginCollector(collector: Collector) {
        val route = repository.getRouteByCollector(collector.id)
        _uiState.update {
            it.copy(
                selectedCollector = collector,
                route = route,
                stores = route?.let { selectedRoute -> repository.getStoresByRoute(selectedRoute.id) }
                    ?: emptyList(),
                selectedStoreId = null,
                message = null
            )
        }
    }

    fun selectStore(storeId: Int) {
        val store = _uiState.value.stores.firstOrNull { it.id == storeId } ?: return
        if (store.isCollected) {
            _uiState.update { it.copy(message = "Tienda bloqueada: ya fue cobrada") }
            return
        }
        _uiState.update { it.copy(selectedStoreId = storeId, message = null) }
    }

    fun clearSelectedStore() {
        _uiState.update { it.copy(selectedStoreId = null) }
    }

    fun saveSales(storeId: Int, soldEmpanadas: Int, soldDeditos: Int) {
        val store = _uiState.value.stores.firstOrNull { it.id == storeId } ?: return
        val safeEmpanadas = soldEmpanadas.coerceIn(0, store.deliveredEmpanadas)
        val safeDeditos = soldDeditos.coerceIn(0, store.deliveredDeditos)
        repository.updateStoreSales(storeId, safeEmpanadas, safeDeditos)
        refreshStores()
        _uiState.update { it.copy(message = "Ventas actualizadas") }
    }

    fun saveDelivered(storeId: Int, deliveredEmpanadas: Int, deliveredDeditos: Int) {
        val safeEmpanadas = deliveredEmpanadas.coerceAtLeast(0)
        val safeDeditos = deliveredDeditos.coerceAtLeast(0)
        repository.updateStoreDelivered(storeId, safeEmpanadas, safeDeditos)
        // Marcar como repartida si se entregó algo
        if (safeEmpanadas > 0 || safeDeditos > 0) {
            repository.markStoreDelivered(storeId)
        }
        val currentStore = _uiState.value.stores.firstOrNull { it.id == storeId }
        if (currentStore != null) {
            repository.updateStoreSales(
                storeId = storeId,
                soldEmpanadas = currentStore.soldEmpanadas.coerceAtMost(safeEmpanadas),
                soldDeditos = currentStore.soldDeditos.coerceAtMost(safeDeditos)
            )
        }
        refreshStores()
        _uiState.update { it.copy(selectedStoreId = null, message = "Entregas guardadas y tienda repartida ✅") }
    }

    fun markCollected(storeId: Int) {
        repository.markStoreCollected(storeId)
        refreshStores()
        _uiState.update { it.copy(selectedStoreId = null, message = "Tienda marcada como cobrada ✅") }
    }

    fun resetCurrentRoute() {
        val route = _uiState.value.route ?: return
        repository.resetRouteStores(route.id)
        refreshStores()
        _uiState.update { it.copy(selectedStoreId = null, message = "Ruta reiniciada desde cero") }
    }

    fun exportCurrentRoutePdf() {
        val state = _uiState.value
        val route = state.route ?: return
        val collector = state.selectedCollector ?: return
        viewModelScope.launch {
            val summary = repository.getRouteSummary(route.id)
            val result = pdfExporter.exportRouteSummary(
                context = getApplication(),
                route = route,
                collectorName = collector.name,
                summary = summary
            )
            _uiState.update {
                it.copy(
                    message = result.fold(
                        onSuccess = { pathOrUri -> "PDF generado en Descargas: $pathOrUri" },
                        onFailure = { "No se pudo generar el PDF" }
                    )
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun refreshStores() {
        val route = _uiState.value.route ?: return
        _uiState.update { it.copy(stores = repository.getStoresByRoute(route.id)) }
    }
}
