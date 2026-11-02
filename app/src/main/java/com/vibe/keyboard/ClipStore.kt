package com.vibe.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class ClipItem(
    val id: Long,
    val type: String, // "text" | "image"
    val text: String,
    val file: String,
    val mime: String,
)

object ClipStore {
    private var prefs: SharedPreferences? = null
    private var dir: File? = null

    var items by mutableStateOf<List<ClipItem>>(emptyList())
    /** Последнее скопированное, ещё не вставленное. Показывается вместо центральной подсказки. */
    var pending by mutableStateOf<ClipItem?>(null)

    fun init(ctx: Context) {
        if (prefs != null) return
        val app = ctx.applicationContext
        prefs = app.getSharedPreferences("vibe_clip", Context.MODE_PRIVATE)
        dir = File(app.filesDir, "clips").also { it.mkdirs() }
        try {
            val arr = JSONArray(prefs?.getString("items", "[]") ?: "[]")
            val list = ArrayList<ClipItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val c = ClipItem(
                    o.getLong("id"), o.getString("type"),
                    o.optString("text"), o.optString("file"), o.optString("mime"),
                )
                if (c.type == "text" || file(c).exists()) list.add(c)
            }
            items = list
        } catch (e: Exception) {
            items = emptyList()
        }
    }

    fun file(item: ClipItem) = File(dir, item.file)

    fun addText(t: String) {
        insertItem(ClipItem(System.currentTimeMillis(), "text", t.take(5000), "", "text/plain"))
    }

    fun addImage(ctx: Context, uri: Uri, mime: String) {
        val app = ctx.applicationContext
        Thread {
            try {
                val ext = when {
                    mime.contains("png") -> "png"
                    mime.contains("gif") -> "gif"
                    mime.contains("webp") -> "webp"
                    else -> "jpg"
                }
                val id = System.currentTimeMillis()
                val f = File(dir, "$id.$ext")
                val input = app.contentResolver.openInputStream(uri) ?: return@Thread
                input.use { ins -> f.outputStream().use { out -> ins.copyTo(out) } }
                if (f.length() > 8_000_000L || f.length() == 0L) {
                    f.delete()
                    return@Thread
                }
                Handler(Looper.getMainLooper()).post {
                    insertItem(ClipItem(id, "image", "", f.name, mime))
                }
            } catch (e: Exception) {
                // не получилось прочитать картинку: молча пропускаем
            }
        }.start()
    }

    private fun insertItem(item: ClipItem) {
        val without = items.filterNot {
            it.type == "text" && item.type == "text" && it.text == item.text
        }
        val keep = ArrayList<ClipItem>()
        var images = 0
        for (c in listOf(item) + without) {
            val ok = if (c.type == "image") { images++; images <= 12 } else true
            if (ok && keep.size < 30) keep.add(c) else if (c.type == "image") file(c).delete()
        }
        items = keep
        pending = item
        save()
    }

    fun consume(item: ClipItem) {
        if (pending?.id == item.id) pending = null
    }

    fun remove(item: ClipItem) {
        if (item.type == "image") file(item).delete()
        items = items.filter { it.id != item.id }
        if (pending?.id == item.id) pending = null
        save()
    }

    fun clear() {
        items.forEach { if (it.type == "image") file(it).delete() }
        items = emptyList()
        pending = null
        save()
    }

    fun thumbnail(item: ClipItem): ImageBitmap? = try {
        val f = file(item)
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        var s = 1
        while (o.outWidth / s > 400) s *= 2
        val o2 = BitmapFactory.Options().apply { inSampleSize = s }
        BitmapFactory.decodeFile(f.path, o2)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }

    private fun save() {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("type", it.type)
                    .put("text", it.text).put("file", it.file).put("mime", it.mime),
            )
        }
        prefs?.edit()?.putString("items", arr.toString())?.apply()
    }
}
