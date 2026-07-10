package com.example.repartocobro.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.repartocobro.ui.theme.*
import kotlinx.coroutines.delay

val cardShape = RoundedCornerShape(18.dp)

@Composable
fun SoftDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(SoftBorder))
}

@Composable
fun SectionLabel(text: String, icon: ImageVector? = null) {
    Row(
        Modifier.padding(start = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Graphite, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = TextDark, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AccentButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    containerColor: Color = Graphite, contentColor: Color = TextOnAccent,
    enabled: Boolean = true
) {
    Button(onClick = onClick, modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

@Composable
fun PastelChip(text: String, bgColor: Color, textColor: Color = TextDark, onClick: () -> Unit = {}) {
    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(bgColor)
        .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor) }
}

@Composable
fun ActionChip(
    icon: ImageVector, label: String,
    bgColor: Color, accentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val alpha = if (enabled) 1f else 0.5f
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor.copy(alpha = bgColor.alpha * alpha))
            .border(1.dp, accentColor.copy(alpha = 0.25f * alpha), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = accentColor.copy(alpha = alpha), modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                color = accentColor.copy(alpha = alpha), maxLines = 1)
        }
    }
}

/* ── Top alert banner ── */
private enum class AlertType { SUCCESS, ERROR, INFO }

private fun classifyMessage(msg: String): AlertType = when {
    msg.contains("cobrada", true) || msg.contains("actualizada", true)
            || msg.contains("generado", true) || msg.contains("guardada", true)
            || msg.contains("activada", true) || msg.contains("reiniciada", true)
            || msg.contains("deuda", true) || msg.contains("registrada", true) -> AlertType.SUCCESS
    msg.contains("error", true) || msg.contains("no se pudo", true)
            || msg.contains("bloqueada", true) -> AlertType.ERROR
    else -> AlertType.INFO
}

@Composable
fun TopAlertBanner(message: String?, onDismiss: () -> Unit) {
    var displayedMessage by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(message) {
        if (message != null) { displayedMessage = message; visible = true; delay(2800); visible = false; delay(400); onDismiss() }
        else visible = false
    }
    AnimatedVisibility(visible = visible,
        enter = slideInVertically(tween(350)) { -it } + fadeIn(tween(350)),
        exit = slideOutVertically(tween(300)) { -it } + fadeOut(tween(300)),
        modifier = Modifier.fillMaxWidth().zIndex(10f)
    ) {
        val msg = displayedMessage ?: return@AnimatedVisibility
        val alertType = classifyMessage(msg)
        val (accent, bgColor, icon) = when (alertType) {
            AlertType.SUCCESS -> Triple(StatusSuccess, StatusSuccess.copy(0.12f), Icons.Rounded.CheckCircle)
            AlertType.ERROR -> Triple(StatusError, StatusError.copy(0.12f), Icons.Rounded.Warning)
            AlertType.INFO -> Triple(StatusInfo, StatusInfo.copy(0.12f), Icons.Rounded.Info)
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
            .background(LightSurface).background(bgColor)
            .border(1.dp, accent.copy(0.3f), RoundedCornerShape(14.dp))
            .clickable { visible = false }.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(msg, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = TextLight,
                    modifier = Modifier.size(18.dp).padding(start = 8.dp))
            }
        }
    }
}
