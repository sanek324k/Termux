package com.vibe.keyboard

import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- цвета и тема

fun hslColor(h: Float, s: Float, l: Float): Color {
    val hh = ((h % 360f) + 360f) % 360f
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((hh / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        hh < 60f -> Triple(c, x, 0f)
        hh < 120f -> Triple(x, c, 0f)
        hh < 180f -> Triple(0f, c, x)
        hh < 240f -> Triple(0f, x, c)
        hh < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

@Composable
fun isDarkTheme(): Boolean = when (Settings.themeMode) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

private fun whiteScheme(dark: Boolean): ColorScheme {
    fun g(l: Float) = hslColor(0f, 0f, l)
    return if (dark) darkColorScheme(
        primary = g(.95f), onPrimary = g(.1f),
        primaryContainer = g(.35f), onPrimaryContainer = g(.95f),
        secondaryContainer = g(.27f), onSecondaryContainer = g(.92f),
        tertiaryContainer = g(.31f), onTertiaryContainer = g(.92f),
        background = g(.07f), surface = g(.07f),
        onSurface = g(.93f), onSurfaceVariant = g(.75f),
        surfaceContainer = g(.10f), surfaceContainerHigh = g(.15f), surfaceContainerHighest = g(.21f),
    ) else lightColorScheme(
        primary = g(.15f), onPrimary = Color.White,
        primaryContainer = g(.85f), onPrimaryContainer = g(.1f),
        secondaryContainer = g(.89f), onSecondaryContainer = g(.12f),
        tertiaryContainer = g(.93f), onTertiaryContainer = g(.12f),
        background = Color.White, surface = Color.White,
        onSurface = g(.08f), onSurfaceVariant = g(.35f),
        surfaceContainer = Color.White, surfaceContainerHigh = g(.96f), surfaceContainerHighest = g(.93f),
    )
}

fun customScheme(hue: Float, dark: Boolean): ColorScheme {
    if (hue < 0f) return whiteScheme(dark)
    fun c(dh: Float, s: Float, l: Float) = hslColor(hue + dh, s, l)
    return if (dark) darkColorScheme(
        primary = c(0f, .85f, .78f), onPrimary = c(0f, .9f, .15f),
        primaryContainer = c(0f, .5f, .30f), onPrimaryContainer = c(0f, .9f, .9f),
        secondaryContainer = c(15f, .22f, .30f), onSecondaryContainer = c(15f, .3f, .9f),
        tertiaryContainer = c(60f, .35f, .30f), onTertiaryContainer = c(60f, .5f, .9f),
        background = c(0f, .15f, .08f), surface = c(0f, .15f, .08f),
        onSurface = c(0f, .1f, .92f), onSurfaceVariant = c(0f, .1f, .75f),
        surfaceContainer = c(0f, .15f, .13f), surfaceContainerHigh = c(0f, .15f, .18f),
        surfaceContainerHighest = c(0f, .15f, .23f),
    ) else lightColorScheme(
        primary = c(0f, .7f, .40f), onPrimary = Color.White,
        primaryContainer = c(0f, .85f, .88f), onPrimaryContainer = c(0f, .9f, .15f),
        secondaryContainer = c(15f, .35f, .86f), onSecondaryContainer = c(15f, .5f, .15f),
        tertiaryContainer = c(60f, .5f, .86f), onTertiaryContainer = c(60f, .6f, .15f),
        background = c(0f, .3f, .98f), surface = c(0f, .3f, .98f),
        onSurface = c(0f, .1f, .10f), onSurfaceVariant = c(0f, .1f, .35f),
        surfaceContainer = c(0f, .2f, .93f), surfaceContainerHigh = c(0f, .2f, .90f),
        surfaceContainerHighest = c(0f, .2f, .87f),
    )
}

/** Стиль One UI: спокойные серые поверхности, белые/тёмно-серые клавиши, синий акцент. */
private fun oneUiScheme(dark: Boolean, accent: Color): ColorScheme {
    val soft = lerp(accent, if (dark) Color.Black else Color.White, 0.78f)
    return if (dark) darkColorScheme(
        primary = accent, onPrimary = Color.White,
        primaryContainer = soft, onPrimaryContainer = Color.White,
        secondaryContainer = Color(0xFF454548), onSecondaryContainer = Color(0xFFEDEDED),
        tertiaryContainer = Color(0xFF454548), onTertiaryContainer = Color(0xFFEDEDED),
        background = Color(0xFF181818), surface = Color(0xFF181818),
        onSurface = Color(0xFFEDEDED), onSurfaceVariant = Color(0xFFA9A9AE),
        surfaceContainer = Color(0xFF1E1E20), surfaceContainerHigh = Color(0xFF2A2A2C),
        surfaceContainerHighest = Color(0xFF343437), outlineVariant = Color(0xFF3A3A3C),
    ) else lightColorScheme(
        primary = accent, onPrimary = Color.White,
        primaryContainer = soft, onPrimaryContainer = lerp(accent, Color.Black, 0.35f),
        secondaryContainer = Color(0xFFCDD0D6), onSecondaryContainer = Color(0xFF1A1A1A),
        tertiaryContainer = Color(0xFFCDD0D6), onTertiaryContainer = Color(0xFF1A1A1A),
        background = Color(0xFFF2F3F5), surface = Color(0xFFF2F3F5),
        onSurface = Color(0xFF1A1A1A), onSurfaceVariant = Color(0xFF6B6B70),
        surfaceContainer = Color(0xFFE4E6EA), surfaceContainerHigh = Color(0xFFF8F8FA),
        surfaceContainerHighest = Color.White, outlineVariant = Color(0xFFD0D2D8),
    )
}

/** AMOLED: чистый чёрный фон, клавиши тёмные с лёгким оттенком акцента. */
private fun amoledScheme(s: ColorScheme): ColorScheme {
    fun d(c: Color, f: Float) = lerp(c, Color.Black, f)
    return s.copy(
        background = Color.Black, surface = Color.Black,
        surfaceContainer = Color.Black,
        surfaceContainerLow = Color.Black, surfaceContainerLowest = Color.Black,
        surfaceContainerHigh = d(s.surfaceContainerHigh, 0.65f),
        surfaceContainerHighest = d(s.surfaceContainerHighest, 0.55f),
        surfaceVariant = d(s.surfaceVariant, 0.6f),
        primaryContainer = d(s.primaryContainer, 0.4f),
        secondaryContainer = d(s.secondaryContainer, 0.45f),
        tertiaryContainer = d(s.tertiaryContainer, 0.45f),
    )
}

@Composable
fun VibeTheme(content: @Composable () -> Unit) {
    val dark = isDarkTheme()
    val ctx = LocalContext.current
    val useDynamic = Settings.colorMode == ColorMode.DYNAMIC &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val base: ColorScheme = if (useDynamic) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        customScheme(Settings.hue, dark)
    }
    val styled = if (Settings.oneUi) {
        oneUiScheme(dark, if (Settings.colorMode == ColorMode.CUSTOM) base.primary else Color(0xFF3E91FF))
    } else {
        base
    }
    val scheme = if (dark && Settings.amoled) amoledScheme(styled) else styled
    MaterialTheme(colorScheme = scheme, content = content)
}

/** В режиме стекла панели становятся полупрозрачными. */
fun glassify(c: Color): Color = if (Settings.glass) c.copy(alpha = 0.45f) else c

// ---------------------------------------------------------------- варианты символов при долгом нажатии

private val EN_ALTS = mapOf(
    'q' to "1", 'w' to "2", 'e' to "3èéêëē", 'r' to "4", 't' to "5",
    'y' to "6ýÿ", 'u' to "7ùúûüū", 'i' to "8ìíîïī", 'o' to "9òóôöõøō", 'p' to "0",
    'a' to "àáâäãåā", 's' to "ßśš", 'c' to "çćč", 'n' to "ñń", 'z' to "žźż", 'l' to "ł",
)

private val RU_ALTS = mapOf(
    'й' to "1", 'ц' to "2", 'у' to "3", 'к' to "4", 'е' to "ё5",
    'н' to "6", 'г' to "7", 'ш' to "8", 'щ' to "9", 'з' to "0", 'ь' to "ъ",
)

fun altsFor(lang: Lang, ch: Char): List<String> {
    val s = (if (lang == Lang.RU) RU_ALTS else EN_ALTS)[ch] ?: return emptyList()
    return s.map { it.toString() }
}

// ---------------------------------------------------------------- состояние касаний

data class PopupUi(
    val left: Float,
    val top: Float,
    val itemW: Float,
    val itemH: Float,
    val items: List<String>,
    val sel: Int,
)

class TouchUi {
    val bounds = HashMap<String, Rect>()
    var origin by mutableStateOf(Offset.Zero)
    var width by mutableStateOf(0f)
    var pressed by mutableStateOf(setOf<String>())
    var popup by mutableStateOf<PopupUi?>(null)
    var dbg by mutableStateOf("")
    val trail = mutableStateListOf<Offset>()
}

class LetterCtl(
    val touch: TouchUi,
    val state: KeyboardState,
    val actions: () -> KeyboardActions,
    val buzz: () -> Unit,
)

private const val LONG_PRESS_MS = 350L

/** Касания одной буквенной клавиши: тап, долгое нажатие (попап) и свайп-ввод. */
private suspend fun PointerInputScope.keyGestures(
    id: String,
    holder: Array<LayoutCoordinates?>,
    ctl: LetterCtl,
) {
    val touch = ctl.touch
    val state = ctl.state
    val slop = 6.dp.toPx()
    val minItemW = 40.dp.toPx()
    val maxItemH = 52.dp.toPx()
    val popGap = 4.dp.toPx()

    fun toRoot(p: Offset): Offset {
        val lc = holder[0]
        return if (lc != null && lc.isAttached) lc.localToRoot(p) else p
    }

    fun shown(): String = if (state.shift != Shift.OFF) id.uppercase() else id

    fun buildPopup(rect: Rect, items: List<String>): PopupUi {
        val itemW = max(minItemW, rect.width)
        val itemH = min(maxItemH, rect.height)
        val n = items.size
        val total = itemW * n
        val cx = rect.center.x
        val rightHalf = cx > touch.origin.x + touch.width / 2f
        val minLeft = touch.origin.x
        val maxLeft = max(minLeft, touch.origin.x + touch.width - total)
        val rawLeft = if (!rightHalf) cx - itemW / 2f else cx + itemW / 2f - total
        val left = rawLeft.coerceIn(minLeft, maxLeft)
        val top = max(touch.origin.y, rect.top - itemH - popGap)
        val display = if (!rightHalf) items else items.reversed()
        val sel = if (!rightHalf) 0 else n - 1
        return PopupUi(left, top, itemW, itemH, display, sel)
    }

    fun finishSwipe(path: List<Offset>) {
        val centers = HashMap<Char, Offset>()
        var w = 0f
        for ((k, r) in touch.bounds) {
            centers[k[0]] = r.center
            w = r.width
        }
        val words = SwipeDecoder.decode(path, centers, w, Dictionary.swipeWords(state.lang))
        touch.dbg = "swipe: ${words.size} words ${words.firstOrNull() ?: ""}"
        if (words.isNotEmpty()) {
            ctl.actions().swipeWords(words)
        } else {
            var len = 0f
            for (i in 1 until path.size) len += (path[i] - path[i - 1]).getDistance()
            if (len < w * 1.5f) ctl.actions().type(shown())
        }
    }

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        var mode = 0 // 0 = тап, 1 = попап, 2 = свайп
        var longFired = false
        val path = ArrayList<Offset>()
        path.add(toRoot(down.position))
        touch.pressed = touch.pressed + id
        ctl.buzz()
        touch.dbg = "down $id bounds=${touch.bounds.size}"
        val startT = SystemClock.uptimeMillis()
        try {
            while (true) {
                val maybe: PointerEvent? = if (mode == 0 && !longFired) {
                    val remaining = LONG_PRESS_MS - (SystemClock.uptimeMillis() - startT)
                    if (remaining <= 0L) null else withTimeoutOrNull(remaining) { awaitPointerEvent() }
                } else {
                    awaitPointerEvent()
                }

                if (maybe == null) {
                    // прошло время долгого нажатия
                    longFired = true
                    val alts = altsFor(state.lang, id[0])
                        .map { if (state.shift != Shift.OFF) it.uppercase() else it }
                    val rect = touch.bounds[id]
                    if (alts.isNotEmpty() && rect != null) {
                        mode = 1
                        touch.popup = buildPopup(rect, alts)
                        ctl.buzz()
                        touch.dbg = "popup $id"
                    } else {
                        touch.dbg = "long $id, no alts"
                    }
                    continue
                }

                val c = maybe.changes.firstOrNull { it.id == down.id } ?: break
                val root = toRoot(c.position)
                c.consume()

                if (!c.pressed) {
                    when (mode) {
                        0 -> ctl.actions().type(shown())
                        1 -> {
                            val p = touch.popup
                            if (p != null) ctl.actions().type(p.items[p.sel])
                        }
                        else -> finishSwipe(path)
                    }
                    break
                }

                path.add(root)
                when (mode) {
                    0 -> {
                        val sz = size
                        val out = c.position.x < -slop || c.position.x > sz.width + slop ||
                            c.position.y < -slop || c.position.y > sz.height + slop
                        if (out && Settings.swipeTyping) {
                            mode = 2
                            touch.pressed = touch.pressed - id
                            touch.trail.clear()
                            touch.trail.addAll(path)
                            touch.dbg = "swipe from $id"
                        }
                    }
                    1 -> {
                        val p = touch.popup
                        if (p != null) {
                            val idx = ((root.x - p.left) / p.itemW).toInt().coerceIn(0, p.items.size - 1)
                            if (idx != p.sel) touch.popup = p.copy(sel = idx)
                        }
                    }
                    else -> {
                        touch.trail.add(root)
                        if (touch.trail.size > 300) touch.trail.removeAt(0)
                    }
                }
            }
        } finally {
            touch.pressed = touch.pressed - id
            if (mode == 1) touch.popup = null
            if (mode == 2) touch.trail.clear()
        }
    }
}

// ---------------------------------------------------------------- клавиатура

@Composable
fun KeyboardUi(state: KeyboardState, actions: KeyboardActions, applyNavInsets: Boolean = true) {
    VibeTheme {
        val cs = MaterialTheme.colorScheme
        val gap = Settings.gap.dp
        val hGap = Settings.hGap.dp
        val symbols = state.layer == Layer.SYMBOLS
        val rows = if (symbols) (if (state.symbolPage == 1) SYMBOL_ROWS_2 else SYMBOL_ROWS) else state.lang.rows
        val maxLen = rows[0].length
        val keyShape: Shape? = if (Settings.shapedKeys) AccentShapeImpl(Settings.accentShape) else null

        val touch = remember { TouchUi() }
        val view = LocalView.current
        val liveActions by rememberUpdatedState(actions)
        val buzz: () -> Unit = {
            if (Settings.haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        val ctl = remember { LetterCtl(touch, state, { liveActions }, buzz) }
        val bgImg = Settings.backgroundImage()
        val decorated = Settings.glass || bgImg != null

        Box(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    touch.origin = it.positionInRoot()
                    touch.width = it.size.width.toFloat()
                },
        ) {
            // фон: картинка / стекло
            if (bgImg != null) {
                Image(
                    bitmap = bgImg, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .then(if (Settings.bgBlur > 0f) Modifier.blur(Settings.bgBlur.dp) else Modifier),
                )
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = Settings.bgDim)))
            } else if (Settings.glass) {
                if (Settings.windowBlur) {
                    Box(Modifier.matchParentSize().background(cs.surfaceContainer.copy(alpha = 0.35f)))
                } else {
                    Box(
                        Modifier.matchParentSize().background(
                            Brush.linearGradient(listOf(cs.primaryContainer, cs.tertiaryContainer)),
                        ),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (decorated) Modifier else Modifier.background(cs.surfaceContainer))
                    .padding(horizontal = Settings.sidePad.dp)
                    .then(if (applyNavInsets) Modifier.navigationBarsPadding() else Modifier)
                    .padding(top = 8.dp, bottom = Settings.bottomPad.dp),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                if (state.layer == Layer.LETTERS || state.layer == Layer.SYMBOLS) SuggestionBar(state, actions)
                if (state.layer == Layer.EMOJI) {
                    EmojiPanel(actions)
                } else if (state.layer == Layer.CLIPBOARD) {
                    ToolsPanel(actions)
                } else {
                    rows.forEachIndexed { i, row ->
                        val side = (maxLen - row.length) / 2f
                        Row(horizontalArrangement = Arrangement.spacedBy(hGap)) {
                            if (i == 2) {
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
                                    Key(
                                        label = if (state.symbolPage == 0) "=\\<" else "?123", weight = side,
                                        container = cs.secondaryContainer, content = cs.onSecondaryContainer,
                                        fontSize = 16f,
                                    ) { state.symbolPage = 1 - state.symbolPage }
                                }
                            } else if (side > 0f) {
                                Spacer(Modifier.weight(side))
                            }

                            row.forEach { ch ->
                                val shown = if (!symbols && state.shift != Shift.OFF)
                                    ch.uppercaseChar().toString() else ch.toString()
                                if (symbols) {
                                    Key(
                                        label = shown, weight = 1f,
                                        container = cs.surfaceContainerHighest, content = cs.onSurface,
                                    ) { actions.type(shown) }
                                } else {
                                    LetterKey(
                                        ch = ch, shown = shown, weight = 1f,
                                        container = cs.surfaceContainerHighest, content = cs.onSurface,
                                        ctl = ctl,
                                    )
                                }
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

                    Row(horizontalArrangement = Arrangement.spacedBy(hGap)) {
                        Key(
                            label = if (symbols) "ABC" else "?123", weight = 1.4f,
                            container = cs.secondaryContainer, content = cs.onSecondaryContainer,
                            fontSize = 16f,
                        ) { state.symbolPage = 0; actions.toggleLayer() }
                        Key(
                            label = if (symbols && state.symbolPage == 1) "<" else ",", weight = 1f,
                            container = cs.surfaceContainerHighest, content = cs.onSurface,
                        ) { actions.type(if (symbols && state.symbolPage == 1) "<" else ",") }
                        Key(
                            label = "😊", weight = 1.1f,
                            container = cs.secondaryContainer, content = cs.onSecondaryContainer,
                            fontSize = 20f, accent = keyShape,
                        ) { actions.toggleEmoji() }
                        Key(
                            label = state.lang.next().label, weight = 1.1f,
                            container = cs.tertiaryContainer, content = cs.onTertiaryContainer,
                            fontSize = 16f, accent = keyShape,
                        ) { actions.switchLang() }
                        Key(
                            label = state.lang.label, weight = 3.2f,
                            container = cs.surfaceContainerHighest, content = cs.onSurfaceVariant,
                            fontSize = 14f,
                        ) { actions.type(" ") }
                        Key(
                            label = if (symbols && state.symbolPage == 1) ">" else ".", weight = 1f,
                            container = cs.surfaceContainerHighest, content = cs.onSurface,
                        ) { actions.type(if (symbols && state.symbolPage == 1) ">" else ".") }
                        Key(
                            label = "↵", weight = 1.4f,
                            container = cs.primary, content = cs.onPrimary,
                            accent = keyShape,
                        ) { actions.enter() }
                    }
                }
            }

            val popup = touch.popup
            if (popup != null) PopupStrip(popup, touch.origin)

            val trailColor = cs.primary
            Canvas(Modifier.matchParentSize()) {
                val pts = touch.trail
                if (pts.size > 1) {
                    val path = Path()
                    path.moveTo(pts[0].x - touch.origin.x, pts[0].y - touch.origin.y)
                    for (i in 1 until pts.size) {
                        path.lineTo(pts[i].x - touch.origin.x, pts[i].y - touch.origin.y)
                    }
                    drawPath(
                        path = path,
                        color = trailColor,
                        alpha = 0.7f,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }

            if (Settings.debug) {
                Text(
                    touch.dbg,
                    color = Color.Yellow,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.TopStart).background(Color.Black.copy(alpha = 0.6f)),
                )
            }
        }
    }
}

@Composable
private fun PopupStrip(p: PopupUi, origin: Offset) {
    val cs = MaterialTheme.colorScheme
    val d = LocalDensity.current
    Row(
        Modifier
            .offset { IntOffset((p.left - origin.x).roundToInt(), (p.top - origin.y).roundToInt()) }
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceContainerHighest),
    ) {
        p.items.forEachIndexed { i, s ->
            Box(
                Modifier
                    .width(with(d) { p.itemW.toDp() })
                    .height(with(d) { p.itemH.toDp() })
                    .background(if (i == p.sel) cs.primary else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    s,
                    fontSize = 22.sp,
                    color = if (i == p.sel) cs.onPrimary else cs.onSurface,
                    fontFamily = Settings.currentFont(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- клавиши

@Composable
fun KeyBox(
    modifier: Modifier,
    label: String,
    container: Color,
    content: Color,
    pressed: Boolean,
    fontSize: Float? = null,
    onPositioned: ((LayoutCoordinates) -> Unit)? = null,
    height: Float? = null,
    accent: Shape? = null,
) {
    val effect = Settings.effect
    val morph = effect == PressEffect.MORPH || effect == PressEffect.COMBO
    val glow = effect == PressEffect.GLOW || effect == PressEffect.COMBO
    val scaling = effect == PressEffect.SQUISH || effect == PressEffect.BOUNCE ||
        effect == PressEffect.COMBO
    val glass = Settings.glass
    val dark = isDarkTheme()

    val base = Settings.cornerRadius
    val radius by animateDpAsState(
        targetValue = if (pressed && morph) (base * 0.3f).dp else base.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "radius",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed && scaling) when (effect) {
            PressEffect.SQUISH -> 0.88f
            PressEffect.BOUNCE -> 0.80f
            else -> 0.94f
        } else 1f,
        animationSpec = if (effect == PressEffect.BOUNCE)
            spring(dampingRatio = 0.28f, stiffness = 450f)
        else spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "scale",
    )
    val bg by animateColorAsState(
        targetValue = if (pressed && glow)
            lerp(container, MaterialTheme.colorScheme.primary, 0.55f) else container,
        animationSpec = tween(90),
        label = "bg",
    )
    val boost by animateFloatAsState(
        targetValue = if (pressed) 0.22f else 0f,
        animationSpec = tween(80),
        label = "boost",
    )
    val shape: Shape = accent ?: RoundedCornerShape(radius)

    val look = if (glass) {
        val a = Settings.glassAlpha
        val tint = lerp(Color.White, container, 0.35f)
        val topA = ((if (dark) a * 0.8f else a + 0.15f) + boost).coerceIn(0f, 1f)
        val botA = ((if (dark) a * 0.4f else a * 0.75f) + boost).coerceIn(0f, 1f)
        Modifier
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = topA), tint.copy(alpha = botA))))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.5f)),
                ),
                shape,
            )
    } else {
        Modifier.background(bg)
    }

    Box(
        modifier = modifier
            .height((height ?: Settings.keyHeight).dp)
            .then(if (onPositioned != null) Modifier.onGloballyPositioned(onPositioned) else Modifier)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .then(look),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label, color = content,
            fontSize = (fontSize ?: Settings.fontSize).sp,
            fontFamily = Settings.currentFont(),
        )
    }
}

/** Обычная клавиша со своим тапом (шифт, backspace, нижний ряд, символы). */
@Composable
fun RowScope.Key(
    label: String,
    weight: Float,
    container: Color,
    content: Color,
    fontSize: Float? = null,
    repeat: Boolean = false,
    height: Float? = null,
    accent: Shape? = null,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val click by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }

    KeyBox(
        modifier = Modifier
            .weight(weight)
            .pointerInput(repeat) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        if (Settings.haptics) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
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
        label = label, container = container, content = content,
        pressed = pressed, fontSize = fontSize, height = height, accent = accent,
    )
}

/** Буквенная клавиша: тап, долгое нажатие с вариантами и свайп-ввод. */
@Composable
fun RowScope.LetterKey(
    ch: Char,
    shown: String,
    weight: Float,
    container: Color,
    content: Color,
    ctl: LetterCtl,
) {
    val id = ch.toString()
    val touch = ctl.touch
    val holder = remember { arrayOfNulls<LayoutCoordinates>(1) }
    DisposableEffect(id) { onDispose { touch.bounds.remove(id) } }
    KeyBox(
        modifier = Modifier
            .weight(weight)
            .pointerInput(id) { keyGestures(id, holder, ctl) },
        label = shown, container = container, content = content,
        pressed = touch.pressed.contains(id),
        onPositioned = {
            holder[0] = it
            touch.bounds[id] = it.boundsInRoot()
        },
    )
}
