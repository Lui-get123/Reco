package com.example.repartocobro.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repartocobro.model.LicenseStatus
import com.example.repartocobro.ui.components.*
import com.example.repartocobro.ui.theme.*

/* ── Lock screen ── */
@Composable
fun LicenseLockScreen(licenseStatus: LicenseStatus, isValidating: Boolean, onRedeemCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    val pulse = rememberInfiniteTransition(label = "lk")
    val alpha by pulse.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "a")

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Bone, Cream, Bone)))
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        Arrangement.Center, Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null,
            tint = Graphite.copy(alpha = alpha), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Licencia Requerida", style = MaterialTheme.typography.headlineMedium,
            color = Graphite.copy(alpha = alpha), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            if (licenseStatus.expirationDate != null) "Tu licencia expiró el ${licenseStatus.expirationDate}"
            else "Ingresa un código de activación para usar la app",
            color = GraphiteMedium, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(code, { if (it.length <= 12) code = it.uppercase() },
            label = { Text("Código de activación") },
            placeholder = { Text("Ej: A7K2M9X4P1B3") },
            leadingIcon = { Icon(Icons.Rounded.VpnKey, contentDescription = null, tint = SkyBlue) },
            enabled = !isValidating,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Graphite, unfocusedBorderColor = SoftBorder,
                cursorColor = Graphite, focusedLabelColor = Graphite, unfocusedLabelColor = GraphiteLight),
            shape = RoundedCornerShape(14.dp), singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text("Ingresa tu serial de 12 caracteres alfanuméricos",
            color = GraphiteLight, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(20.dp))
        if (isValidating) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(SkyBlueSoft).padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Graphite, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Validando con servidor...", color = Graphite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        } else {
            AccentButton("Activar Licencia", { if (code.length == 12) onRedeemCode(code) },
                Modifier.fillMaxWidth(), Graphite)
        }
    }
}

/* ── License dialog ── */
@Composable
fun LicenseCodeDialog(isValidating: Boolean, onDismiss: () -> Unit, onRedeem: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isValidating) onDismiss() },
        containerColor = WhiteSurface,
        shape = RoundedCornerShape(24.dp),
        icon = { Icon(Icons.Rounded.Key, contentDescription = null, tint = Graphite, modifier = Modifier.size(28.dp)) },
        title = { Text("Extender Licencia", color = Graphite, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Ingresa un código quincenal para añadir 15 días más.", color = GraphiteMedium)
                Spacer(Modifier.height(4.dp))
                Text("Serial de 12 caracteres alfanuméricos", color = GraphiteLight, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(code, { if (it.length <= 12) code = it.uppercase() },
                    label = { Text("Código") }, placeholder = { Text("A7K2M9X4P1B3") },
                    leadingIcon = { Icon(Icons.Rounded.VpnKey, contentDescription = null, tint = SkyBlue) },
                    enabled = !isValidating,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Graphite, unfocusedBorderColor = SoftBorder,
                        cursorColor = Graphite, focusedLabelColor = Graphite, unfocusedLabelColor = GraphiteLight),
                    shape = RoundedCornerShape(14.dp), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (isValidating) {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Graphite, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Validando...", color = Graphite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            if (!isValidating) AccentButton("Activar", { if (code.length == 12) onRedeem(code) })
        },
        dismissButton = {
            if (!isValidating) TextButton(onDismiss) { Text("Cancelar", color = GraphiteLight) }
        }
    )
}

/* ── License banner ── */
@Composable
fun LicenseBanner(licenseStatus: LicenseStatus, onExtend: () -> Unit) {
    val days = licenseStatus.daysRemaining
    val (color, text) = when {
        days > 5 -> Pair(Sage, "Licencia válida por $days días")
        days > 0 -> Pair(Mustard, "Licencia vence en $days días")
        else -> Pair(StatusError, "Licencia expirada")
    }
    Box(Modifier.fillMaxWidth().clip(cardShape).background(color.copy(0.10f))
        .border(1.dp, color.copy(0.25f), cardShape).clickable(onClick = onExtend)
        .padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
            Spacer(Modifier.width(10.dp))
            Text(text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.Key, contentDescription = "Extender", tint = color, modifier = Modifier.size(18.dp))
        }
    }
}
