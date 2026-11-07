package com.vibe.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

enum class AccentShape(val label: String) {
    COOKIE("Печенька"),
    SUN("Солнце"),
    CIRCLE("Круг"),
    SQUARE("Квадрат"),
    ROUNDED("Скругл. квадрат"),
}

/** Форма, вписанная в центральный квадрат области (для прямоугольных клавиш берётся меньшая сторона). */
class AccentShapeImpl(private val kind: AccentShape) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val s = minOf(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val box = Rect(cx - s / 2f, cy - s / 2f, cx + s / 2f, cy + s / 2f)
        val path = Path()
        when (kind) {
            AccentShape.CIRCLE -> path.addOval(box)
            AccentShape.SQUARE -> path.addRoundRect(RoundRect(box, CornerRadius(s * 0.06f)))
            AccentShape.ROUNDED -> path.addRoundRect(RoundRect(box, CornerRadius(s * 0.28f)))
            AccentShape.COOKIE -> lobes(path, cx, cy, s / 2f, 9, 0.10f, 0.5f)
            AccentShape.SUN -> lobes(path, cx, cy, s / 2f, 6, 0.12f, 0.5f)
        }
        return Outline.Generic(path)
    }

    /** Фигура с n округлыми выступами и острыми «ямками» между ними: печенька (9) и солнце (6, ямок меньше). */
    private fun lobes(path: Path, cx: Float, cy: Float, rMax: Float, n: Int, depth: Float, k: Float) {
        val rMin = rMax * (1f - depth)
        val steps = 360
        val twoPi = (2.0 * PI).toFloat()
        val halfPi = (PI / 2.0).toFloat()
        for (i in 0..steps) {
            val t = i.toFloat() / steps * twoPi
            val g = ((1f + cos(n * (t + halfPi))) / 2f).coerceAtLeast(0f).pow(k)
            val r = rMin + (rMax - rMin) * g
            val x = cx + r * cos(t)
            val y = cy + r * sin(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }
}

/** Значок: четыре скруглённых квадрата, между ними крест из зазоров. */
@Composable
fun GridIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val gap = s * 0.16f
        val c = (s - gap) / 2f
        val r = CornerRadius(c * 0.3f)
        for (i in 0..1) {
            for (j in 0..1) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(i * (c + gap), j * (c + gap)),
                    size = Size(c, c),
                    cornerRadius = r,
                )
            }
        }
    }
}

/** Кнопка открытия панели инструментов (буфер, перевод, калькулятор). */
@Composable
fun ToolsButton(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .size(40.dp)
            .clip(AccentShapeImpl(Settings.accentShape))
            .background(cs.primaryContainer)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        GridIcon(cs.onPrimaryContainer, Modifier.size(18.dp))
    }
}

/** Выбор формы в настройках. */
@Composable
fun ShapePicker() {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AccentShape.entries.forEach { k ->
            val sel = Settings.accentShape == k
            Column(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { Settings.accentShape = k; Settings.save() }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(AccentShapeImpl(k))
                        .background(if (sel) cs.primaryContainer else cs.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    GridIcon(
                        if (sel) cs.onPrimaryContainer else cs.onSurfaceVariant,
                        Modifier.size(24.dp),
                    )
                }
                Text(
                    k.label,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = if (sel) cs.primary else cs.onSurfaceVariant,
                    modifier = Modifier.width(64.dp).padding(top = 4.dp),
                )
            }
        }
    }
}
