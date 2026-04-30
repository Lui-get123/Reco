package com.example.repartocobro.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.repartocobro.model.*
import com.example.repartocobro.ui.components.*
import com.example.repartocobro.ui.theme.*
import com.example.repartocobro.viewmodel.AppUiState
import kotlinx.coroutines.delay

/* ── Tabs para la barra inferior ── */
private enum class BottomTab(val label: String, val emoji: String) {
    REPARTO("Reparto", "📦"), COBRO("Cobro", "💰")
}

/* ── Root ── */
@Composable
fun AppScreen(
    state: AppUiState, onLogin: (Int) -> Unit, onSelectStore: (Int) -> Unit,
    onCloseStoreForm: () -> Unit, onSaveDelivered: (Int, Int, Int) -> Unit,
    onSaveSales: (Int, Int, Int) -> Unit, onMarkCollected: (Int) -> Unit,
    onResetRoute: () -> Unit, onExportPdf: () -> Unit, onDismissMessage: () -> Unit,
    onRedeemCode: (String) -> Unit, onToggleLicenseDialog: () -> Unit
) {
    Scaffold(containerColor = LightBackground) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (!state.licenseStatus.isActive) {
                LicenseLockScreen(state.licenseStatus, state.isValidatingLicense, onRedeemCode)
            } else if (state.selectedCollector == null) {
                LoginContent(state, onLogin, onToggleLicenseDialog)
            } else {
                RouteContent(state, onSelectStore, onCloseStoreForm, onSaveDelivered,
                    onSaveSales, onMarkCollected, onResetRoute, onExportPdf, onToggleLicenseDialog)
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

/* ── Lock screen ── */
@Composable
private fun LicenseLockScreen(licenseStatus: LicenseStatus, isValidating: Boolean, onRedeemCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    val pulse = rememberInfiniteTransition(label = "lk")
    val alpha by pulse.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "a")
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LightBackground, MintSoft, LightBackground)))
        .padding(32.dp).verticalScroll(rememberScrollState()), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("🔒", fontSize = 64.sp)
        Spacer(Modifier.height(12.dp))
        Text("Licencia Requerida", style = MaterialTheme.typography.headlineMedium,
            color = PeachPrimary.copy(alpha = alpha), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(if (licenseStatus.expirationDate != null) "⚠️ Tu licencia expiró el ${licenseStatus.expirationDate}"
            else "Ingresa un código de activación para usar la app",
            color = TextMedium, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(code, { if (it.length <= 20) code = it.uppercase() },
            label = { Text("🔑 Código de activación") }, placeholder = { Text("Ej: MESAÑO-XXXXXX") },
            enabled = !isValidating,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintPrimary,
                unfocusedBorderColor = SoftBorder, cursorColor = MintPrimary,
                focusedLabelColor = MintPrimary, unfocusedLabelColor = TextLight),
            shape = RoundedCornerShape(14.dp), singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text("Formato: MESAÑO-CÓDIGO (ej: MESAÑO-XXXXXX)", color = TextLight, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(20.dp))
        if (isValidating) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(MintPrimary.copy(0.1f)).padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MintPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Validando con servidor...", color = MintPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        } else {
            AccentButton("Activar Licencia", { if (code.contains("-")) onRedeemCode(code) },
                Modifier.fillMaxWidth(), MintPrimary)
        }
    }
}

/* ── License dialog ── */
@Composable
private fun LicenseCodeDialog(isValidating: Boolean, onDismiss: () -> Unit, onRedeem: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = { if (!isValidating) onDismiss() }, containerColor = LightSurface,
        title = { Text("🔑 Extender Licencia", color = MintPrimary, fontWeight = FontWeight.Bold) },
        text = { Column {
            Text("Ingresa un código mensual para añadir 30 días más.", color = TextMedium)
            Spacer(Modifier.height(4.dp))
            Text("Formato: MESAÑO-CÓDIGO", color = TextLight, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(code, { if (it.length <= 20) code = it.uppercase() },
                label = { Text("Código") }, placeholder = { Text("ABRIL2026-AB12XZ") },
                enabled = !isValidating,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MintPrimary, unfocusedBorderColor = SoftBorder,
                    cursorColor = MintPrimary, focusedLabelColor = MintPrimary, unfocusedLabelColor = TextLight),
                shape = RoundedCornerShape(14.dp), singleLine = true, modifier = Modifier.fillMaxWidth())
            if (isValidating) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MintPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Validando...", color = MintPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }},
        confirmButton = {
            if (!isValidating) AccentButton("Activar", { if (code.contains("-")) onRedeem(code) })
        },
        dismissButton = {
            if (!isValidating) TextButton(onDismiss) { Text("Cancelar", color = TextLight) }
        })
}

/* ── License banner ── */
@Composable
private fun LicenseBanner(licenseStatus: LicenseStatus, onExtend: () -> Unit) {
    val days = licenseStatus.daysRemaining
    val (color, text, emoji) = when {
        days > 5 -> Triple(StatusSuccess, "Licencia válida por $days días", "🟢")
        days > 0 -> Triple(StatusWarning, "¡Licencia vence en $days días!", "🟡")
        else -> Triple(StatusError, "Licencia expirada", "🔴")
    }
    Box(Modifier.fillMaxWidth().clip(cardShape).background(color.copy(0.08f))
        .border(1.dp, color.copy(0.25f), cardShape).clickable(onClick = onExtend)
        .padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 18.sp); Spacer(Modifier.width(8.dp))
            Text(text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("🔑", fontSize = 16.sp)
        }
    }
}

/* ── Login ── */
@Composable
private fun LoginContent(state: AppUiState, onLogin: (Int) -> Unit, onExtend: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LightBackground, MintSoft, LavenderSoft, LightBackground)))) {
        // Parte superior: Logo y Títulos
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.6f).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LicenseBanner(state.licenseStatus, onExtend)
            Spacer(Modifier.height(32.dp))
            
            // Logo sin fondo
             Box(
                 Modifier.size(240.dp),
                 Alignment.Center
             ) {
                 androidx.compose.foundation.Image(
                     painter = androidx.compose.ui.res.painterResource(id = com.example.repartocobro.R.drawable.app_logo),
                     contentDescription = "Logo",
                     modifier = Modifier.fillMaxSize(),
                     contentScale = androidx.compose.ui.layout.ContentScale.Fit
                 )
             }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                "Reparto & Cobro",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = TextDark,
                fontSize = 38.sp,
                textAlign = TextAlign.Center
            )
            Text(
                "GESTIÓN DE RUTAS",
                style = MaterialTheme.typography.labelLarge,
                color = TextMedium,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }

        // Parte inferior: Tarjeta blanca con los perfiles
        Box(
            Modifier.fillMaxWidth().fillMaxHeight(0.45f).align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(Color.White).padding(24.dp)
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Selecciona tu perfil",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(Modifier.height(20.dp))
                
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.collectors.forEachIndexed { idx, c ->
                        val bg = if (idx % 2 == 0) MintLight else LavenderLight
                        val accent = if (idx % 2 == 0) MintPrimary else LavenderPrimary
                        
                        Button(
                            onClick = { onLogin(c.id) },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                                .border(1.dp, accent.copy(0.5f), RoundedCornerShape(28.dp)),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = bg,
                                contentColor = TextDark
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (idx % 2 == 0) "🟢" else "🔵", fontSize = 16.sp)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    c.name.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                Text(
                    "© 2026 LuisPG. Todos los derechos reservados.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLight,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* ── Route content with bottom nav ── */
@Composable
private fun RouteContent(
    state: AppUiState, onSelectStore: (Int) -> Unit, onCloseStoreForm: () -> Unit,
    onSaveDelivered: (Int, Int, Int) -> Unit, onSaveSales: (Int, Int, Int) -> Unit,
    onMarkCollected: (Int) -> Unit, onResetRoute: () -> Unit,
    onExportPdf: () -> Unit, onExtendLicense: () -> Unit
) {
    var currentTab by remember { mutableStateOf(BottomTab.REPARTO) }
    val selectedStore = state.stores.firstOrNull { it.id == state.selectedStoreId }

    Column(Modifier.fillMaxSize().background(LightBackground)) {
        // Scrollable content
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(8.dp))
            LicenseBanner(state.licenseStatus, onExtendLicense)
            // Header card
            Box(Modifier.fillMaxWidth().shadow(4.dp, cardShape).clip(cardShape).background(LightSurface)
                .padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(MintLight), Alignment.Center) {
                        Text("📍", fontSize = 24.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(state.route?.name ?: "Ruta", style = MaterialTheme.typography.titleLarge, color = TextDark)
                        Text("👤 ${state.selectedCollector?.name ?: ""}", style = MaterialTheme.typography.bodyMedium, color = TextMedium)
                    }
                }
            }
            // Summary card
            val delivered = state.stores.count { it.isDelivered }
            val collected = state.stores.count { it.isCollected }
            val total = state.stores.size
            val totalMoney = state.stores.filter { it.isCollected }.sumOf { it.collectedValue }
            Box(Modifier.fillMaxWidth().clip(cardShape).background(
                if (currentTab == BottomTab.REPARTO) YellowSoft else MintSoft)
                .border(1.dp, (if (currentTab == BottomTab.REPARTO) YellowPrimary else MintPrimary).copy(0.2f), cardShape)
                .padding(16.dp)) {
                Column {
                    Text("📊 Resumen del Día", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Total Tiendas: $total", color = TextMedium)
                    if (currentTab == BottomTab.REPARTO) {
                        Text("Repartidas: $delivered", color = if (delivered > 0) StatusSuccess else TextMedium, fontWeight = FontWeight.SemiBold)
                        Text("Sin repartir: ${total - delivered}", color = if (total - delivered > 0) StatusWarning else StatusSuccess)
                    } else {
                        Text("Cobradas: $collected", color = StatusSuccess, fontWeight = FontWeight.SemiBold)
                        Text("Pendientes: ${total - collected}", color = if (total - collected > 0) StatusWarning else StatusSuccess)
                        Text("Total Cobrado: \$$totalMoney", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 15.sp)
                    }
                }
            }
            // Quick actions
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                PastelChip("🔄 Reiniciar", PeachLight, PeachPrimary) { onResetRoute() }
                PastelChip("📄 Generar PDF", MintLight, MintPrimary) { onExportPdf() }
            }
            // Section title based on tab
            SectionLabel(if (currentTab == BottomTab.REPARTO) "📦 Tiendas — Reparto" else "💰 Tiendas — Cobro")
            // Store list
            state.stores.forEach { store ->
                val isDone = if (currentTab == BottomTab.REPARTO) store.isDelivered else store.isCollected
                StoreRow(store, store.id == selectedStore?.id, currentTab, isDone) { onSelectStore(store.id) }
                AnimatedVisibility(store.id == selectedStore?.id,
                    enter = expandVertically(tween(350)) + fadeIn(tween(350)),
                    exit = shrinkVertically(tween(250)) + fadeOut(tween(250))) {
                    StoreForm(store, currentTab, onCloseStoreForm, onSaveDelivered, onSaveSales, onMarkCollected)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        // Bottom nav bar
        BottomNavBar(currentTab) { currentTab = it }
    }
}

/* ── Bottom Navigation ── */
@Composable
private fun BottomNavBar(current: BottomTab, onSelect: (BottomTab) -> Unit) {
    Box(Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(NavBarBackground)
        .padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
            BottomTab.values().forEach { tab ->
                val selected = tab == current
                val bgColor by animateColorAsState(
                    if (selected) (if (tab == BottomTab.REPARTO) YellowLight else MintLight) else Color.Transparent,
                    tween(300), label = "bg${tab.name}")
                val textColor = if (selected) (if (tab == BottomTab.REPARTO) YellowPrimary else MintPrimary) else NavBarUnselected
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(bgColor)
                    .clickable { onSelect(tab) }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tab.emoji, fontSize = 22.sp)
                        if (selected) {
                            Spacer(Modifier.width(6.dp))
                            Text(tab.label, fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

/* ── Store row ── */
@Composable
private fun StoreRow(store: Store, isSelected: Boolean, tab: BottomTab, isDone: Boolean, onClick: () -> Unit) {
    val accent = if (tab == BottomTab.REPARTO) YellowPrimary else MintPrimary
    val bgLight = if (tab == BottomTab.REPARTO) YellowSoft else MintSoft
    val doneColor = StatusSuccess
    val borderColor by animateColorAsState(
        when { isDone -> doneColor.copy(0.4f); isSelected -> accent; else -> SoftBorder },
        tween(400), label = "brd")
    Box(Modifier.fillMaxWidth().shadow(2.dp, cardShape).clip(cardShape).background(LightSurface)
        .border(1.dp, borderColor, cardShape).clickable(enabled = !isDone) { onClick() }
        .padding(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape)
                    .background(if (isDone) doneColor.copy(0.1f) else bgLight), Alignment.Center) {
                    Text(if (isDone) "✅" else if (tab == BottomTab.REPARTO) "📦" else "💰", fontSize = 18.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(store.name, fontWeight = FontWeight.Bold, color = TextDark)
                    val statusText = if (tab == BottomTab.REPARTO) {
                        if (isDone) "Repartida ✅" else "Sin repartir ⏳"
                    } else {
                        if (isDone) "Cobrada 🔒" else "Pendiente ⏳"
                    }
                    Text(statusText, style = MaterialTheme.typography.bodySmall,
                        color = if (isDone) doneColor else TextMedium)
                }
                val dateText = if (tab == BottomTab.REPARTO) store.deliveryDate else store.collectionDate
                if (isDone && dateText != null) {
                    Text("📅 $dateText", style = MaterialTheme.typography.labelSmall, color = TextLight)
                }
            }
        }
    }
}

/* ── Store form ── */
@Composable
private fun StoreForm(
    store: Store, tab: BottomTab, onClose: () -> Unit,
    onSaveDelivered: (Int, Int, Int) -> Unit, onSaveSales: (Int, Int, Int) -> Unit,
    onMarkCollected: (Int) -> Unit
) {
    var delEmp by remember(store.id) { mutableStateOf(store.deliveredEmpanadas.toString()) }
    var delDed by remember(store.id) { mutableStateOf(store.deliveredDeditos.toString()) }
    var soldEmp by remember(store.id) { mutableStateOf(store.soldEmpanadas.toString()) }
    var soldDed by remember(store.id) { mutableStateOf(store.soldDeditos.toString()) }
    val dE = delEmp.toIntOrNull() ?: 0; val dD = delDed.toIntOrNull() ?: 0
    val sE = (soldEmp.toIntOrNull() ?: 0).coerceIn(0, dE)
    val sD = (soldDed.toIntOrNull() ?: 0).coerceIn(0, dD)
    val delivTotal = dE * EMPANADA_PRICE + dD * DEDITO_PRICE
    val collTotal = sE * EMPANADA_PRICE + sD * DEDITO_PRICE
    val notColl = (delivTotal - collTotal).coerceAtLeast(0)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MintPrimary, unfocusedBorderColor = SoftBorder,
        cursorColor = MintPrimary, focusedLabelColor = MintPrimary, unfocusedLabelColor = TextLight)
    val accent = if (tab == BottomTab.REPARTO) YellowPrimary else MintPrimary

    Box(Modifier.fillMaxWidth().shadow(3.dp, cardShape).clip(cardShape).background(LightSurface)
        .border(1.dp, accent.copy(0.3f), cardShape).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("🏪 ${store.name}", fontWeight = FontWeight.Bold, color = accent, fontSize = 17.sp)
                TextButton(onClose) { Text("✖ Cerrar", color = PeachPrimary, fontWeight = FontWeight.Bold) }
            }
            SoftDivider()
            if (tab == BottomTab.REPARTO) {
                Text("📦 Apartado de Entregas", fontWeight = FontWeight.Bold, color = YellowPrimary)
                OutlinedTextField(delEmp, { delEmp = it.filter(Char::isDigit) },
                    label = { Text("🥟 Empanadas entregadas") }, colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(delDed, { delDed = it.filter(Char::isDigit) },
                    label = { Text("🌯 Deditos entregados") }, colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Text("💰 Total entregado: \$$delivTotal", fontWeight = FontWeight.Bold, color = TextDark)
                AccentButton("💾 Guardar entregas", { onSaveDelivered(store.id, dE, dD) },
                    Modifier.fillMaxWidth(), YellowPrimary, TextDark)
            } else {
                Text("🧾 Apartado de Cobranza", fontWeight = FontWeight.Bold, color = MintPrimary)
                Text("📊 Base: E:$dE  D:$dD", style = MaterialTheme.typography.bodyMedium, color = TextMedium)
                val isInvalid = (soldEmp.toIntOrNull() ?: 0) > dE || (soldDed.toIntOrNull() ?: 0) > dD
                OutlinedTextField(soldEmp, { soldEmp = it.filter(Char::isDigit) },
                    label = { Text("🥟 Empanadas vendidas") }, colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isInvalid,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(soldDed, { soldDed = it.filter(Char::isDigit) },
                    label = { Text("🌯 Deditos vendidos") }, colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isInvalid,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                if (isInvalid) {
                    Text("⚠️ Los vendidos no pueden superar los entregados.",
                        color = StatusError, fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("✅ Cobrado: \$$collTotal", fontWeight = FontWeight.Bold, color = StatusSuccess)
                    Text("❌ No cobrado: \$$notColl", fontWeight = FontWeight.Bold, color = StatusError)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    AccentButton("💾 Guardar", { if (!isInvalid) onSaveSales(store.id, sE, sD) }, 
                        Modifier.weight(1f), MintPrimary, enabled = !isInvalid)
                    AccentButton("✅ Cobrada", { if (!isInvalid) onMarkCollected(store.id) }, 
                        Modifier.weight(1f), StatusSuccess, enabled = !isInvalid)
                }
            }
        }
    }
}
