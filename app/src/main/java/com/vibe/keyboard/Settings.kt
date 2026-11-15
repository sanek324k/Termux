package com.vibe.keyboard

import android.content.Context

enum class KeyboardTheme {
    MATERIAL_YOU,
    MATERIAL_3_EXPRESSIVE,
    MINECRAFT,
    CYBERPUNK
}

enum class EmojiStyleSystem {
    SYSTEM,
    APPLE
}

object SettingsManager {
    private const val PREFS_NAME = "vibe_keyboard_prefs"
    
    fun getTheme(context: Context): KeyboardTheme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return runCatching { 
            KeyboardTheme.valueOf(prefs.getString("keyboard_theme", "MATERIAL_3_EXPRESSIVE") ?: "MATERIAL_3_EXPRESSIVE")
        }.getOrDefault(KeyboardTheme.MATERIAL_3_EXPRESSIVE)
    }

    fun setTheme(context: Context, theme: KeyboardTheme) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("keyboard_theme", theme.name).apply()
    }

    fun isLiquidGlassEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("liquid_glass_enabled", true)
    }

    fun setLiquidGlassEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("liquid_glass_enabled", enabled).apply()
    }
    
    fun getEmojiStyle(context: Context): EmojiStyleSystem {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return runCatching {
            EmojiStyleSystem.valueOf(prefs.getString("emoji_style", "APPLE") ?: "APPLE")
        }.getOrDefault(EmojiStyleSystem.APPLE)
    }
}
