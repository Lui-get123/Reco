package com.example.repartocobro.ui

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repartocobro.ui.components.*
import com.example.repartocobro.ui.theme.*
import com.example.repartocobro.viewmodel.AppUiState
import com.example.repartocobro.viewmodel.DriveUploadState

/* ── Route content with bottom nav ── */
@Composable
fun RouteContent(
    state: AppUiState, onSelectStore: (Int) -> Unit, onCloseStoreForm: () -> Unit,
    onSaveDelivered: (Int, List<com.example.repartocobro.model.StoreProduct>) -> Unit, 
    onSaveAllDelivered: (Map<Int, List<com.example.repartocobro.model.StoreProduct>>) -> Unit,
    onMarkCollected: (Int, List<com.example.repartocobro.model.StoreProduct>, String?) -> Unit,
    onMarkPendingPayment: (Int, List<com.example.repartocobro.model.StoreProduct>, String?) -> Unit,
    onCollectDebt: (Int) -> Unit,
    onResetRoute: () -> Unit,
    onExportPdf: () -> Unit, onExtendLicense: () -> Unit,
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf(BottomTab.REPARTO) }
    val selectedStore = state.stores.firstOrNull { it.id == state.selectedStoreId }

    val editedDeliveries = remember { mutableStateMapOf<Int, Map<Int, Int>>() }

    var showResetConfirmation by remember { mutableStateOf(false) }

    if (showResetConfirmation) {
        ResetConfirmationDialog(
            onConfirm = { showResetConfirmation = false; onResetRoute() },
            onDismiss = { showResetConfirmation = false }
        )
    }

    val listState = rememberLazyListState()

    // El scroll automático ya no es necesario porque el formulario se abrirá en un Dialog sobrepuesto

    Column(Modifier.fillMaxSize().background(Bone)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LicenseBanner(state.licenseStatus, onExtendLicense)
            }

            if (state.isDataStale && currentTab == BottomTab.COBRO) {
                item {
                    Box(Modifier.fillMaxWidth().clip(cardShape).background(StatusError.copy(0.1f))
                        .border(1.dp, StatusError.copy(0.3f), cardShape).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Datos desfasados", fontWeight = FontWeight.Bold, color = StatusError)
                                Text("La fecha actual no coincide con la de los registros. Reinicia la ruta para evitar errores.",
                                    style = MaterialTheme.typography.bodySmall, color = Graphite)
                            }
                        }
                    }
                }
            }
            
            // Header card with logout button
            item {
                Box(Modifier.fillMaxWidth().shadow(4.dp, cardShape).clip(cardShape).background(WhiteSurface)
                    .padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(SkyBlueSoft), Alignment.Center) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(state.route?.name ?: "Ruta", style = MaterialTheme.typography.titleLarge, color = Graphite)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = GraphiteMedium, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(state.selectedCollector?.name ?: "", style = MaterialTheme.typography.bodyMedium, color = GraphiteMedium)
                            }
                        }
                        // Botón cerrar sesión
                        Box(
                            Modifier.clip(RoundedCornerShape(12.dp))
                                .background(StatusError.copy(0.08f))
                                .border(1.dp, StatusError.copy(0.2f), RoundedCornerShape(12.dp))
                                .clickable { onLogout() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Logout, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Salir", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = StatusError)
                            }
                        }
                    }
                }
            }

            item {
                Crossfade(
                    targetState = currentTab,
                    animationSpec = tween(250),
                    label = "tabTransition",
                    modifier = Modifier.fillMaxWidth()
                ) { targetTab ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (targetTab == BottomTab.STATS) {
                            // ── Estadísticas ──
                            StatsScreen(stores = state.stores)
                        } else {
                            // ── Reparto / Cobro ──
                        // Summary card
                        val deliveredCount by remember(state.stores) { derivedStateOf { state.stores.count { it.isDelivered } } }
                        val collectedCount by remember(state.stores) { derivedStateOf { state.stores.count { it.isCollected } } }
                        val totalStores = state.stores.size
                        val totalMoneyCollected by remember(state.stores) { derivedStateOf { state.stores.filter { it.isCollected }.sumOf { it.collectedValue } } }
                        
                        Box(Modifier.fillMaxWidth().clip(cardShape).background(
                            if (targetTab == BottomTab.REPARTO) MustardSoft else SkyBlueSoft)
                            .border(1.dp, (if (targetTab == BottomTab.REPARTO) Mustard else SkyBlue).copy(0.2f), cardShape)
                            .padding(16.dp)) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Assessment, contentDescription = null,
                                        tint = Graphite, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Resumen del Día", fontWeight = FontWeight.Bold, color = Graphite, fontSize = 16.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("Total Tiendas: $totalStores", color = GraphiteMedium)
                                if (targetTab == BottomTab.COBRO) {
                                    Text("Cobradas: $collectedCount", color = Sage, fontWeight = FontWeight.SemiBold)
                                    Text("Pendientes: ${totalStores - collectedCount}", color = if (totalStores - collectedCount > 0) Mustard else Sage)
                                    Text("Total Cobrado: \$$totalMoneyCollected", fontWeight = FontWeight.Bold, color = Graphite, fontSize = 15.sp)
                                }
                            }
                        }
                            // Quick actions card
                            Box(Modifier.fillMaxWidth().shadow(2.dp, cardShape).clip(cardShape)
                                .background(WhiteSurface).padding(14.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Graphite, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Acciones Rápidas", fontWeight = FontWeight.Bold, color = Graphite, fontSize = 14.sp)
                                    }
                                    SoftDivider()
                                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                                        ActionChip(Icons.Rounded.Refresh, "Reiniciar", StatusError.copy(0.08f), StatusError, Modifier.weight(1f)) {
                                            showResetConfirmation = true
                                        }
                                        ActionChip(Icons.Rounded.Description, "Generar PDF", SkyBlueSoft, SkyBlue, Modifier.weight(1f)) { onExportPdf() }
                                    }
                                    // Indicador de subida a Drive
                                    when (state.driveUploadState) {
                                        DriveUploadState.UPLOADING -> {
                                            Row(
                                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                                    .background(SkyBlueSoft).padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SkyBlue, strokeWidth = 2.dp)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Subiendo a Drive...", color = Graphite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        DriveUploadState.SUCCESS -> {
                                            Row(
                                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                                    .background(Sage.copy(0.15f)).padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("✓ Subido a Drive", color = Sage, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        DriveUploadState.ERROR -> {
                                            Row(
                                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                                    .background(StatusError.copy(0.1f)).padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Error al subir a Drive", color = StatusError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        else -> { /* IDLE - no mostrar nada */ }
                                    }
                                }
                            }
                            // Section title
                            SectionLabel(
                                if (targetTab == BottomTab.REPARTO) "Tiendas — Reparto" else "Tiendas — Cobro",
                                if (targetTab == BottomTab.REPARTO) Icons.Rounded.Inventory2 else Icons.Rounded.Payments
                            )
                        }
                    }
                }
            }

            if (currentTab != BottomTab.STATS) {
                items(state.stores, key = { it.id }) { store ->
                    val isDone = if (currentTab == BottomTab.REPARTO) store.isDelivered else store.isCollected
                    StoreRow(store, store.id == selectedStore?.id, currentTab, isDone) { onSelectStore(store.id) }
                }
            }
        }
        BottomNavBar(currentTab) { currentTab = it }
    }

    // Modal overlay para el formulario de la tienda
    if (selectedStore != null && currentTab != BottomTab.STATS) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onCloseStoreForm,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier.fillMaxSize().padding(16.dp).imePadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(Modifier.padding(top = 48.dp).verticalScroll(rememberScrollState())) {
                    StoreForm(
                        store = selectedStore,
                        tab = currentTab,
                        onClose = onCloseStoreForm,
                        onSaveDelivered = onSaveDelivered,
                        onMarkCollected = onMarkCollected,
                        onMarkPendingPayment = onMarkPendingPayment,
                        onCollectDebt = onCollectDebt,
                        editedDeliveries = editedDeliveries,
                        isBlocked = state.isDataStale
                    )
                }
            }
        }
    }
}

/* ── Reset Confirmation Dialog ── */
@Composable
private fun ResetConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhiteSurface,
        shape = RoundedCornerShape(24.dp),
        icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(40.dp)) },
        title = {
            Text("¿Reiniciar ruta?", fontWeight = FontWeight.Bold, color = StatusError,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column {
                Text("Esta acción eliminará TODOS los datos del día:", color = Graphite,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                listOf("Entregas registradas", "Ventas y cobros", "Fechas de operación").forEach {
                    Text("  •  $it", color = GraphiteMedium, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(StatusError.copy(0.08f))
                        .border(1.dp, StatusError.copy(0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("Esta acción NO se puede deshacer.", color = StatusError,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            AccentButton("Sí, reiniciar", onConfirm, containerColor = StatusError)
        },
        dismissButton = {
            TextButton(onDismiss) { Text("Cancelar", color = GraphiteMedium, fontWeight = FontWeight.SemiBold) }
        }
    )
}

/* ── Bottom Navigation ── */
@Composable
fun BottomNavBar(current: BottomTab, onSelect: (BottomTab) -> Unit) {
    Box(Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(NavBarBackground)
        .padding(horizontal = 8.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
            BottomTab.values().forEach { tab ->
                val selected = tab == current
                val tabColor = when (tab) {
                    BottomTab.REPARTO -> Mustard
                    BottomTab.COBRO -> SkyBlue
                    BottomTab.STATS -> Graphite
                }
                val tabBg = when (tab) {
                    BottomTab.REPARTO -> MustardSoft
                    BottomTab.COBRO -> SkyBlueSoft
                    BottomTab.STATS -> Cream
                }
                val bgColor by animateColorAsState(
                    if (selected) tabBg else Color.Transparent,
                    tween(300), label = "bg${tab.name}")
                val tintColor = if (selected) tabColor else NavBarUnselected
                Box(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            when (tab) {
                                BottomTab.REPARTO -> Icons.Rounded.Inventory2
                                BottomTab.COBRO -> Icons.Rounded.Payments
                                BottomTab.STATS -> Icons.Rounded.Assessment
                            },
                            contentDescription = tab.label, tint = tintColor, modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            tab.label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = tintColor,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
