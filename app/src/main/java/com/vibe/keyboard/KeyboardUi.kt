package com.vibe.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

fun Modifier.liquidGlass(enabled: Boolean, isDark: Boolean): Modifier = if (enabled) {
    this.background(
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.05f))
            } else {
                listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.25f))
            }
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)
        ),
        shape = RoundedCornerShape(16.dp)
    )
} else this

@Composable
fun VibeKeyboardContainer(
    theme: KeyboardTheme,
    liquidGlass: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val keyboardShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    
    val bgBrush = when (theme) {
        KeyboardTheme.MINECRAFT -> Brush.linearGradient(listOf(Color(0xFF8B5A2B), Color(0xFF5C4033)))
        KeyboardTheme.CYBERPUNK -> Brush.linearGradient(listOf(Color(0xFF0F051D), Color(0xFF1A0B2E)))
        KeyboardTheme.MATERIAL_3_EXPRESSIVE -> Brush.linearGradient(listOf(Color(0xFFE8DEF8), Color(0xFFD0BCFF)))
        else -> Brush.linearGradient(listOf(Color(0xFF1C1B1F), Color(0xFF2B2930)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(keyboardShape)
            .background(bgBrush)
            .liquidGlass(liquidGlass, theme != KeyboardTheme.MATERIAL_3_EXPRESSIVE)
            .shadow(16.dp, keyboardShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            content = content
        )
    }
}
