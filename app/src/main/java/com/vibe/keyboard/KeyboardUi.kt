package com.vibe.keyboard

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VibeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme: ColorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun KeyboardUi(state: KeyboardState, actions: KeyboardActions) {
    VibeTheme {
        val cs = MaterialTheme.colorScheme
        val symbols = state.layer == Layer.SYMBOLS
        val rows = if (symbols) SYMBOL_ROWS else state.lang.rows
        val maxLen = rows[0].length

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cs.surfaceContainer)
                .padding(horizontal = 6.dp)
                .padding(top = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rows.forEachIndexed { i, row ->
                val side = (maxLen - row.length) / 2f
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (i == 2) {
                        // Shift (или пустышка на символах)
                        if (!symbols) {
                            val label = when (state.shift) {
                                Shift.OFF -> "⇧"
                                Shift.ONCE -> "⬆"
                                Shift.LOCK -> "⇪"
                            }
                            val active = state.shift != Shift.OFF
                            Key(
                                label = label, weight = side,
                                container = if (active) cs.primary else cs.secondaryContainer,
                                content = if (active) cs.onPrimary else cs.onSecondaryContainer,
                            ) { actions.toggleShift() }
                        } else {
                            Spacer(Modifier.weight(side))
                        }
                    } else if (side > 0f) {
                        Spacer(Modifier.weight(side))
                    }

                    row.forEach { ch ->
                        val shown = if (!symbols && state.shift != Shift.OFF)
                            ch.uppercaseChar().toString() else ch.toString()
                        Key(
                            label = shown, weight = 1f,
                            container = cs.surfaceContainerHighest,
                            content = cs.onSurface,
                        ) { actions.type(shown) }
                    }

                    if (i == 2) {
                        Key(
                            label = "⌫", weight = side,
                            container = cs.secondaryContainer,
                            content = cs.onSecondaryContainer,
                            repeat = true,
                        ) { actions.backspace() }
                    } else if (side > 0f) {
                        Spacer(Modifier.weight(side))
                    }
                }
            }

            // Нижний ряд
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Key(
                    label = if (symbols) "ABC" else "?123", weight = 1.5f,
                    container = cs.secondaryContainer, content = cs.onSecondaryContainer,
                    fontSize = 16,
                ) { actions.toggleLayer() }
                Key(
                    label = ",", weight = 1f,
                    container = cs.surfaceContainerHighest, content = cs.onSurface,
                ) { actions.type(",") }
                Key(
                    label = state.lang.next().label, weight = 1.2f,
                    container = cs.tertiaryContainer, content = cs.onTertiaryContainer,
                    fontSize = 16,
                ) { actions.switchLang() }
                Key(
                    label = state.lang.label, weight = 4f,
                    container = cs.surfaceContainerHighest, content = cs.onSurfaceVariant,
                    fontSize = 14,
                ) { actions.type(" ") }
                Key(
                    label = ".", weight = 1f,
                    container = cs.surfaceContainerHighest, content = cs.onSurface,
                ) { actions.type(".") }
                Key(
                    label = "↵", weight = 1.5f,
                    container = cs.primary, content = cs.onPrimary,
                ) { actions.enter() }
            }
        }
    }
}

@Composable
fun RowScope.Key(
    label: String,
    weight: Float,
    container: Color,
    content: Color,
    fontSize: Int = 22,
    repeat: Boolean = false,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val click by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    // Material You: при нажатии клавиша «схлопывается» из пилюли в более квадратную форму
    val radius: Dp by animateDpAsState(
        targetValue = if (pressed) 10.dp else 26.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "keyRadius",
    )

    Box(
        modifier = Modifier
            .weight(weight)
            .height(54.dp)
            .clip(RoundedCornerShape(radius))
            .background(container)
            .pointerInput(repeat) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        click()
                        coroutineScope {
                            val job = if (repeat) launch {
                                delay(400)
                                while (true) { click(); delay(45) }
                            } else null
                            tryAwaitRelease()
                            job?.cancel()
                        }
                        pressed = false
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = content, fontSize = fontSize.sp)
    }
}
