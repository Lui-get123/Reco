package com.example.repartocobro.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repartocobro.model.Store
import com.example.repartocobro.model.StoreProduct
import com.example.repartocobro.ui.components.*
import com.example.repartocobro.ui.theme.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/* ── Tabs para la barra inferior ── */
enum class BottomTab(val label: String, val icon: @Composable () -> Unit) {
    REPARTO("Reparto", { Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(22.dp)) }),
    COBRO("Cobro", { Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(22.dp)) }),
    STATS("Estadísticas", { Icon(Icons.Rounded.Assessment, contentDescription = null, modifier = Modifier.size(22.dp)) })
}

/* ── Store row ── */
@Composable
fun StoreRow(store: Store, isSelected: Boolean, tab: BottomTab, isDone: Boolean, onClick: () -> Unit) {
    val accent = if (tab == BottomTab.REPARTO) Mustard else SkyBlue
    val bgLight = if (tab == BottomTab.REPARTO) MustardSoft else SkyBlueSoft
    val showStatus = tab == BottomTab.COBRO
    val actualDone = if (tab == BottomTab.REPARTO) false else isDone
    val doneColor = Sage
    val hasDebt = store.pendingDebtTotal > 0
    val debtColor = StatusWarning
    val borderColor by animateColorAsState(
        when {
            hasDebt && tab == BottomTab.COBRO -> debtColor.copy(0.5f)
            actualDone -> doneColor.copy(0.4f)
            isSelected -> accent
            else -> SoftBorder
        },
        tween(400), label = "brd")

    Box(Modifier.fillMaxWidth().shadow(2.dp, cardShape).clip(cardShape).background(WhiteSurface)
        .border(1.dp, borderColor, cardShape).clickable { onClick() }
        .padding(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape)
                    .background(
                        when {
                            hasDebt && tab == BottomTab.COBRO -> debtColor.copy(0.12f)
                            actualDone -> doneColor.copy(0.12f)
                            else -> bgLight
                        }
                    ), Alignment.Center) {
                    Icon(
                        when {
                            hasDebt && tab == BottomTab.COBRO -> Icons.Rounded.Warning
                            actualDone -> Icons.Rounded.CheckCircle
                            tab == BottomTab.REPARTO -> Icons.Rounded.Inventory2
                            else -> Icons.Rounded.Payments
                        },
                        contentDescription = null,
                        tint = when {
                            hasDebt && tab == BottomTab.COBRO -> debtColor
                            actualDone -> doneColor
                            else -> accent
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(store.name, fontWeight = FontWeight.Bold, color = Graphite)
                    if (showStatus) {
                        val statusText = when {
                            hasDebt -> "Debe: \$${store.pendingDebtTotal}"
                            isDone -> "Cobrada"
                            else -> "Pendiente"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when {
                                    hasDebt -> Icons.Rounded.Warning
                                    isDone -> Icons.Rounded.CheckCircle
                                    else -> Icons.Rounded.Schedule
                                },
                                contentDescription = null,
                                tint = when {
                                    hasDebt -> debtColor
                                    isDone -> doneColor
                                    else -> GraphiteMedium
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(statusText, style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    hasDebt -> debtColor
                                    isDone -> doneColor
                                    else -> GraphiteMedium
                                },
                                fontWeight = if (hasDebt) FontWeight.Bold else FontWeight.Normal)
                        }
                    } else {
                        // Modo reparto: mostrar cantidades actuales dinámicamente
                        val summary = store.products.filter { it.deliveredQuantity > 0 }
                            .joinToString("  ") { "${it.product.name.take(1).uppercase()}:${it.deliveredQuantity}" }
                            .ifEmpty { "Sin entregas" }
                        Text("Entrega: $summary",
                            style = MaterialTheme.typography.bodySmall, color = GraphiteMedium)
                    }
                }
                // Badge de deuda en la esquina
                if (hasDebt && tab == BottomTab.COBRO) {
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(debtColor.copy(0.15f))
                            .border(1.dp, debtColor.copy(0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("\$${store.pendingDebtTotal}", fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = debtColor)
                    }
                } else if (showStatus && isDone && store.collectionDate != null && !hasDebt) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null,
                            tint = GraphiteLight, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(store.collectionDate, style = MaterialTheme.typography.labelSmall, color = GraphiteLight)
                    }
                }
            }
        }
    }
}

/* ── Store form ── */
@Composable
fun StoreForm(
    store: Store, tab: BottomTab, onClose: () -> Unit,
    onSaveDelivered: (Int, List<StoreProduct>) -> Unit,
    onMarkCollected: (Int, List<StoreProduct>, String?) -> Unit,
    onMarkPendingPayment: (Int, List<StoreProduct>, String?) -> Unit = { _, _, _ -> },
    onCollectDebt: (Int) -> Unit = { },
    editedDeliveries: MutableMap<Int, Map<Int, Int>> = mutableMapOf(),
    isBlocked: Boolean = false
) {
    val currentEdited = editedDeliveries[store.id] ?: emptyMap()
    
    // States for delivery
    val deliveriesState = remember(store.id) {
        mutableStateMapOf<Int, String>().apply {
            store.products.forEach { sp ->
                val initVal = currentEdited[sp.product.id] ?: sp.deliveredQuantity
                put(sp.product.id, initVal.toString())
            }
        }
    }
    
    // States for sales
    val salesState = remember(store.id) {
        mutableStateMapOf<Int, String>().apply {
            store.products.forEach { sp ->
                put(sp.product.id, if (sp.soldQuantity == 0) "" else sp.soldQuantity.toString())
            }
        }
    }
    
    var obs by remember(store.id) { mutableStateOf(store.observations ?: "") }
    
    // Build updated product list
    val updatedProducts = store.products.map { sp ->
        val delStr = deliveriesState[sp.product.id] ?: "0"
        val soldStr = salesState[sp.product.id] ?: "0"
        val dQty = delStr.toIntOrNull() ?: 0
        val sQty = (soldStr.toIntOrNull() ?: 0).coerceIn(0, dQty)
        StoreProduct(store.id, sp.product, dQty, sQty)
    }
    
    val delivTotal = updatedProducts.sumOf { it.deliveredValue }
    val collTotal = updatedProducts.sumOf { it.collectedValue }
    val notColl = (delivTotal - collTotal).coerceAtLeast(0)
    
    val isInvalid = store.products.any { sp ->
        val delStr = deliveriesState[sp.product.id] ?: "0"
        val soldStr = salesState[sp.product.id] ?: "0"
        val dQty = delStr.toIntOrNull() ?: 0
        val sQty = soldStr.toIntOrNull() ?: 0
        sQty > dQty
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Graphite, unfocusedBorderColor = SoftBorder,
        cursorColor = Graphite, focusedLabelColor = Graphite, unfocusedLabelColor = GraphiteLight)
    val accent = if (tab == BottomTab.REPARTO) Mustard else SkyBlue
    val hasDebt = store.pendingDebtTotal > 0

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (e: Exception) { /* Ignore */ }
    }

    Box(Modifier.fillMaxWidth().shadow(3.dp, cardShape).clip(cardShape).background(WhiteSurface)
        .border(1.dp, accent.copy(0.3f), cardShape).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Storefront, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(store.name, fontWeight = FontWeight.Bold, color = accent, fontSize = 17.sp)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = StatusError, modifier = Modifier.size(20.dp))
                }
            }
            SoftDivider()

            if (hasDebt && tab == BottomTab.COBRO) {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StatusWarning.copy(0.1f))
                        .border(1.dp, StatusWarning.copy(0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, contentDescription = null,
                                tint = StatusWarning, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Deuda Pendiente", fontWeight = FontWeight.Bold,
                                color = StatusWarning, fontSize = 15.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Monto adeudado:", color = Graphite,
                                fontWeight = FontWeight.Medium)
                            Text("\$${store.pendingDebtTotal}", fontWeight = FontWeight.Bold,
                                color = StatusWarning, fontSize = 16.sp)
                        }
                        AccentButton(
                            "Cobrar Deuda",
                            { onCollectDebt(store.id) },
                            Modifier.fillMaxWidth(),
                            StatusWarning,
                            enabled = !isBlocked
                        )
                    }
                }
                SoftDivider()
            }

            if (tab == BottomTab.REPARTO) {
                Text("Apartado de Entregas", fontWeight = FontWeight.Bold, color = Mustard)
                
                store.products.forEachIndexed { index, sp ->
                    OutlinedTextField(
                        value = deliveriesState[sp.product.id] ?: "",
                        onValueChange = { 
                            deliveriesState[sp.product.id] = it.filter(Char::isDigit)
                            editedDeliveries[store.id] = deliveriesState.mapValues { entry -> entry.value.toIntOrNull() ?: 0 }
                        },
                        label = { Text("${sp.product.name} entregados") }, 
                        colors = fieldColors, enabled = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp), 
                        modifier = Modifier.fillMaxWidth().let { if (index == 0) it.focusRequester(focusRequester) else it }
                    )
                }

                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                    Text("Total: \$$delivTotal", fontWeight = FontWeight.Bold, color = Graphite, modifier = Modifier.weight(1f))
                    AccentButton("Guardar", { onSaveDelivered(store.id, updatedProducts) }, Modifier.weight(1f), Mustard)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Receipt, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Apartado de Cobranza", fontWeight = FontWeight.Bold, color = SkyBlue)
                }
                
                val baseSummary = store.products.joinToString("  ") { "${it.product.name.take(1).uppercase()}:${it.deliveredQuantity}" }
                Text("Base: $baseSummary",
                    style = MaterialTheme.typography.bodyMedium, color = GraphiteMedium)
                
                store.products.forEachIndexed { index, sp ->
                    val dQty = (deliveriesState[sp.product.id] ?: "0").toIntOrNull() ?: 0
                    val sQtyStr = salesState[sp.product.id] ?: "0"
                    val isProdInvalid = (sQtyStr.toIntOrNull() ?: 0) > dQty
                    OutlinedTextField(
                        value = salesState[sp.product.id] ?: "", 
                        onValueChange = { salesState[sp.product.id] = it.filter(Char::isDigit) },
                        label = { Text("${sp.product.name} vendidos") }, 
                        colors = fieldColors, enabled = !isBlocked,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isProdInvalid, 
                        shape = RoundedCornerShape(14.dp), 
                        modifier = Modifier.fillMaxWidth().let { if (index == 0) it.focusRequester(focusRequester) else it }
                    )
                }
                
                OutlinedTextField(obs, { obs = it },
                    label = { Text("Observaciones / Novedades") }, colors = fieldColors, enabled = !isBlocked,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 4)

                if (isInvalid) {
                    Text("Los vendidos no pueden superar los entregados.",
                        color = StatusError, fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Cobrado: \$$collTotal", fontWeight = FontWeight.Bold, color = Sage)
                    Text("No cobrado: \$$notColl", fontWeight = FontWeight.Bold, color = StatusError)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    AccentButton("Cobrada", { if (!isInvalid) onMarkCollected(store.id, updatedProducts, obs.ifBlank { null }) },
                        Modifier.weight(1f), Sage, enabled = !isInvalid && !isBlocked)
                    AccentButton("Deuda", { if (!isInvalid) onMarkPendingPayment(store.id, updatedProducts, obs.ifBlank { null }) },
                        Modifier.weight(1f), StatusWarning, enabled = !isInvalid && !isBlocked && collTotal > 0)
                }
            }
        }
    }
}
