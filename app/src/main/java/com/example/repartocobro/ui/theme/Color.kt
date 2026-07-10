package com.example.repartocobro.ui.theme

import androidx.compose.ui.graphics.Color

// ── Paleta principal — Profesional & Elegante ───────────────

// Salvia suave — Éxito, confirmaciones, pagos
val Sage               = Color(0xFFA8C9A5)
val SageLight          = Color(0xFFD4E8D2)
val SageSoft           = Color(0xFFEAF2E9)

// Mostaza pálida — Botones secundarios, advertencia leve
val Mustard            = Color(0xFFD4B87A)
val MustardLight       = Color(0xFFEADDBB)
val MustardSoft        = Color(0xFFF5EEDC)

// Azul cielo desaturado — Tarjetas, inputs, campos
val SkyBlue            = Color(0xFF9DBCC6)
val SkyBlueLight       = Color(0xFFCEDDE2)
val SkyBlueSoft        = Color(0xFFE4EEF1)

// Grafito — Textos principales, botones primarios
val Graphite           = Color(0xFF3A3A3A)
val GraphiteMedium     = Color(0xFF5A5A5A)
val GraphiteLight      = Color(0xFF8A8A8A)
val GraphiteMuted      = Color(0xFFAAAAAA)

// ── Fondos y superficies ────────────────────────────────────
val Bone               = Color(0xFFF8F9FA)   // Blanco hueso — fondo principal
val Cream              = Color(0xFFF0EDE0)   // Crema — fondos alt, modales
val WhiteSurface       = Color(0xFFFFFFFF)   // Superficie de tarjetas
val ElevatedSurface    = Color(0xFFF3F3F5)   // Superficie elevada

// ── Status ──────────────────────────────────────────────────
val StatusSuccess      = Color(0xFFA8C9A5)   // Sage = éxito
val StatusError        = Color(0xFFD47B6C)   // Terracota suave
val StatusWarning      = Color(0xFFD4B87A)   // Mostaza = advertencia
val StatusInfo         = Color(0xFF9DBCC6)   // Sky blue = info

// ── Barra de navegación ─────────────────────────────────────
val NavBarBackground   = Color(0xFFFFFFFF)
val NavBarSelected     = Color(0xFF3A3A3A)
val NavBarUnselected   = Color(0xFFAAAAAA)

// ── Bordes ──────────────────────────────────────────────────
val SoftBorder         = Color(0xFFE0E0E0)

// ── Aliases retrocompatibles para minimizar cambios ─────────
val MintPrimary        = SkyBlue
val MintLight          = SkyBlueLight
val MintSoft           = SkyBlueSoft
val LavenderPrimary    = Mustard
val LavenderLight      = MustardLight
val LavenderSoft       = MustardSoft
val YellowPrimary      = Mustard
val YellowLight        = MustardLight
val YellowSoft         = MustardSoft
val PeachPrimary       = StatusError
val PeachLight         = Color(0xFFF2DCD7)
val LightBackground    = Bone
val LightSurface       = WhiteSurface
val LightSurfaceHigh   = ElevatedSurface
val TextDark           = Graphite
val TextMedium         = GraphiteMedium
val TextLight          = GraphiteLight
val TextOnAccent       = Color(0xFFFFFFFF)

// Mantener aliases legacy
val NeonCyan           = StatusInfo
val NeonMagenta        = Mustard
val NeonPurple         = Mustard
val NeonLime           = StatusSuccess
val NeonOrange         = StatusWarning
val NeonPink           = StatusError
val NeonYellow         = Mustard
val NeonSuccess        = StatusSuccess
val NeonError          = StatusError
val NeonWarning        = StatusWarning
val DarkBackground     = Bone
val DarkSurface        = WhiteSurface
val DarkSurfaceHigh    = ElevatedSurface
val DarkCard           = WhiteSurface
val TextPrimary        = Graphite
val TextSecondary      = GraphiteMedium
val TextMuted          = GraphiteLight