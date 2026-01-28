
package com.acesur.solarpvtracker.ui.theme

import androidx.compose.ui.graphics.Color

// === Solar Kinetic Palette ===

// Primary Brand Colors (Energy & Sun)
val EnergyOrange = Color(0xFFFF6D00) // Vibrant Solar Flare
val EnergyOrangeLight = Color(0xFFFF9E40)
val EnergyOrangeDark = Color(0xFFC43C00)

val TechBlue = Color(0xFF0277BD) // Solar Panel Silicon Blue
val TechBlueLight = Color(0xFF4FC3F7) // Lighter blue for better visibility
val TechBlueDark = Color(0xFF004C8C)

val SunGold = Color(0xFFFFD600) // Direct Sunlight
val SunGoldLight = Color(0xFFFFFF52)
val SunGoldDark = Color(0xFFC7A500)

// Semantic Colors
val EfficientGreen = Color(0xFF00C853) // High Efficiency/Good
val EfficientGreenLight = Color(0xFF5EFC82)

val WarningRed = Color(0xFFD50000) // Error/Offline
val WarningRedLight = Color(0xFFFF5131)

// Light Theme Surface Colors
val LightBackground = Color(0xFFFAFAFA) // Very light grey/white
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F4F8) // Subtle cool tint for cards

val LightOnPrimary = Color(0xFFFFFFFF)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF191C1E)
val LightOnSurface = Color(0xFF191C1E)
val LightOnSurfaceVariant = Color(0xFF43474E)

// Dark Theme Surface Colors (Deep Space/Night Mode)
val DarkBackground = Color(0xFF070B0E) // Even deeper blue-black
val DarkSurface = Color(0xFF11171C) // Deep space grey
val DarkSurfaceVariant = Color(0xFF1B2329) // Subtle elevation

val DarkOnPrimary = Color(0xFF1A1C1E)
val DarkOnSecondary = Color(0xFF00325B)
val DarkOnBackground = Color(0xFFE1E2E4)
val DarkOnSurface = Color(0xFFE1E2E4)
val DarkOnSurfaceVariant = Color(0xFFC3C7CF)

// Gradients/Effects
val GradientDayStart = EnergyOrange
val GradientDayEnd = SunGold

val GradientNightStart = Color(0xFF2C3E50)
val GradientNightEnd = Color(0xFF000000)

// === Legacy Compatibility / Aliases ===
// These ensure existing code referncing old names still works, but uses the new palette

val SolarOrange = EnergyOrange
val SolarOrangeLight = EnergyOrangeLight
val SolarOrangeDark = EnergyOrangeDark

val SkyBlue = TechBlueLight
val SkyBlueLight = Color(0xFF8BF6FF) // Keep original light or map to new
val SkyBlueDark = TechBlueDark

val SunYellow = SunGold
val SunYellowLight = SunGoldLight
val SunYellowDark = SunGoldDark

val SolarGreen = EfficientGreen
val SolarGreenLight = EfficientGreenLight
val SolarGreenDark = Color(0xFF009624)

// Mapped Festival colors to new palette to maintain build health
val FestivalRed = WarningRed
val FestivalRedLight = WarningRedLight
val FestivalRedDark = Color(0xFF9B0000)

val FestivalOrange = EnergyOrange
val FestivalOrangeLight = EnergyOrangeLight
val FestivalOrangeDark = EnergyOrangeDark

val FestivalBrown = Color(0xFF3E2723) // Keep brown for specific text contrast if needed
val FestivalSurface = LightSurface
val FestivalBeige = LightBackground