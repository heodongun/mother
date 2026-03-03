package com.smartpet.todo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Toss-inspired color theme
 */
object TossColors {
    // Primary
    val Blue = Color(0xFF0064FF)
    val BlueLight = Color(0xFF4D94FF)
    val BlueDark = Color(0xFF0050CC)
    
    // Grayscale
    val Black = Color(0xFF191F28)
    val Gray900 = Color(0xFF333D4B)
    val Gray700 = Color(0xFF4E5968)
    val Gray500 = Color(0xFF8B95A1)
    val Gray300 = Color(0xFFB0B8C1)
    val Gray100 = Color(0xFFE5E8EB)
    val Gray50 = Color(0xFFF2F4F6)
    val White = Color(0xFFFFFFFF)
    
    // Status
    val Red = Color(0xFFF04452)
    val Orange = Color(0xFFFF8A00)
    val Green = Color(0xFF00C853)
    val Yellow = Color(0xFFFFD600)
    
    // Pet Mood Colors
    val PetHappy = Color(0xFF00C853)
    val PetWorried = Color(0xFFFFD600)
    val PetChasing = Color(0xFFFF8A00)
    val PetAngry = Color(0xFFF04452)
}

/**
 * Common dimensions
 */
object Dimensions {
    const val CardCornerRadius = 16
    const val ButtonCornerRadius = 12
    const val ItemCornerRadius = 12
    const val PaddingSmall = 8
    const val PaddingMedium = 16
    const val PaddingLarge = 24
    const val MinTouchTarget = 48
    const val CardElevation = 2
}

private val SmartPetLightColorScheme = lightColorScheme(
    primary = TossColors.Blue,
    onPrimary = TossColors.White,
    primaryContainer = TossColors.BlueLight,
    onPrimaryContainer = TossColors.White,
    secondary = TossColors.Gray700,
    onSecondary = TossColors.White,
    background = TossColors.Gray50,
    onBackground = TossColors.Black,
    surface = TossColors.White,
    onSurface = TossColors.Black,
    surfaceVariant = TossColors.Gray50,
    onSurfaceVariant = TossColors.Gray700,
    outline = TossColors.Gray100,
    error = TossColors.Red,
    onError = TossColors.White
)

private val SmartPetTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
)

@Composable
fun SmartPetTodoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SmartPetLightColorScheme,
        typography = SmartPetTypography,
        content = content
    )
}
