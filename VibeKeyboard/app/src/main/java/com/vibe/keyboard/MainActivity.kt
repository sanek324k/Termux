package com.vibe.keyboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.Intent
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
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

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = MaterialTheme.colorScheme.onSurface)
            Text("${value.roundToInt()}$unit", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            onValueChangeFinished = { Settings.save() },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(onEnable: () -> Unit, onPick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = Settings.importFont(ctx, uri)
            Toast.makeText(ctx, if (ok) "Шрифт установлен" else "Не удалось загрузить шрифт", Toast.LENGTH_SHORT).show()
        }
    }
    val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = Settings.importBackground(ctx, uri)
            Toast.makeText(ctx, if (ok) "Фон установлен" else "Не удалось загрузить картинку", Toast.LENGTH_SHORT).show()
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
            ctx.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + ctx.packageName)))
        }
    }
    var typed by remember { mutableStateOf("") }
    val kb = remember { KeyboardState() }
    val actions = remember {
        object : KeyboardActions {
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
            override fun toggleEmoji() {
                kb.layer = if (kb.layer == Layer.EMOJI) Layer.LETTERS else Layer.EMOJI
            }
        }
    }

    LaunchedEffect(typed, kb.lang, Settings.suggestions) {
        kb.currentWord = currentWord(typed)
        kb.suggestions = if (Settings.suggestions) Dictionary.suggest(kb.currentWord, kb.lang) else emptyList()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(cs.surface)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Vibe Keyboard", style = MaterialTheme.typography.headlineLarge, color = cs.onSurface)

        Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) { Text("1. Включить в настройках") }
        FilledTonalButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) { Text("2. Выбрать клавиатуру") }

        SectionTitle("Превью (рабочее, жми клавиши)")
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cs.surfaceContainerHigh)
                .padding(14.dp),
        ) {
            Text(
                typed.ifEmpty { "Печатай на превью…" },
                color = if (typed.isEmpty()) cs.onSurfaceVariant else cs.onSurface,
            )
        }
        Box(Modifier.clip(RoundedCornerShape(24.dp))) { KeyboardUi(kb, actions, applyNavInsets = false) }

        SectionTitle("Размеры")
        SliderRow("Высота клавиш", Settings.keyHeight, 44f..84f, " dp") { Settings.keyHeight = it }
        SliderRow("Скругление", Settings.cornerRadius, 4f..32f, " dp") { Settings.cornerRadius = it }
        SliderRow("Зазор по вертикали", Settings.gap, 2f..12f, " dp") { Settings.gap = it }
        SliderRow("Зазор по горизонтали", Settings.hGap, 0f..12f, " dp") { Settings.hGap = it }
        SliderRow("Боковые отступы (меньше = шире клавиши)", Settings.sidePad, 0f..24f, " dp") { Settings.sidePad = it }
        SliderRow("Нижний отступ (если стрелка перекрывает клавиши)", Settings.bottomPad, 0f..60f, " dp") { Settings.bottomPad = it }
        SliderRow("Размер букв", Settings.fontSize, 16f..30f, " sp") { Settings.fontSize = it }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ввод свайпом", color = cs.onSurface)
            Switch(checked = Settings.swipeTyping, onCheckedChange = { Settings.swipeTyping = it; Settings.save() })
        }

        SectionTitle("Форма кнопок")
        ShapePicker()
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Форма на клавишах (Enter, язык, эмодзи)", color = cs.onSurface, modifier = Modifier.weight(1f))
            Switch(checked = Settings.shapedKeys, onCheckedChange = { Settings.shapedKeys = it; Settings.save() })
        }

        SectionTitle("Оформление")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !Settings.glass, onClick = { Settings.glass = false; Settings.save() }, label = { Text("Material You") })
            FilterChip(selected = Settings.glass, onClick = { Settings.glass = true; Settings.save() }, label = { Text("Liquid Glass") })
        }
        if (Settings.glass) {
            SliderRow("Прозрачность стекла", Settings.glassAlpha * 100f, 10f..70f, " %") { Settings.glassAlpha = it / 100f }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Размытие окна позади (Android 12+, не везде)", color = cs.onSurface, modifier = Modifier.weight(1f))
                Switch(checked = Settings.windowBlur, onCheckedChange = { Settings.windowBlur = it; Settings.save() })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = { bgPicker.launch(arrayOf("image/*")) }) { Text("Фон-картинка") }
            TextButton(onClick = { Settings.clearBackground() }) { Text("Убрать фон") }
        }
        SliderRow("Затемнение фона", Settings.bgDim * 100f, 0f..80f, " %") { Settings.bgDim = it / 100f }
        SliderRow("Размытие фона (Android 12+)", Settings.bgBlur, 0f..24f, " dp") { Settings.bgBlur = it }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Скриншоты в буфер", color = cs.onSurface)
            Switch(
                checked = Settings.screenshots,
                onCheckedChange = { on ->
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
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Переводчик (текст уходит на сервер перевода только по кнопке)", color = cs.onSurface, modifier = Modifier.weight(1f))
            Switch(checked = Settings.translatorEnabled, onCheckedChange = { Settings.translatorEnabled = it; Settings.save() })
        }
        FilledTonalButton(onClick = { dictPicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Загрузить словарь для свайпа (.txt)")
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Отладка касаний", color = cs.onSurface)
            Switch(checked = Settings.debug, onCheckedChange = { Settings.debug = it; Settings.save() })
        }

        SectionTitle("Шрифт")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Settings.fontPresets.forEach { (key, label) ->
                FilterChip(
                    selected = Settings.fontKey == key,
                    onClick = { Settings.fontKey = key; Settings.save() },
                    label = { Text(label, fontFamily = Settings.presetFamily(key)) },
                )
            }
        }
        FilledTonalButton(
            onClick = { fontPicker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Выбрать файл шрифта (.ttf / .otf)") }

        SectionTitle("Эффект нажатия")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PressEffect.entries.forEach { e ->
                FilterChip(
                    selected = Settings.effect == e,
                    onClick = { Settings.effect = e; Settings.save() },
                    label = { Text(e.label) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Вибрация", color = cs.onSurface)
            Switch(checked = Settings.haptics, onCheckedChange = { Settings.haptics = it; Settings.save() })
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Подсказки слов", color = cs.onSurface)
            Switch(checked = Settings.suggestions, onCheckedChange = { Settings.suggestions = it; Settings.save() })
        }
        TextButton(onClick = { Dictionary.clearUser() }) { Text("Забыть выученные слова") }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Буфер обмена (текст и картинки)", color = cs.onSurface)
            Switch(checked = Settings.clipboard, onCheckedChange = { Settings.clipboard = it; Settings.save() })
        }
        TextButton(onClick = { ClipStore.clear() }) { Text("Очистить буфер") }

        SectionTitle("Цвета")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorMode.entries.forEach { m ->
                FilterChip(
                    selected = Settings.colorMode == m,
                    onClick = { Settings.colorMode = m; Settings.save() },
                    label = { Text(m.label) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(-1f, 0f, 25f, 50f, 140f, 175f, 210f, 260f, 320f).forEach { h ->
                val selected = Settings.colorMode == ColorMode.CUSTOM && Settings.hue == h
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(AccentShapeImpl(Settings.accentShape))
                        .background(if (h < 0f) Color.White else hslColor(h, .8f, .55f))
                        .border(if (selected) 3.dp else 1.dp, if (selected) cs.onSurface else cs.outlineVariant, AccentShapeImpl(Settings.accentShape))
                        .clickable {
                            Settings.hue = h
                            if (h < 0f) Settings.themeMode = 1
                            Settings.colorMode = ColorMode.CUSTOM
                            Settings.save()
                        },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Тема: авто", 1 to "Светлая", 2 to "Тёмная").forEach { (v, label) ->
                FilterChip(
                    selected = Settings.themeMode == v,
                    onClick = { Settings.themeMode = v; Settings.save() },
                    label = { Text(label) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("AMOLED: глубокий чёрный (в тёмной теме)", color = cs.onSurface, modifier = Modifier.weight(1f))
            Switch(checked = Settings.amoled, onCheckedChange = { Settings.amoled = it; Settings.save() })
        }
        if (Settings.colorMode == ColorMode.CUSTOM && Settings.hue >= 0f) {
            SliderRow("Оттенок", Settings.hue, 0f..360f, "°") { Settings.hue = it }
        }

        TextButton(onClick = { Settings.reset() }) { Text("Сбросить настройки") }

        SectionTitle("Тест в системном поле")
        var text by remember { mutableStateOf("") }
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            label = { Text("Тестовое поле") },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.height(24.dp))
    }
}
