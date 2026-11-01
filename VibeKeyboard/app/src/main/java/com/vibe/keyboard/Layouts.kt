package com.vibe.keyboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Layer { LETTERS, SYMBOLS, EMOJI, CLIPBOARD }
enum class Shift { OFF, ONCE, LOCK }

enum class Lang(val label: String, val rows: List<String>) {
    EN("EN", listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")),
    RU("RU", listOf("йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю"));

    fun next() = if (this == EN) RU else EN
}

val SYMBOL_ROWS = listOf("1234567890", "@#\$_&-+()/", "*\"':;!?")
val SYMBOL_ROWS_2 = listOf("~`|•√π÷×¶∆", "£¢€¥^°={}\\", "%©®™✓[]")

class KeyboardState {
    var lang by mutableStateOf(Lang.EN)
    var layer by mutableStateOf(Layer.LETTERS)
    var shift by mutableStateOf(Shift.OFF)
    var suggestions by mutableStateOf<List<String>>(emptyList())
    var currentWord by mutableStateOf("")
    var sensitiveField by mutableStateOf(false)
    var symbolPage by mutableStateOf(0)
}

interface KeyboardActions {
    fun type(text: String)
    fun backspace()
    fun enter()
    fun toggleShift()
    fun toggleLayer()
    fun switchLang()
    fun pickSuggestion(word: String)
    fun emoji(e: String)
    fun toggleEmoji()
    fun pasteClip(item: ClipItem)
    fun toggleClipboard()
    fun swipeWords(words: List<String>)
}
