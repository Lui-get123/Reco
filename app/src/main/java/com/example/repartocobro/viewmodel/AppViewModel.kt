package com.example.repartocobro.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.repartocobro.data.AppDatabaseHelper
import com.example.repartocobro.data.GoogleDriveRepository
import com.example.repartocobro.data.SupabaseLicenseRepository
import com.example.repartocobro.data.StoreRepository
import com.example.repartocobro.model.Collector
import com.example.repartocobro.model.LicenseStatus
import com.example.repartocobro.model.Route
import com.example.repartocobro.model.Store
import com.example.repartocobro.pdf.PdfExportResult
import com.example.repartocobro.pdf.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val isValidatingLicense: Boolean = false,
    val hasAcceptedTerms: Boolean = false,
    val lastPdfUri: String? = null,
    val isDataStale: Boolean = false,
    val allProducts: List<com.example.repartocobro.model.Product> = emptyList(),
    val allRoutes: List<com.example.repartocobro.model.Route> = emptyList(),
    val allStoresAdmin: List<com.example.repartocobro.model.Store> = emptyList(),
    // Google Drive
    val driveUploadState: DriveUploadState = DriveUploadState.IDLE,
    val needsGoogleSignIn: Boolean = false,
    val isDriveSignedIn: Boolean = false
)

enum class DriveUploadState {
    IDLE,
    UPLOADING,
    SUCCESS,
    ERROR
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dbHelper = AppDatabaseHelper(application)
    private val repository = StoreRepository(dbHelper)
    private val supabaseLicenseRepo = SupabaseLicenseRepository(dbHelper)
    private val pdfExporter = PdfExporter()
    val driveRepository = GoogleDriveRepository(application)

    private var pendingDriveUpload: PdfExportResult? = null
    private val prefs = application.getSharedPreferences("reco_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                checkLicense()
                loadCollectors()
                loadTermsAcceptance()
                loadLastPdfUri()
                loadAdminData()
            }
        }
        _uiState.update { it.copy(isDriveSignedIn = driveRepository.isSignedIn()) }
    }

    // --- Admin Data & CRUD ---
    fun loadAdminData() {
        viewModelScope.launch(Dispatchers.IO) {
            val products = repository.getProducts()
            // To get all routes, we need a method in repo. We can get them by iterating collectors or add a method.
            // Since we have collectors, let's just get routes for them.
            val cols = repository.getCollectors()
            val routes = cols.mapNotNull { repository.getRouteByCollector(it.id) }
            val stores = routes.flatMap { repository.getStoresByRoute(it.id) }
            
            _uiState.update { 
                it.copy(
                    allProducts = products,
                    allRoutes = routes,
                    allStoresAdmin = stores
                ) 
            }
        }
    }

    fun addProduct(name: String, price: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addProduct(name, price, "inventory_2")
            loadAdminData()
            _uiState.update { it.copy(message = "Producto agregado") }
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProduct(id)
            loadAdminData()
            _uiState.update { it.copy(message = "Producto eliminado") }
        }
    }

    fun addCollector(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCollector(name)
            loadAdminData()
            loadCollectors() // Update collectors list
            _uiState.update { it.copy(message = "Cobrador agregado") }
        }
    }

    fun deleteCollector(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCollector(id)
            loadAdminData()
            loadCollectors() // Update collectors list
            _uiState.update { it.copy(message = "Cobrador eliminado") }
        }
    }

    fun addStore(name: String, routeId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addStore(name, routeId)
            loadAdminData()
            _uiState.update { it.copy(message = "Tienda agregada") }
        }
    }

    fun deleteStore(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStore(id)
            loadAdminData()
            _uiState.update { it.copy(message = "Tienda eliminada") }
        }
    }

    /* ───────────── Términos y Condiciones ───────────── */

    private fun loadTermsAcceptance() {
        val accepted = prefs.getBoolean("terms_accepted", false)
        _uiState.update { it.copy(hasAcceptedTerms = accepted) }
    }

    fun acceptTerms() {
        prefs.edit().putBoolean("terms_accepted", true).apply()
        _uiState.update { it.copy(hasAcceptedTerms = true) }
    }

    private fun loadLastPdfUri() {
        val savedUri = prefs.getString("last_pdf_uri", null)
        if (savedUri != null) {
            _uiState.update { it.copy(lastPdfUri = savedUri) }
        }
    }

    private fun saveLastPdfUri(uri: String) {
        prefs.edit().putString("last_pdf_uri", uri).apply()
        _uiState.update { it.copy(lastPdfUri = uri) }
    }

    /* ───────────── Licencia (Supabase) ───────────── */

    fun checkLicense() {
        viewModelScope.launch(Dispatchers.IO) {
            val status = supabaseLicenseRepo.getLicenseStatus()
            _uiState.update { it.copy(licenseStatus = status) }
        }
    }

    fun redeemCode(code: String) {
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
                            message = "Licencia activada por ${newStatus.daysRemaining} días (vence: ${result.newExpiration})"
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
        viewModelScope.launch(Dispatchers.IO) {
            val collectors = repository.getCollectors()
            _uiState.update { it.copy(collectors = collectors) }
        }
    }

    fun loginCollector(collector: Collector) {
        viewModelScope.launch(Dispatchers.IO) {
            val route = repository.getRouteByCollector(collector.id)
            val stores = route?.let { selectedRoute -> repository.getStoresByRoute(selectedRoute.id) } ?: emptyList()
            _uiState.update {
                it.copy(
                    selectedCollector = collector,
                    route = route,
                    stores = stores,
                    selectedStoreId = null,
                    message = null,
                    isDataStale = checkStaleData(stores)
                )
            }
        }
    }

    fun logout() {
        _uiState.update {
            it.copy(
                selectedCollector = null,
                route = null,
                stores = emptyList(),
                selectedStoreId = null,
                message = null,
                isDataStale = false
            )
        }
    }

    private fun checkStaleData(stores: List<Store>): Boolean {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return stores.any { store ->
            store.collectionDate != null && store.collectionDate != currentDate
        }
    }

    fun selectStore(storeId: Int) {
        _uiState.update { it.copy(selectedStoreId = storeId, message = null) }
    }

    fun clearSelectedStore() {
        _uiState.update { it.copy(selectedStoreId = null) }
    }

    fun updateStoreProducts(storeId: Int, products: List<com.example.repartocobro.model.StoreProduct>) {
        viewModelScope.launch(Dispatchers.IO) {
            products.forEach { sp ->
                repository.updateStoreProduct(storeId, sp.product.id, sp.deliveredQuantity, sp.soldQuantity)
            }
            refreshStores()
            _uiState.update { it.copy(message = "Cantidades actualizadas") }
        }
    }

    fun saveAllDelivered(deliveries: Map<Int, List<com.example.repartocobro.model.StoreProduct>>) {
        viewModelScope.launch(Dispatchers.IO) {
            deliveries.forEach { (storeId, products) ->
                products.forEach { sp ->
                    repository.updateStoreProduct(storeId, sp.product.id, sp.deliveredQuantity, sp.soldQuantity)
                }
            }
            refreshStores()
            _uiState.update {
                it.copy(
                    selectedStoreId = null,
                    message = "Cantidades actualizadas para ${deliveries.size} tienda(s)"
                )
            }
        }
    }

    fun markCollected(storeId: Int, products: List<com.example.repartocobro.model.StoreProduct>, observations: String?) {
        if (!_uiState.value.licenseStatus.isActive) {
            _uiState.update { it.copy(message = "Se requiere licencia activa para registrar cobros", showLicenseDialog = true) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            products.forEach { sp ->
                repository.updateStoreProduct(storeId, sp.product.id, sp.deliveredQuantity, sp.soldQuantity)
            }
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.updateStoreCollected(storeId, 1, currentDate, observations)
            refreshStores()
            _uiState.update { it.copy(selectedStoreId = null, message = "Tienda marcada como cobrada") }
        }
    }

    fun markAsPendingPayment(storeId: Int, products: List<com.example.repartocobro.model.StoreProduct>, observations: String?) {
        if (!_uiState.value.licenseStatus.isActive) {
            _uiState.update { it.copy(message = "Se requiere licencia activa para registrar deudas", showLicenseDialog = true) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val store = _uiState.value.stores.firstOrNull { it.id == storeId } ?: return@launch
            val route = _uiState.value.route ?: return@launch
            val amount = products.sumOf { it.collectedValue }
            products.forEach { sp ->
                repository.updateStoreProduct(storeId, sp.product.id, sp.deliveredQuantity, sp.soldQuantity)
            }
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.updateStoreCollected(storeId, 2, currentDate, observations)
            repository.addPendingDebt(
                storeId = storeId,
                amount = amount,
                observations = observations
            )
            refreshStores()
            _uiState.update { it.copy(selectedStoreId = null, message = "Deuda de \$$amount registrada") }
        }
    }

    fun collectPendingDebt(storeId: Int) {
        if (!_uiState.value.licenseStatus.isActive) {
            _uiState.update { it.copy(message = "Se requiere licencia activa", showLicenseDialog = true) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val store = _uiState.value.stores.firstOrNull { it.id == storeId } ?: return@launch
            val debtAmount = store.pendingDebtTotal
            repository.markAllDebtsAsPaid(storeId)
            refreshStores()
            _uiState.update { it.copy(selectedStoreId = null, message = "Deuda de \$$debtAmount cobrada exitosamente") }
        }
    }

    fun resetCurrentRoute() {
        viewModelScope.launch(Dispatchers.IO) {
            val route = _uiState.value.route ?: return@launch
            repository.resetRouteStores(route.id)
            refreshStores()
            _uiState.update { it.copy(selectedStoreId = null, message = "Ruta reiniciada desde cero") }
        }
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
            result.onSuccess { pdfResult ->
                saveLastPdfUri(pdfResult.localPath)
                _uiState.update { it.copy(message = "PDF guardado en Descargas") }
                uploadToDrive(pdfResult)
            }
            result.onFailure {
                _uiState.update { it.copy(message = "No se pudo generar el PDF") }
            }
        }
    }

    private fun uploadToDrive(pdfResult: PdfExportResult) {
        if (!driveRepository.isSignedIn()) {
            pendingDriveUpload = pdfResult
            _uiState.update { it.copy(needsGoogleSignIn = true) }
            return
        }
        _uiState.update { it.copy(driveUploadState = DriveUploadState.UPLOADING) }
        viewModelScope.launch {
            val uploadResult = withContext(Dispatchers.IO) {
                driveRepository.uploadPdf(pdfResult.fileName, pdfResult.pdfBytes)
            }
            uploadResult.onSuccess {
                _uiState.update {
                    it.copy(
                        driveUploadState = DriveUploadState.SUCCESS,
                        message = "PDF guardado en Descargas y subido a Drive ✓"
                    )
                }
            }
            uploadResult.onFailure { error ->
                _uiState.update {
                    it.copy(
                        driveUploadState = DriveUploadState.ERROR,
                        message = "PDF guardado localmente. Error al subir a Drive: ${error.localizedMessage}"
                    )
                }
            }
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(driveUploadState = DriveUploadState.IDLE) }
        }
    }

    fun handleGoogleSignIn(success: Boolean) {
        _uiState.update {
            it.copy(
                needsGoogleSignIn = false,
                isDriveSignedIn = success
            )
        }
        if (success) {
            val pending = pendingDriveUpload
            pendingDriveUpload = null
            if (pending != null) {
                uploadToDrive(pending)
            }
        } else {
            _uiState.update {
                it.copy(message = "PDF guardado localmente. Inicia sesión con Google para respaldo en Drive.")
            }
        }
    }

    fun getGoogleSignInIntent(): android.content.Intent = driveRepository.getSignInIntent()

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun refreshStores() {
        val route = _uiState.value.route ?: return
        val stores = repository.getStoresByRoute(route.id)
        _uiState.update { it.copy(stores = stores, isDataStale = checkStaleData(stores)) }
    }
}
