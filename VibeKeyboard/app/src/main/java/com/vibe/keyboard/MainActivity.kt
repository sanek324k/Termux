@file:OptIn(ExperimentalLayoutApi::class)

package com.vibe.keyboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.init(this)
        setContent {
            VibeTheme {
                SettingsScreen(
                    onEnable = { startActivity(Intent(AndroidSettings.ACTION_INPUT_METHOD_SETTINGS)) },
                    onPick = {
                        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .showInputMethodPicker()
                    },
                )
            }
        }
    }

    override fun onPause() {
        Settings.save()
        super.onPause()
    }
}

// ---------------------------------------------------------------- страницы

private enum class Page(val icon: String, val title: String, val subtitle: String) {
    HOME("", "Vibe Keyboard", ""),
    LOOK("🎨", "Внешний вид", "Стиль, тема, цвета, AMOLED"),
    BG("🖼️", "Фон и шрифт", "Картинка, размытие, шрифт"),
    KEYS("⌨️", "Клавиши", "Размеры, форма, эффекты, вибрация"),
    INPUT("✍️", "Ввод", "Подсказки, свайп, словарь"),
    TOOLS("grid", "Инструменты", "Буфер, переводчик, скриншоты"),
    OTHER("⚙️", "Прочее", "Сброс, отладка, тест"),
}

private class Pickers(
    val bg: () -> Unit,
    val font: () -> Unit,
    val dict: () -> Unit,
    val shot: (Boolean) -> Unit,
)

private class PreviewState {
    var typed by mutableStateOf("")
    val kb = KeyboardState()
    val actions: KeyboardActions = object : KeyboardActions {
        override fun type(text: String) {
            typed += text
            if (kb.shift == Shift.ONCE) kb.shift = Shift.OFF
        }
        override fun backspace() { typed = typed.dropLast(1) }
        override fun enter() { typed += "\n" }
        override fun toggleShift() {
            kb.shift = when (kb.shift) {
                Shift.OFF -> Shift.ONCE
                Shift.ONCE -> Shift.LOCK
                Shift.LOCK -> Shift.OFF
            }
        }
        override fun toggleLayer() {
            kb.layer = if (kb.layer == Layer.LETTERS) Layer.SYMBOLS else Layer.LETTERS
        }
        override fun switchLang() { kb.lang = kb.lang.next() }
        override fun pickSuggestion(word: String) {
            typed = typed.dropLast(kb.currentWord.length) + word + " "
        }
        override fun emoji(e: String) {
            typed += e
            EmojiStore.add(e)
        }
        override fun toggleEmoji() {
            kb.layer = if (kb.layer == Layer.EMOJI) Layer.LETTERS else Layer.EMOJI
        }
        override fun pasteClip(item: ClipItem) {
            if (item.type == "text") typed += item.text
            ClipStore.consume(item)
        }
        override fun toggleClipboard() {
            kb.layer = if (kb.layer == Layer.CLIPBOARD) Layer.LETTERS else Layer.CLIPBOARD
        }
        override fun swipeWords(words: List<String>) {
            if (words.isNotEmpty()) typed += words[0] + " "
        }
    }
}

@Composable
fun SettingsScreen(onEnable: () -> Unit, onPick: () -> Unit) {
    val ctx = LocalContext.current
    val preview = remember { PreviewState() }
    var page by remember { mutableStateOf(Page.HOME) }
    BackHandler(enabled = page != Page.HOME) { page = Page.HOME }

    val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = Settings.importBackground(ctx, uri)
            Toast.makeText(ctx, if (ok) "Фон установлен" else "Не удалось загрузить картинку", Toast.LENGTH_SHORT).show()
        }
    }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = Settings.importFont(ctx, uri)
            Toast.makeText(ctx, if (ok) "Шрифт установлен" else "Не удалось загрузить шрифт", Toast.LENGTH_SHORT).show()
        }
    }
    val dictPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val n = Dictionary.importWords(ctx, uri)
            Toast.makeText(ctx, if (n > 0) "Загружено слов: $n" else "Не удалось прочитать словарь", Toast.LENGTH_LONG).show()
        }
    }
    val shotPerm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
    else Manifest.permission.READ_EXTERNAL_STORAGE
    val shotLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Settings.screenshots = granted
        Settings.save()
        if (!granted) {
            Toast.makeText(ctx, "Включи «Фото и видео» → «Разрешить все» в настройках приложения", Toast.LENGTH_LONG).show()
            ctx.startActivity(
                Intent(
                    AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:" + ctx.packageName),
                ),
            )
        }
    }
    val pickers = Pickers(
        bg = { bgPicker.launch(arrayOf("image/*")) },
        font = { fontPicker.launch(arrayOf("*/*")) },
        dict = { dictPicker.launch(arrayOf("*/*")) },
        shot = { on ->
            if (!on) {
                Settings.screenshots = false
                Settings.save()
            } else if (ContextCompat.checkSelfPermission(ctx, shotPerm) == PackageManager.PERMISSION_GRANTED) {
                Settings.screenshots = true
                Settings.save()
            } else {
                shotLauncher.launch(shotPerm)
            }
        },
    )
    val back = { page = Page.HOME }

    when (page) {
        Page.HOME -> HomePage({ page = it }, onEnable, onPick)
        Page.LOOK -> LookPage(preview, back)
        Page.BG -> BgPage(preview, pickers, back)
        Page.KEYS -> KeysPage(preview, back)
        Page.INPUT -> InputPage(pickers, back)
        Page.TOOLS -> ToolsPage(pickers, back)
        Page.OTHER -> OtherPage(back)
    }
}

@Composable
private fun HomePage(onOpen: (Page) -> Unit, onEnable: () -> Unit, onPick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    PageScaffold(Page.HOME.title, null) {
        OneCard {
            FolderRow("⌨️", "Включить клавиатуру", "Откроет системные настройки", onEnable)
            HorizontalDivider(Modifier.padding(start = 74.dp), color = cs.outlineVariant)
            FolderRow("🔀", "Выбрать клавиатуру", "Переключиться на Vibe Keyboard", onPick)
        }
        OneCard {
            val folders = Page.entries.filter { it != Page.HOME }
            folders.forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider(Modifier.padding(start = 74.dp), color = cs.outlineVariant)
                FolderRow(p.icon, p.title, p.subtitle) { onOpen(p) }
            }
        }
    }
}

@Composable
private fun LookPage(preview: PreviewState, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val style = if (Settings.oneUi) 2 else if (Settings.glass) 1 else 0
    PageScaffold(Page.LOOK.title, onBack) {
        PreviewCard(preview)
        OneCard {
            Label("Стиль")
            ChipGroup(listOf(0 to "Material You", 1 to "Liquid Glass", 2 to "One UI"), style) {
                Settings.applyStyle(it)
            }
            if (Settings.glass) {
                SliderRow("Прозрачность стекла", Settings.glassAlpha * 100f, 10f..70f, " %") {
                    Settings.glassAlpha = it / 100f
                }
                SwitchRow("Размытие окна позади", "Android 12+, работает не на всех телефонах", Settings.windowBlur) {
                    Settings.windowBlur = it; Settings.save()
                }
            }
        }
        OneCard {
            Label("Цвет")
            ChipGroup(ColorMode.entries.map { it to it.label }, Settings.colorMode) {
                Settings.colorMode = it; Settings.save()
            }
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(-1f, 0f, 25f, 50f, 140f, 175f, 210f, 260f, 320f).forEach { h ->
                    val selected = Settings.colorMode == ColorMode.CUSTOM && Settings.hue == h
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(AccentShapeImpl(Settings.accentShape))
                            .background(if (h < 0f) Color.White else hslColor(h, .8f, .55f))
                            .border(
                                if (selected) 3.dp else 1.dp,
                                if (selected) cs.onSurface else cs.outlineVariant,
                                AccentShapeImpl(Settings.accentShape),
                            )
                            .clickable {
                                Settings.hue = h
                                if (h < 0f) Settings.themeMode = 1
                                Settings.colorMode = ColorMode.CUSTOM
                                Settings.save()
                            },
                    )
                }
            }
            if (Settings.colorMode == ColorMode.CUSTOM && Settings.hue >= 0f) {
                SliderRow("Оттенок", Settings.hue, 0f..360f, "°") { Settings.hue = it }
            }
            Label("Тема")
            ChipGroup(listOf(0 to "Авто", 1 to "Светлая", 2 to "Тёмная"), Settings.themeMode) {
                Settings.themeMode = it; Settings.save()
            }
            SwitchRow("AMOLED: глубокий чёрный", "Только в тёмной теме", Settings.amoled) {
                Settings.amoled = it; Settings.save()
            }
        }
    }
}

@Composable
private fun BgPage(preview: PreviewState, pickers: Pickers, onBack: () -> Unit) {
    PageScaffold(Page.BG.title, onBack) {
        PreviewCard(preview)
        OneCard {
            Label("Фон-картинка")
            Row(
                Modifier.padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = pickers.bg) { Text("Выбрать картинку") }
                TextButton(onClick = { Settings.clearBackground() }) { Text("Убрать фон") }
            }
            SliderRow("Затемнение фона", Settings.bgDim * 100f, 0f..80f, " %") { Settings.bgDim = it / 100f }
            SliderRow("Размытие фона (Android 12+)", Settings.bgBlur, 0f..24f, " dp") { Settings.bgBlur = it }
        }
        OneCard {
            Label("Шрифт")
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Settings.fontPresets.forEach { (key, label) ->
                    FilterChip(
                        selected = Settings.fontKey == key,
                        onClick = { Settings.fontKey = key; Settings.save() },
                        label = { Text(label, fontFamily = Settings.presetFamily(key)) },
                    )
                }
            }
            Box(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                FilledTonalButton(onClick = pickers.font, modifier = Modifier.fillMaxWidth()) {
                    Text("Выбрать файл шрифта (.ttf / .otf)")
                }
            }
        }
    }
}

@Composable
private fun KeysPage(preview: PreviewState, onBack: () -> Unit) {
    PageScaffold(Page.KEYS.title, onBack) {
        PreviewCard(preview)
        OneCard {
            Label("Размеры")
            SliderRow("Высота клавиш", Settings.keyHeight, 44f..84f, " dp") { Settings.keyHeight = it }
            SliderRow("Скругление", Settings.cornerRadius, 4f..32f, " dp") { Settings.cornerRadius = it }
            SliderRow("Зазор по вертикали", Settings.gap, 2f..12f, " dp") { Settings.gap = it }
            SliderRow("Зазор по горизонтали", Settings.hGap, 0f..12f, " dp") { Settings.hGap = it }
            SliderRow("Боковые отступы (меньше = шире клавиши)", Settings.sidePad, 0f..24f, " dp") { Settings.sidePad = it }
            SliderRow("Нижний отступ (если стрелка перекрывает клавиши)", Settings.bottomPad, 0f..60f, " dp") { Settings.bottomPad = it }
            SliderRow("Размер букв", Settings.fontSize, 16f..30f, " sp") { Settings.fontSize = it }
        }
        OneCard {
            Label("Форма кнопок")
            Box(Modifier.padding(horizontal = 12.dp)) { ShapePicker() }
            SwitchRow("Форма на клавишах", "Enter, язык, эмодзи", Settings.shapedKeys) {
                Settings.shapedKeys = it; Settings.save()
            }
        }
        OneCard {
            Label("Нажатие")
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PressEffect.entries.forEach { e ->
                    FilterChip(
                        selected = Settings.effect == e,
                        onClick = { Settings.effect = e; Settings.save() },
                        label = { Text(e.label) },
                    )
                }
            }
            SwitchRow("Вибрация", null, Settings.haptics) { Settings.haptics = it; Settings.save() }
        }
    }
}

@Composable
private fun InputPage(pickers: Pickers, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    PageScaffold(Page.INPUT.title, onBack) {
        OneCard {
            SwitchRow("Подсказки слов", "Предлагает слова и учится на твоём вводе", Settings.suggestions) {
                Settings.suggestions = it; Settings.save()
            }
            HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = cs.outlineVariant)
            SwitchRow("Ввод свайпом", "Проведи пальцем по буквам слова", Settings.swipeTyping) {
                Settings.swipeTyping = it; Settings.save()
            }
        }
        OneCard {
            FolderRow("📖", "Загрузить словарь", "Текстовый файл со словами для свайпа и подсказок", pickers.dict)
            HorizontalDivider(Modifier.padding(start = 74.dp), color = cs.outlineVariant)
            FolderRow("🧹", "Забыть выученные слова", "Удалит слова, которые клавиатура запомнила") {
                Dictionary.clearUser()
            }
        }
    }
}

@Composable
private fun ToolsPage(pickers: Pickers, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    PageScaffold(Page.TOOLS.title, onBack) {
        OneCard {
            SwitchRow("Буфер обмена", "История текста и картинок", Settings.clipboard) {
                Settings.clipboard = it; Settings.save()
            }
            HorizontalDivider(Modifier.padding(start = 74.dp), color = cs.outlineVariant)
            FolderRow("🗑️", "Очистить буфер", "Удалит всю сохранённую историю") { ClipStore.clear() }
            HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = cs.outlineVariant)
            SwitchRow("Скриншоты в буфер", "Нужен доступ к фото («Разрешить все»)", Settings.screenshots) {
                pickers.shot(it)
            }
        }
        OneCard {
            SwitchRow(
                "Переводчик",
                "Текст уходит на сервер перевода только по кнопке «Перевести»",
                Settings.translatorEnabled,
            ) { Settings.translatorEnabled = it; Settings.save() }
        }
    }
}

@Composable
private fun OtherPage(onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var text by remember { mutableStateOf("") }
    PageScaffold(Page.OTHER.title, onBack) {
        OneCard {
            SwitchRow("Отладка касаний", "Жёлтая строка над клавиатурой", Settings.debug) {
                Settings.debug = it; Settings.save()
            }
            HorizontalDivider(Modifier.padding(start = 74.dp), color = cs.outlineVariant)
            FolderRow("🔄", "Сбросить настройки", "Вернёт всё к значениям по умолчанию") { Settings.reset() }
        }
        OneCard {
            Label("Тест в системном поле")
            Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Тестовое поле") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- элементы в стиле One UI

@Composable
private fun PageScaffold(title: String, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(cs.surface)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceContainerHighest)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("‹", fontSize = 28.sp, color = cs.onSurface) }
        }
        Box(
            Modifier.fillMaxWidth().height(if (onBack != null) 56.dp else 150.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(title, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
        }
        content()
        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

@Composable
private fun OneCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(vertical = 6.dp),
        content = content,
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun FolderRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(cs.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (icon == "grid") GridIcon(cs.onPrimaryContainer, Modifier.size(22.dp))
            else Text(icon, fontSize = 22.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, fontSize = 17.sp, color = cs.onSurface)
            if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 13.sp, color = cs.onSurfaceVariant)
        }
        Text("›", fontSize = 26.sp, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 16.sp, color = cs.onSurface)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = cs.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onChange: (Float) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = cs.onSurface, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("${value.roundToInt()}$unit", color = cs.onSurfaceVariant, fontSize = 15.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            onValueChangeFinished = { Settings.save() },
        )
    }
}

@Composable
private fun <T> ChipGroup(items: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (v, label) ->
            FilterChip(selected = v == selected, onClick = { onSelect(v) }, label = { Text(label) })
        }
    }
}

@Composable
private fun PreviewCard(p: PreviewState) {
    val cs = MaterialTheme.colorScheme
    LaunchedEffect(p.typed, p.kb.lang, Settings.suggestions) {
        p.kb.currentWord = currentWord(p.typed)
        p.kb.suggestions = if (Settings.suggestions) Dictionary.suggest(p.kb.currentWord, p.kb.lang) else emptyList()
    }
    OneCard {
        Label("Превью (рабочее, жми клавиши)")
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(cs.surface)
                .padding(12.dp),
        ) {
            Text(
                p.typed.ifEmpty { "Печатай на превью…" },
                color = if (p.typed.isEmpty()) cs.onSurfaceVariant else cs.onSurface,
            )
        }
        Box(Modifier.padding(8.dp).clip(RoundedCornerShape(22.dp))) {
            KeyboardUi(p.kb, p.actions, applyNavInsets = false)
        }
    }
}
