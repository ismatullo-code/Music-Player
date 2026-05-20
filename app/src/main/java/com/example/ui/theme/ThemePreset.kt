package com.example.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

enum class NeonTheme {
    CYBERPUNK_RESET,
    GLASS_VISION_PRO,
    TOXIC_INDUSTRIAL,
    QUANTUM_ORANGE
}

data class ThemeColors(
    val primaryColor: Color,
    val secondaryColor: Color,
    val neonAccent: Color,
    val neonGlow: Color,
    val glassBg: Color,
    val glassBorder: Color,
    val screenBackground: Color
)

object ThemeState {
    var activeTheme by mutableStateOf(NeonTheme.CYBERPUNK_RESET)
    
    fun getColors(): ThemeColors {
        return when (activeTheme) {
            NeonTheme.CYBERPUNK_RESET -> ThemeColors(
                primaryColor = Color(0xFFD0BCFF),
                secondaryColor = Color(0xFFFF007F), // Razor Magenta
                neonAccent = Color(0xFF00FFCC),    // Neon Cyan
                neonGlow = Color(0xFF7B00FF),      // Cyber Purple
                glassBg = Color(0x280E0426),       // Translucent deep spaces
                glassBorder = Color(0x3BFFFFFF),   // Light-scattering reflection edge
                screenBackground = Color(0xFF06010F) // Total Eclipse violet background
            )
            NeonTheme.GLASS_VISION_PRO -> ThemeColors(
                primaryColor = Color(0xFFF1F5F9),
                secondaryColor = Color(0xFF38BDF8), // Electric Cyan-blue
                neonAccent = Color(0xFFA855F7),    // Radiant Amorphous Purple
                neonGlow = Color(0xFFEC4899),      // Electric Hot Pink
                glassBg = Color(0x221E293B),       // Misty Space Slate
                glassBorder = Color(0x2EFFFFFF),   // Pure crystal border
                screenBackground = Color(0xFF0F172A) // Dark Midnight Blue
            )
            NeonTheme.TOXIC_INDUSTRIAL -> ThemeColors(
                primaryColor = Color(0xFF39FF14),  // Toxic Cyber Green
                secondaryColor = Color(0xFF00E6FF), // Cyan Flare
                neonAccent = Color(0xFF39FF14),    // Green Flash
                neonGlow = Color(0xFF1F2937),      // Deep Carbon
                glassBg = Color(0x200B1A04),       // Acid toxic swamp translucent glass
                glassBorder = Color(0x3539FF14),   // Green specular border highlights
                screenBackground = Color(0xFF020C02) // Dark Nuclear Zone black
            )
            NeonTheme.QUANTUM_ORANGE -> ThemeColors(
                primaryColor = Color(0xFFFF6600),  // Pulse Orange
                secondaryColor = Color(0xFFFFCC00), // Solar Yellow
                neonAccent = Color(0xFFFF3300),    // Molten Laser Crimson
                neonGlow = Color(0xFF7C2D12),      // Deep Lava Glow
                glassBg = Color(0x261F0A03),       // Obsidian Amber glass
                glassBorder = Color(0x30FF6600),   // Orange specular highlights
                screenBackground = Color(0xFF050100) // Deep Geothermal black
            )
        }
    }
}
