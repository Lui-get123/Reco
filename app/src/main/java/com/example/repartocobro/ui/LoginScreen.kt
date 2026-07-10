package com.example.repartocobro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repartocobro.ui.components.*
import com.example.repartocobro.ui.theme.*
import com.example.repartocobro.viewmodel.AppUiState

@Composable
fun LoginContent(state: AppUiState, onLogin: (Int) -> Unit, onOpenAdmin: () -> Unit, onExtend: () -> Unit, onAcceptTerms: () -> Unit) {
    var showTermsDialog by remember { mutableStateOf(false) }
    var termsChecked by remember { mutableStateOf(state.hasAcceptedTerms) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Bone, Cream, SkyBlueSoft, Bone)))) {

        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.55f).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LicenseBanner(state.licenseStatus, onExtend)
            Spacer(Modifier.height(32.dp))
            Box(Modifier.size(240.dp), Alignment.Center) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.repartocobro.R.drawable.app_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Reco", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = Graphite, fontSize = 38.sp, textAlign = TextAlign.Center)
            Text("REPARTO & COBRO", style = MaterialTheme.typography.labelLarge,
                color = GraphiteMedium, letterSpacing = 2.sp, textAlign = TextAlign.Center)
        }

        Box(
            Modifier.fillMaxWidth().fillMaxHeight(0.50f).align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(Color.White).padding(24.dp)
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Selecciona tu perfil", style = MaterialTheme.typography.bodySmall,
                        color = GraphiteMedium, fontWeight = FontWeight.Medium)
                    IconButton(
                        onClick = onOpenAdmin,
                        modifier = Modifier.align(Alignment.CenterEnd).size(36.dp).offset(x = 12.dp)
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Admin", tint = GraphiteMedium, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))

                // ── Términos y Condiciones ──
                if (!state.hasAcceptedTerms) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(MustardSoft)
                            .border(1.dp, Mustard.copy(0.3f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = termsChecked, onCheckedChange = { termsChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = Graphite, uncheckedColor = GraphiteLight, checkmarkColor = Color.White))
                        Text("Acepto los ", style = MaterialTheme.typography.bodySmall, color = GraphiteMedium)
                        Text("Términos y Condiciones",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Graphite, modifier = Modifier.clickable { showTermsDialog = true })
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val profileColors = listOf(
                        Pair(SkyBlueSoft, SkyBlue),
                        Pair(SageSoft, Sage),
                        Pair(MustardSoft, Mustard),
                        Pair(Cream, Graphite)
                    )
                    state.collectors.forEachIndexed { idx, c ->
                        val (bg, accent) = profileColors[idx % profileColors.size]
                        val canLogin = state.hasAcceptedTerms || termsChecked
                        Button(
                            onClick = { if (termsChecked && !state.hasAcceptedTerms) onAcceptTerms(); onLogin(c.id) },
                            enabled = canLogin,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                                .border(1.dp, if (canLogin) accent.copy(0.5f) else SoftBorder, RoundedCornerShape(28.dp)),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = Graphite,
                                disabledContainerColor = bg.copy(alpha = 0.4f), disabledContentColor = GraphiteLight),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(28.dp).clip(CircleShape).background(accent.copy(0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Person, contentDescription = null,
                                        tint = accent, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(c.name.uppercase(), fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("© 2026 LuisPG.", style = MaterialTheme.typography.labelSmall, color = GraphiteLight)
                    if (state.hasAcceptedTerms) {
                        Spacer(Modifier.width(4.dp))
                        Text("Términos y Condiciones",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Graphite.copy(alpha = 0.6f), modifier = Modifier.clickable { showTermsDialog = true })
                    }
                }
                Text("Todos los derechos reservados.", style = MaterialTheme.typography.labelSmall,
                    color = GraphiteLight, textAlign = TextAlign.Center)
            }
        }
    }
    if (showTermsDialog) {
        TermsAndConditionsDialog(onDismiss = { showTermsDialog = false })
    }
}

@Composable
fun TermsAndConditionsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = WhiteSurface, shape = RoundedCornerShape(24.dp),
        icon = { Icon(Icons.Rounded.Article, contentDescription = null, tint = Graphite, modifier = Modifier.size(32.dp)) },
        title = {
            Text("Términos y Condiciones", color = Graphite, fontWeight = FontWeight.Bold,
                fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 450.dp).verticalScroll(rememberScrollState())) {
                FullTermsAndConditions.forEachIndexed { index, section ->
                    Text(section.title, fontWeight = FontWeight.Bold, color = Graphite, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(section.content, color = GraphiteMedium, fontSize = 13.sp, lineHeight = 18.sp)
                    if (index < FullTermsAndConditions.lastIndex) {
                        Spacer(Modifier.height(14.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(SoftBorder))
                        Spacer(Modifier.height(14.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Última actualización: Junio 2026", color = GraphiteLight, fontSize = 11.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { AccentButton("Entendido", onDismiss, containerColor = Graphite) }
    )
}
