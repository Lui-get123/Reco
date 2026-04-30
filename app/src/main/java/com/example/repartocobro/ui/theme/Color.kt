package com.example.repartocobro.ui.theme

import androidx.compose.ui.graphics.Color

// ── Paleta principal (estilo moderno pastel) ────────────────
val MintPrimary        = Color(0xFF8CD4C4)   // Verde menta principal
val MintLight          = Color(0xFFD4F0EA)   // Menta claro para fondos de tarjeta
val MintSoft           = Color(0xFFE8F8F4)   // Menta muy suave para backgrounds

val LavenderPrimary    = Color(0xFFCDB4DB)   // Lavanda principal
val LavenderLight      = Color(0xFFEDE3F2)   // Lavanda claro
val LavenderSoft       = Color(0xFFF5F0F8)   // Lavanda muy suave

val YellowPrimary      = Color(0xFFF5D680)   // Amarillo cálido
val YellowLight        = Color(0xFFFFF4D6)   // Amarillo claro para tarjetas
val YellowSoft         = Color(0xFFFFFAEB)   // Amarillo muy suave

val PeachPrimary       = Color(0xFFF5A68C)   // Durazno / coral suave
val PeachLight         = Color(0xFFFDE0D6)   // Durazno claro

// ── Fondos y superficies ────────────────────────────────────
val LightBackground    = Color(0xFFF8F9FC)   // Fondo general (gris casi blanco)
val LightSurface       = Color(0xFFFFFFFF)   // Superficie de tarjetas
val LightSurfaceHigh   = Color(0xFFF0F1F5)   // Superficie elevada

// ── Textos ──────────────────────────────────────────────────
val TextDark           = Color(0xFF2D3142)   // Texto principal oscuro
val TextMedium         = Color(0xFF5A5E72)   // Texto secundario
val TextLight          = Color(0xFF9A9DB0)   // Texto apagado / hints
val TextOnAccent       = Color(0xFFFFFFFF)   // Texto sobre colores de acento

// ── Status ──────────────────────────────────────────────────
val StatusSuccess      = Color(0xFF5CB85C)   // Verde éxito
val StatusError        = Color(0xFFE85D5D)   // Rojo error
val StatusWarning      = Color(0xFFF0AD4E)   // Naranja advertencia
val StatusInfo         = Color(0xFF5BC0DE)   // Azul info

// ── Barra de navegación ─────────────────────────────────────
val NavBarBackground   = Color(0xFFFFFFFF)
val NavBarSelected     = Color(0xFF6DB5A0)   // Menta más oscuro para seleccionados
val NavBarUnselected   = Color(0xFFB0B3C5)   // Gris para no seleccionados

// ── Sombras y bordes ────────────────────────────────────────
val SoftBorder         = Color(0xFFE8E9EE)   // Borde suave de tarjetas
val SoftShadow         = Color(0x1A000000)   // Sombra suave

// ── Mantener aliases para retrocompatibilidad con alertas ───
val NeonCyan           = StatusInfo
val NeonMagenta        = LavenderPrimary
val NeonPurple         = LavenderPrimary
val NeonLime           = StatusSuccess
val NeonOrange         = StatusWarning
val NeonPink           = PeachPrimary
val NeonYellow         = YellowPrimary
val NeonSuccess        = StatusSuccess
val NeonError          = StatusError
val NeonWarning        = StatusWarning
val DarkBackground     = LightBackground
val DarkSurface        = LightSurface
val DarkSurfaceHigh    = LightSurfaceHigh
val DarkCard           = LightSurface
val TextPrimary        = TextDark
val TextSecondary      = TextMedium
val TextMuted          = TextLight