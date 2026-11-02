package com.vibe.keyboard

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.max
import org.json.JSONArray
import org.json.JSONObject

/** Мост к текстовому полю: сервис клавиатуры подставляет сюда чтение и замену текста. */
object FieldBridge {
    var read: () -> String = { "" }
    var replace: (String) -> Unit = {}
}

object ToolsState {
    var tab by mutableStateOf(0)
    var calcExpr by mutableStateOf("")
    var trTarget by mutableStateOf("auto")
    var trSource by mutableStateOf("")
    var trResult by mutableStateOf("")
    var trError by mutableStateOf("")
    var trBusy by mutableStateOf(false)
}

// ---------------------------------------------------------------- калькулятор

object Calc {
    private class P(s: String) {
        val t: String = s.replace('×', '*').replace('÷', '/').replace('−', '-').replace(',', '.').replace(" ", "")
        var i = 0
        fun done() = i >= t.length

        fun expr(): Double {
            var v = term()
            while (i < t.length && (t[i] == '+' || t[i] == '-')) {
                val op = t[i++]
                val r = term()
                v = if (op == '+') v + r else v - r
            }
            return v
        }

        fun term(): Double {
            var v = factor()
            while (i < t.length && (t[i] == '*' || t[i] == '/')) {
                val op = t[i++]
                val r = factor()
                v = if (op == '*') v * r else v / r
            }
            return v
        }

        fun factor(): Double {
            if (i < t.length && t[i] == '-') { i++; return -factor() }
            if (i < t.length && t[i] == '+') { i++; return factor() }
            var v: Double
            if (i < t.length && t[i] == '(') {
                i++
                v = expr()
                if (i >= t.length || t[i] != ')') throw IllegalArgumentException("paren")
                i++
            } else {
                val st = i
                while (i < t.length && (t[i].isDigit() || t[i] == '.')) i++
                if (st == i) throw IllegalArgumentException("num")
                v = t.substring(st, i).toDouble()
            }
            while (i < t.length && t[i] == '%') { i++; v /= 100.0 }
            return v
        }
    }

    fun eval(src: String): Double? {
        return try {
            var s = src.trimEnd('+', '−', '×', '÷', '-', '*', '/')
            if (s.isEmpty()) return null
            val open = s.count { it == '(' } - s.count { it == ')' }
            if (open > 0) s += ")".repeat(open)
            val p = P(s)
            val v = p.expr()
            if (!p.done() || v.isNaN() || v.isInfinite()) null else v
        } catch (e: Exception) {
            null
        }
    }

    fun format(d: Double): String {
        if (d == Math.rint(d) && abs(d) < 1e15) return d.toLong().toString()
        return java.math.BigDecimal(d).round(java.math.MathContext(12)).stripTrailingZeros().toPlainString()
    }

    fun live(expr: String): String? {
        val v = eval(expr) ?: return null
        return format(v)
    }
}

private val CALC_ROWS = listOf(
    listOf("7", "8", "9", "÷", "C"),
    listOf("4", "5", "6", "×", "⌫"),
    listOf("1", "2", "3", "−", "("),
    listOf("0", ".", "=", "+", ")"),
)

private fun calcPress(k: String) {
    val e = ToolsState.calcExpr
    when (k) {
        "C" -> ToolsState.calcExpr = ""
        "⌫" -> ToolsState.calcExpr = e.dropLast(1)
        "=" -> {
            val r = Calc.live(e)
            if (r != null) ToolsState.calcExpr = r
        }
        "+", "−", "×", "÷" -> {
            if (e.isEmpty()) {
                if (k == "−") ToolsState.calcExpr = "−"
            } else if (e.last() in "+−×÷") {
                ToolsState.calcExpr = e.dropLast(1) + k
            } else {
                ToolsState.calcExpr = e + k
            }
        }
        else -> ToolsState.calcExpr = e + k
    }
}

// ---------------------------------------------------------------- переводчик

object Translator {
    private val main = Handler(Looper.getMainLooper())

    fun translate(text: String, target: String, cb: (String?, String?) -> Unit) {
        Thread {
            var res: String? = null
            var err: String? = null
            try {
                res = google(text, target)
            } catch (e: Exception) {
                err = e.message
            }
            if (res == null) {
                try {
                    res = myMemory(text, target)
                    err = null
                } catch (e: Exception) {
                    err = e.message
                }
            }
            val out = res
            val msg = if (out == null) "Нет сети или сервис недоступен" else null
            main.post { cb(out, msg) }
        }.start()
    }

    private fun http(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 7000
        c.readTimeout = 10000
        c.setRequestProperty("User-Agent", "Mozilla/5.0")
        try {
            if (c.responseCode != 200) throw IOException("HTTP " + c.responseCode)
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun google(text: String, target: String): String {
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" +
            target + "&dt=t&q=" + URLEncoder.encode(text, "UTF-8")
        val arr = JSONArray(http(url))
        val parts = arr.getJSONArray(0)
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val p = parts.optJSONArray(i) ?: continue
            sb.append(p.optString(0, ""))
        }
        val out = sb.toString()
        if (out.isEmpty()) throw IOException("empty")
        return out
    }

    private fun myMemory(text: String, target: String): String {
        val cyr = text.any { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }
        var src = if (cyr) "ru" else "en"
        if (src == target) src = if (target == "en") "ru" else "en"
        val url = "https://api.mymemory.translated.net/get?q=" +
            URLEncoder.encode(text.take(450), "UTF-8") + "&langpair=" + src + "|" + target
        val o = JSONObject(http(url))
        val t = o.getJSONObject("responseData").optString("translatedText", "")
        if (t.isEmpty()) throw IOException("empty")
        return t
    }
}

private fun doTranslate() {
    if (ToolsState.trBusy) return
    if (!Settings.translatorEnabled) {
        ToolsState.trError = "Переводчик выключен в настройках"
        return
    }
    val src = FieldBridge.read().trim()
    if (src.isEmpty()) {
        ToolsState.trResult = ""
        ToolsState.trError = "В поле нет текста"
        return
    }
    val text = src.take(1500)
    ToolsState.trSource = text
    ToolsState.trError = ""
    ToolsState.trResult = ""
    ToolsState.trBusy = true
    val cyr = text.any { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }
    val tgt = if (ToolsState.trTarget == "auto") (if (cyr) "en" else "ru") else ToolsState.trTarget
    Translator.translate(text, tgt) { res, err ->
        ToolsState.trBusy = false
        ToolsState.trResult = res ?: ""
        ToolsState.trError = if (res == null) (err ?: "Ошибка перевода") else ""
    }
}

// ---------------------------------------------------------------- панель

@Composable
fun ToolsPanel(actions: KeyboardActions) {
    val cs = MaterialTheme.colorScheme
    val tab = ToolsState.tab
    Row(
        Modifier.fillMaxWidth().height(42.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("📋 Буфер", "🌐 Перевод", "🧮 Счёт").forEachIndexed { i, label ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(21.dp))
                    .background(if (i == tab) cs.primaryContainer else glassify(cs.surfaceContainerHighest))
                    .clickable { ToolsState.tab = i },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (i == tab) cs.onPrimaryContainer else cs.onSurface,
                    fontSize = 15.sp,
                    maxLines = 1,
                    fontFamily = Settings.currentFont(),
                )
            }
        }
    }
    when (tab) {
        0 -> ClipboardBody(actions)
        1 -> TranslatorBody(actions)
        else -> CalcBody(actions)
    }
}

@Composable
private fun TranslatorBody(actions: KeyboardActions) {
    val cs = MaterialTheme.colorScheme
    val gridHeight = (Settings.keyHeight * 3 + Settings.gap * 2).dp
    val langs = listOf(
        "auto" to "Авто", "ru" to "RU", "en" to "EN", "de" to "DE", "fr" to "FR",
        "es" to "ES", "it" to "IT", "tr" to "TR", "uk" to "UK",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(gridHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(glassify(cs.surfaceContainerHigh)),
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                langs.forEach { (code, label) ->
                    val sel = ToolsState.trTarget == code
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) cs.primaryContainer else glassify(cs.surfaceContainerHighest))
                            .clickable { ToolsState.trTarget = code }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            label, fontSize = 14.sp,
                            color = if (sel) cs.onPrimaryContainer else cs.onSurface,
                        )
                    }
                }
            }
            if (ToolsState.trSource.isNotEmpty()) {
                Text(
                    ToolsState.trSource, color = cs.onSurfaceVariant, fontSize = 13.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            val isError = ToolsState.trError.isNotEmpty()
            val body = when {
                ToolsState.trBusy -> "Перевожу…"
                isError -> ToolsState.trError
                ToolsState.trResult.isNotEmpty() -> ToolsState.trResult
                else -> "Набери текст в поле (или выдели часть) и нажми «Перевести». Текст уходит на сервер перевода только по этой кнопке."
            }
            Text(
                body,
                color = if (isError) cs.error else cs.onSurface,
                fontSize = 16.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                fontFamily = Settings.currentFont(),
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Settings.hGap.dp)) {
        Key(
            label = "ABC", weight = 2f,
            container = cs.secondaryContainer, content = cs.onSecondaryContainer,
            fontSize = 16f,
        ) { actions.toggleLayer() }
        Key(
            label = "Перевести", weight = 3f,
            container = cs.tertiaryContainer, content = cs.onTertiaryContainer,
            fontSize = 15f,
        ) { doTranslate() }
        Key(
            label = "Заменить", weight = 3f,
            container = cs.primary, content = cs.onPrimary,
            fontSize = 15f,
        ) {
            if (ToolsState.trResult.isNotEmpty()) FieldBridge.replace(ToolsState.trResult)
        }
        Key(
            label = "⌫", weight = 2f,
            container = cs.secondaryContainer, content = cs.onSecondaryContainer,
            repeat = true,
        ) { actions.backspace() }
    }
}

@Composable
private fun CalcBody(actions: KeyboardActions) {
    val cs = MaterialTheme.colorScheme
    val expr = ToolsState.calcExpr
    val res = Calc.live(expr)
    val gridDp = max(Settings.keyHeight * 3 + Settings.gap * 2, 210f)
    val rowH = ((gridDp - 48f - 3f * Settings.gap - 8f) / 4f).coerceAtLeast(28f)

    Box(
        Modifier
            .fillMaxWidth()
            .height(gridDp.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(glassify(cs.surfaceContainerHigh)),
    ) {
        Column(Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.spacedBy(Settings.gap.dp)) {
            Column(
                Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    expr.ifEmpty { "0" }, fontSize = 15.sp, color = cs.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (res != null && res != expr) "= $res" else "",
                    fontSize = 20.sp, color = cs.onSurface, maxLines = 1,
                )
            }
            CALC_ROWS.forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(Settings.hGap.dp)) {
                    r.forEach { k ->
                        val isOp = k == "÷" || k == "×" || k == "−" || k == "+"
                        val isFn = k == "C" || k == "⌫"
                        val container = when {
                            k == "=" -> cs.primary
                            isOp -> cs.secondaryContainer
                            isFn -> cs.tertiaryContainer
                            else -> cs.surfaceContainerHighest
                        }
                        val content = when {
                            k == "=" -> cs.onPrimary
                            isOp -> cs.onSecondaryContainer
                            isFn -> cs.onTertiaryContainer
                            else -> cs.onSurface
                        }
                        Key(
                            label = k, weight = 1f, container = container, content = content,
                            fontSize = 20f, repeat = k == "⌫", height = rowH,
                        ) { calcPress(k) }
                    }
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Settings.hGap.dp)) {
        Key(
            label = "ABC", weight = 2f,
            container = cs.secondaryContainer, content = cs.onSecondaryContainer,
            fontSize = 16f,
        ) { actions.toggleLayer() }
        Key(
            label = "Вставить", weight = 6f,
            container = cs.primary, content = cs.onPrimary,
            fontSize = 16f,
        ) {
            val t = res ?: expr
            if (t.isNotEmpty()) actions.type(t)
        }
        Key(
            label = "⌫", weight = 2f,
            container = cs.secondaryContainer, content = cs.onSecondaryContainer,
            repeat = true,
        ) { actions.backspace() }
    }
}
