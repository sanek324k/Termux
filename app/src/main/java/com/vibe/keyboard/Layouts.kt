package com.vibe.keyboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Layer { LETTERS, SYMBOLS }
enum class Shift { OFF, ONCE, LOCK }

enum class Lang(val label: String, val rows: List<String>) {
    EN("EN", listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")),
    RU("RU", listOf("йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю"));

    fun next() = if (this == EN) RU else EN
}

val SYMBOL_ROWS = listOf("1234567890", "@#$%&-+()/", "*\"':;!?")

class KeyboardState {
    var lang by mutableStateOf(Lang.EN)
    var layer by mutableStateOf(Layer.LETTERS)
    var shift by mutableStateOf(Shift.OFF)
}

interface KeyboardActions {
    fun type(text: String)
    fun backspace()
    fun enter()
    fun toggleShift()
    fun toggleLayer()
    fun switchLang()
}
