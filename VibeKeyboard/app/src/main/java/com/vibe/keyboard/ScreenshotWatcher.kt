package com.vibe.keyboard

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat

/** Следит за новыми скриншотами в галерее и кладёт их в историю буфера. */
class ScreenshotWatcher(private val ctx: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var lastId = 0L
    private val check = Runnable { checkNew() }
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            handler.removeCallbacks(check)
            handler.postDelayed(check, 900)
        }
    }

    fun start() {
        try {
            ctx.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer,
            )
        } catch (e: Exception) {
        }
    }

    fun stop() {
        try {
            handler.removeCallbacks(check)
            ctx.contentResolver.unregisterContentObserver(observer)
        } catch (e: Exception) {
        }
    }

    private fun hasPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkNew() {
        if (!Settings.screenshots || !Settings.clipboard || !hasPermission()) return
        try {
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val proj = ArrayList<String>()
            proj.add(MediaStore.Images.Media._ID)
            proj.add(MediaStore.Images.Media.DISPLAY_NAME)
            proj.add(MediaStore.Images.Media.MIME_TYPE)
            proj.add(MediaStore.Images.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= 29) proj.add("relative_path")
            val since = System.currentTimeMillis() / 1000 - 25
            ctx.contentResolver.query(
                uri, proj.toTypedArray(),
                MediaStore.Images.Media.DATE_ADDED + " >= ?", arrayOf(since.toString()),
                MediaStore.Images.Media.DATE_ADDED + " DESC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: ""
                    val mime = c.getString(2) ?: "image/png"
                    val path = if (Build.VERSION.SDK_INT >= 29) (c.getString(4) ?: "") else ""
                    if (id <= lastId) continue
                    val shot = name.contains("screenshot", true) ||
                        path.contains("screenshot", true) ||
                        name.contains("скриншот", true) ||
                        path.contains("скриншот", true)
                    if (shot) {
                        lastId = id
                        ClipStore.addImage(ctx, ContentUris.withAppendedId(uri, id), mime)
                        break
                    }
                }
            }
        } catch (e: Exception) {
        }
    }
}
