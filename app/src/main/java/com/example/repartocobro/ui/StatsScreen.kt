package com.example.repartocobro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repartocobro.model.Store
import com.example.repartocobro.ui.components.SoftDivider
import com.example.repartocobro.ui.components.cardShape
import com.example.repartocobro.ui.theme.*

@Composable
fun StatsScreen(stores: List<Store>) {
    // Animate in
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { 
        kotlinx.coroutines.delay(300)
        visible = true 
    }
    val animProgress by animateFloatAsState(
        if (visible) 1f else 0f, tween(800), label = "stats"
    )

    // Compute stats
    val total = stores.size
    val delivered = remember(stores) { stores.count { it.isDelivered } }
    val collected = remember(stores) { stores.count { it.isCollected } }
    val totalMoney = remember(stores) { stores.sumOf { it.collectedValue } }
    val totalDeliveredValue = remember(stores) { stores.sumOf { it.deliveredValue } }
    val avgPerStore = remember(totalMoney, collected) { if (collected > 0) totalMoney / collected else 0 }
    val collectionRate = remember(total, collected) { if (total > 0) (collected.toFloat() / total * 100).toInt() else 0 }
    val totalDebt = remember(stores) { stores.sumOf { it.pendingDebtTotal } }

    val storesWithSales by remember(stores) { derivedStateOf { stores.filter { store -> store.products.any { it.soldQuantity > 0 } } } }
    val storesWithDebt by remember(stores) { derivedStateOf { stores.filter { it.pendingDebtTotal > 0 } } }
    
    val allProducts = remember(stores) {
        stores.flatMap { it.products }.map { it.product }.distinctBy { it.id }.sortedBy { it.id }
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Assessment, contentDescription = null, tint = Graphite, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("Estadísticas del Día", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = Graphite)
        }

        // KPI Cards Row
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
            KpiCard("Cobrado", "\$$totalMoney", Sage, Icons.Rounded.Payments, Modifier.weight(1f))
            KpiCard("Deuda", "\$$totalDebt", StatusWarning, Icons.Rounded.Warning, Modifier.weight(1f))
            KpiCard("Tasa", "$collectionRate%", Mustard, Icons.Rounded.Inventory2, Modifier.weight(1f))
        }

        // Donut charts
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DonutCard(
                title = "Progreso de Cobro",
                done = collected,
                total = total,
                doneColor = SkyBlue,
                pendingColor = SoftBorder,
                animProgress = animProgress,
                modifier = Modifier.weight(1f)
            )
            // Donut: Total Cobrado / Total Entregado
            val cobradoFraction = if (totalDeliveredValue > 0) totalMoney.toFloat() / totalDeliveredValue else 0f
            DonutMoneyCard(
                title = "Cobrado / Entregado",
                collected = totalMoney,
                delivered = totalDeliveredValue,
                fraction = cobradoFraction.coerceIn(0f, 1f),
                doneColor = Sage,
                pendingColor = SoftBorder,
                animProgress = animProgress,
                modifier = Modifier.weight(1f)
            )
        }

        // Bar chart — Cobrado por tienda (Solo las que tienen ventas para no saturar)
            if (storesWithSales.isNotEmpty()) {
                BarChartCard(
                    title = "Cobrado por tienda",
                    stores = storesWithSales,
                    valueSelector = { it.collectedValue },
                    barColor = Sage,
                    animProgress = animProgress
                )

                allProducts.forEachIndexed { index, prod ->
                    GroupedBarChartCard(
                        title = "${prod.name}: Entregados vs Vendidos",
                        stores = storesWithSales,
                        value1Selector = { store -> store.products.firstOrNull { it.product.id == prod.id }?.deliveredQuantity ?: 0 },
                        value2Selector = { store -> store.products.firstOrNull { it.product.id == prod.id }?.soldQuantity ?: 0 },
                        color1 = if (index % 2 == 0) Mustard else SkyBlue,
                        color2 = Sage,
                        label1 = "Entregados",
                        label2 = "Vendidos",
                        animProgress = animProgress
                    )
                }
            } else {
                // Mensaje si no hay ventas aún
                Box(Modifier.fillMaxWidth().shadow(2.dp, cardShape).clip(cardShape).background(WhiteSurface).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Registra ventas para ver las gráficas detalladas", color = GraphiteMedium, textAlign = TextAlign.Center)
                }
            }

        Spacer(Modifier.height(16.dp))
    }
}

/* ── KPI Card ── */
@Composable
private fun KpiCard(label: String, value: String, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier.shadow(2.dp, cardShape).clip(cardShape)
            .background(WhiteSurface)
            .border(1.dp, color.copy(0.15f), cardShape)
            .padding(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(color.copy(0.12f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Graphite)
            Text(label, fontSize = 11.sp, color = GraphiteMedium)
        }
    }
}

/* ── Donut Chart Card ── */
@Composable
private fun DonutCard(
    title: String, done: Int, total: Int,
    doneColor: Color, pendingColor: Color,
    animProgress: Float, modifier: Modifier = Modifier
) {
    val fraction = if (total > 0) done.toFloat() / total else 0f
    val sweepAngle = 360f * fraction * animProgress

    Box(
        modifier.shadow(2.dp, cardShape).clip(cardShape)
            .background(WhiteSurface)
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Graphite)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Background ring
                    drawArc(
                        color = pendingColor, startAngle = 0f, sweepAngle = 360f,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    // Progress arc
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = doneColor, startAngle = -90f, sweepAngle = sweepAngle,
                            useCenter = false, topLeft = topLeft, size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$done/$total", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Graphite)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${(fraction * 100).toInt()}% completado", fontSize = 12.sp, color = GraphiteMedium)
        }
    }
}
/* ── Donut Money Card (Cobrado / Entregado) ── */
@Composable
private fun DonutMoneyCard(
    title: String, collected: Int, delivered: Int,
    fraction: Float, doneColor: Color, pendingColor: Color,
    animProgress: Float, modifier: Modifier = Modifier
) {
    val sweepAngle = 360f * fraction * animProgress
    val pct = (fraction * 100).toInt()

    Box(
        modifier.shadow(2.dp, cardShape).clip(cardShape)
            .background(WhiteSurface)
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Graphite, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    drawArc(
                        color = pendingColor, startAngle = 0f, sweepAngle = 360f,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = doneColor, startAngle = -90f, sweepAngle = sweepAngle,
                            useCenter = false, topLeft = topLeft, size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$pct%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Graphite)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("\$$collected", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Sage)
            Text("de \$$delivered", fontSize = 11.sp, color = GraphiteMedium)
        }
    }
}

/* ── Horizontal Bar Chart Card ── */
@Composable
private fun BarChartCard(
    title: String, stores: List<Store>,
    valueSelector: (Store) -> Int, barColor: Color,
    animProgress: Float
) {
    val maxValue = stores.maxOfOrNull(valueSelector) ?: 1

    Box(
        Modifier.fillMaxWidth().shadow(2.dp, cardShape).clip(cardShape)
            .background(WhiteSurface).padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Graphite)
            SoftDivider()
            stores.forEach { store ->
                val value = valueSelector(store)
                val fraction = if (maxValue > 0) value.toFloat() / maxValue else 0f
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        store.name,
                        fontSize = 11.sp, color = GraphiteMedium,
                        modifier = Modifier.width(80.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.weight(1f).height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(SoftBorder)
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction * animProgress)
                                .clip(RoundedCornerShape(7.dp))
                                .background(barColor)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "\$$value",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = Graphite, modifier = Modifier.width(75.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

/* ── Grouped Bar Chart Card ── */
@Composable
private fun GroupedBarChartCard(
    title: String, stores: List<Store>,
    value1Selector: (Store) -> Int, value2Selector: (Store) -> Int,
    color1: Color, color2: Color,
    label1: String, label2: String,
    animProgress: Float
) {
    val maxValue = stores.maxOfOrNull { maxOf(value1Selector(it), value2Selector(it)) } ?: 1

    Box(
        Modifier.fillMaxWidth().shadow(2.dp, cardShape).clip(cardShape)
            .background(WhiteSurface).padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Graphite)
            // Legend
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
                LegendDot(color1, label1)
                LegendDot(color2, label2)
            }
            SoftDivider()
            stores.forEach { store ->
                val v1 = value1Selector(store)
                val v2 = value2Selector(store)
                val f1 = if (maxValue > 0) v1.toFloat() / maxValue else 0f
                val f2 = if (maxValue > 0) v2.toFloat() / maxValue else 0f

                Column(Modifier.fillMaxWidth()) {
                    Text(store.name, fontSize = 11.sp, color = GraphiteMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(SoftBorder)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(f1 * animProgress)
                                .clip(RoundedCornerShape(5.dp)).background(color1))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("$v1", fontSize = 12.sp, color = color1, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(SoftBorder)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(f2 * animProgress)
                                .clip(RoundedCornerShape(5.dp)).background(color2))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("$v2", fontSize = 12.sp, color = color2, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = GraphiteMedium)
    }
}
