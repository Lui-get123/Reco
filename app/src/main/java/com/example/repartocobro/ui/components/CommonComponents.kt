package com.example.repartocobro.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = TextDark,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
}

@Composable
fun AccentButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    containerColor: Color = MintPrimary, contentColor: Color = TextOnAccent,
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

/* ── Top alert banner ── */
private enum class AlertType { SUCCESS, ERROR, INFO }

private fun classifyMessage(msg: String): AlertType = when {
    msg.contains("cobrada", true) || msg.contains("actualizada", true)
            || msg.contains("generado", true) || msg.contains("✅") -> AlertType.SUCCESS
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
            AlertType.SUCCESS -> Triple(StatusSuccess, StatusSuccess.copy(0.12f), "🎉")
            AlertType.ERROR -> Triple(StatusError, StatusError.copy(0.12f), "⚠️")
            AlertType.INFO -> Triple(StatusInfo, StatusInfo.copy(0.12f), "💡")
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
            .background(LightSurface).background(bgColor)
            .border(1.dp, accent.copy(0.3f), RoundedCornerShape(14.dp))
            .clickable { visible = false }.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 22.sp); Spacer(Modifier.width(10.dp))
                Text(msg, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("✕", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
