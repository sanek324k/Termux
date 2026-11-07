package com.vibe.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PressEffect(val label: String) {
    COMBO("Комбо"),
    MORPH("Морф"),
    SQUISH("Сжатие"),
    BOUNCE("Пружина"),
    GLOW("Подсветка"),
    NONE("Без эффекта"),
}

enum class ColorMode(val label: String) {
    DYNAMIC("Из обоев"),
    CUSTOM("Свой цвет"),
}

/** Общие настройки: приложение и клавиатура живут в одном процессе, поэтому state общий. */
object Settings {
    private var prefs: SharedPreferences? = null

    var keyHeight by mutableStateOf(62f)
    var gap by mutableStateOf(6f)
    var hGap by mutableStateOf(4f)
    var sidePad by mutableStateOf(3f)
    var bottomPad by mutableStateOf(10f)
    var cornerRadius by mutableStateOf(26f)
    var fontSize by mutableStateOf(22f)
    var effect by mutableStateOf(PressEffect.COMBO)
    var haptics by mutableStateOf(true)
    var colorMode by mutableStateOf(ColorMode.DYNAMIC)
    var hue by mutableStateOf(220f)
    var suggestions by mutableStateOf(true)
    var clipboard by mutableStateOf(true)
    var swipeTyping by mutableStateOf(true)
    var glass by mutableStateOf(false)
    var themeMode by mutableStateOf(0)
    var amoled by mutableStateOf(true)
    var accentShape by mutableStateOf(AccentShape.COOKIE)
    var shapedKeys by mutableStateOf(true)
    var translatorEnabled by mutableStateOf(true)
    var glassAlpha by mutableStateOf(0.38f)
    var windowBlur by mutableStateOf(false)
    var bgDim by mutableStateOf(0.25f)
    var bgBlur by mutableStateOf(0f)
    var screenshots by mutableStateOf(false)
    var debug by mutableStateOf(false)
    var bgVersion by mutableStateOf(0)
    private var bgFile: File? = null
    private var bgCacheVer = -1
    private var bgCache: ImageBitmap? = null

    fun backgroundImage(): ImageBitmap? {
        val v = bgVersion
        if (v != bgCacheVer) {
            bgCache = try {
                val f = bgFile
                if (f != null && f.exists()) BitmapFactory.decodeFile(f.path)?.asImageBitmap() else null
            } catch (e: Exception) {
                null
            }
            bgCacheVer = v
        }
        return bgCache
    }

    fun importBackground(ctx: Context, uri: Uri): Boolean = try {
        val target = bgFile ?: return false
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var s = 1
        while (o.outWidth / s > 1600) s *= 2
        val bmp = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s },
        ) ?: return false
        target.parentFile?.mkdirs()
        target.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bgVersion++
        true
    } catch (e: Exception) {
        false
    }

    fun clearBackground() {
        try { bgFile?.delete() } catch (e: Exception) { }
        bgVersion++
    }
    var fontKey by mutableStateOf("default")
    var fontVersion by mutableStateOf(0)
    private var fontFile: File? = null
    private var cacheKey = ""
    private var cacheVer = -1
    private var cacheFont: FontFamily = FontFamily.Default

    val fontPresets = listOf(
        "default" to "Системный", "condensed" to "Узкий", "light" to "Тонкий",
        "medium" to "Средний", "black" to "Жирный", "serif" to "С засечками",
        "mono" to "Моно", "casual" to "Casual", "cursive" to "Рукописный",
        "custom" to "Свой файл",
    )

    fun presetFamily(key: String): FontFamily = when (key) {
        "condensed" -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
        "light" -> FontFamily(Typeface.create("sans-serif-light", Typeface.NORMAL))
        "medium" -> FontFamily(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        "black" -> FontFamily(Typeface.create("sans-serif-black", Typeface.NORMAL))
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        "casual" -> FontFamily(Typeface.create("casual", Typeface.NORMAL))
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    fun currentFont(): FontFamily {
        val k = fontKey
        val v = fontVersion
        if (k == cacheKey && v == cacheVer) return cacheFont
        cacheFont = if (k == "custom") (loadCustom() ?: FontFamily.Default) else presetFamily(k)
        cacheKey = k
        cacheVer = v
        return cacheFont
    }

    private fun loadCustom(): FontFamily? = try {
        val f = fontFile
        if (f != null && f.exists()) FontFamily(Typeface.createFromFile(f)) else null
    } catch (e: Exception) {
        null
    }

    fun importFont(ctx: Context, uri: Uri): Boolean = try {
        val target = fontFile ?: return false
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "custom.tmp")
        val input = ctx.contentResolver.openInputStream(uri) ?: return false
        input.use { ins -> tmp.outputStream().use { out -> ins.copyTo(out) } }
        Typeface.createFromFile(tmp)
        if (target.exists()) target.delete()
        tmp.renameTo(target)
        fontKey = "custom"
        fontVersion++
        save()
        true
    } catch (e: Exception) {
        false
    }

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("vibe_settings", Context.MODE_PRIVATE)
        prefs = p
        Dictionary.init(context)
        EmojiStore.init(context)
        ClipStore.init(context)
        fontFile = File(context.applicationContext.filesDir, "fonts/custom.ttf")
        bgFile = File(context.applicationContext.filesDir, "bg/keyboard.jpg")
        keyHeight = p.getFloat("keyHeight", keyHeight)
        gap = p.getFloat("gap", gap)
        hGap = p.getFloat("hGap", hGap)
        sidePad = p.getFloat("sidePad", sidePad)
        bottomPad = p.getFloat("bottomPad", bottomPad)
        cornerRadius = p.getFloat("cornerRadius", cornerRadius)
        fontSize = p.getFloat("fontSize", fontSize)
        haptics = p.getBoolean("haptics", haptics)
        hue = p.getFloat("hue", hue)
        suggestions = p.getBoolean("suggestions", suggestions)
        clipboard = p.getBoolean("clipboard", clipboard)
        swipeTyping = p.getBoolean("swipeTyping", swipeTyping)
        glass = p.getBoolean("glass", glass)
        themeMode = p.getInt("themeMode", themeMode)
        amoled = p.getBoolean("amoled", amoled)
        shapedKeys = p.getBoolean("shapedKeys", shapedKeys)
        accentShape = runCatching { AccentShape.valueOf(p.getString("accentShape", "") ?: "") }
            .getOrDefault(AccentShape.COOKIE)
        translatorEnabled = p.getBoolean("translatorEnabled", translatorEnabled)
        glassAlpha = p.getFloat("glassAlpha", glassAlpha)
        windowBlur = p.getBoolean("windowBlur", windowBlur)
        bgDim = p.getFloat("bgDim", bgDim)
        bgBlur = p.getFloat("bgBlur", bgBlur)
        screenshots = p.getBoolean("screenshots", screenshots)
        debug = p.getBoolean("debug", debug)
        fontKey = p.getString("fontKey", "default") ?: "default"
        effect = runCatching { PressEffect.valueOf(p.getString("effect", "") ?: "") }
            .getOrDefault(PressEffect.COMBO)
        colorMode = runCatching { ColorMode.valueOf(p.getString("colorMode", "") ?: "") }
            .getOrDefault(ColorMode.DYNAMIC)
    }

    fun save() {
        prefs?.edit()
            ?.putFloat("keyHeight", keyHeight)
            ?.putFloat("gap", gap)
            ?.putFloat("hGap", hGap)
            ?.putFloat("sidePad", sidePad)
            ?.putFloat("bottomPad", bottomPad)
            ?.putFloat("cornerRadius", cornerRadius)
            ?.putFloat("fontSize", fontSize)
            ?.putBoolean("haptics", haptics)
            ?.putFloat("hue", hue)
            ?.putBoolean("suggestions", suggestions)
            ?.putBoolean("clipboard", clipboard)
            ?.putBoolean("swipeTyping", swipeTyping)
            ?.putBoolean("glass", glass)
            ?.putInt("themeMode", themeMode)
            ?.putBoolean("amoled", amoled)
            ?.putBoolean("shapedKeys", shapedKeys)
            ?.putString("accentShape", accentShape.name)
            ?.putBoolean("translatorEnabled", translatorEnabled)
            ?.putFloat("glassAlpha", glassAlpha)
            ?.putBoolean("windowBlur", windowBlur)
            ?.putFloat("bgDim", bgDim)
            ?.putFloat("bgBlur", bgBlur)
            ?.putBoolean("screenshots", screenshots)
            ?.putBoolean("debug", debug)
            ?.putString("fontKey", fontKey)
            ?.putString("effect", effect.name)
            ?.putString("colorMode", colorMode.name)
            ?.apply()
    }

    fun reset() {
        keyHeight = 62f; gap = 6f; hGap = 4f; sidePad = 3f; bottomPad = 10f; cornerRadius = 26f; fontSize = 22f
        effect = PressEffect.COMBO; haptics = true
        colorMode = ColorMode.DYNAMIC; hue = 220f; suggestions = true; clipboard = true; swipeTyping = true; fontKey = "default"; glass = false; themeMode = 0; amoled = true; accentShape = AccentShape.COOKIE; shapedKeys = true; translatorEnabled = true; glassAlpha = 0.38f; bgDim = 0.25f; bgBlur = 0f; windowBlur = false
        save()
    }
}
