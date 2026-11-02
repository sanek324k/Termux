package com.vibe.keyboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class VibeKeyboardService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    KeyboardActions {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    private val state = KeyboardState()
    private var suggestionsOk = true
    private var learnOk = true
    private var ignoreClip = false
    private var lastSwipe: String? = null
    private var watcher: ScreenshotWatcher? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { onClipChanged() }

    override fun onCreate() {
        super.onCreate()
        Settings.init(this)
        watcher = ScreenshotWatcher(this).also { it.start() }
        FieldBridge.read = { readFieldText() }
        FieldBridge.replace = { replaceFieldText(it) }
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).addPrimaryClipChangedListener(clipListener)
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onCreateInputView(): View {
        val decor = window.window!!.decorView
        decor.setViewTreeLifecycleOwner(this)
        decor.setViewTreeViewModelStoreOwner(this)
        decor.setViewTreeSavedStateRegistryOwner(this)

        return ComposeView(this).apply {
            setContent { KeyboardUi(state, this@VibeKeyboardService) }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        applyWindowStyle()
        state.layer = Layer.LETTERS

        val type = info?.inputType ?: 0
        val cls = type and InputType.TYPE_MASK_CLASS
        val variation = type and InputType.TYPE_MASK_VARIATION
        val isPassword = (cls == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )) || (cls == InputType.TYPE_CLASS_NUMBER &&
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        val isAddress = cls == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_URI ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
            )
        val noSuggest = (type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
        suggestionsOk = cls == InputType.TYPE_CLASS_TEXT && !isPassword && !isAddress && !noSuggest
        val noLearn = ((info?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
        learnOk = suggestionsOk && !noLearn
        state.sensitiveField = isPassword

        autoCap()
        refreshSuggestions()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd,
        )
        refreshSuggestions()
    }

    override fun onDestroy() {
        watcher?.stop()
        FieldBridge.read = { "" }
        FieldBridge.replace = { }
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).removePrimaryClipChangedListener(clipListener)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    // ---- helpers ----

    private fun readFieldText(): String {
        if (state.sensitiveField) return ""
        val ic = currentInputConnection ?: return ""
        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) return sel.toString()
        val before = ic.getTextBeforeCursor(1500, 0) ?: ""
        val after = ic.getTextAfterCursor(1500, 0) ?: ""
        return before.toString() + after.toString()
    }

    private fun replaceFieldText(newText: String) {
        val ic = currentInputConnection ?: return
        val sel = ic.getSelectedText(0)
        ic.beginBatchEdit()
        if (sel.isNullOrEmpty()) {
            val b = ic.getTextBeforeCursor(1500, 0)?.length ?: 0
            val a = ic.getTextAfterCursor(1500, 0)?.length ?: 0
            ic.deleteSurroundingText(b, a)
        }
        ic.commitText(newText, 1)
        ic.endBatchEdit()
        autoCap()
        refreshSuggestions()
    }

    private fun applyWindowStyle() {
        try {
            val w = window?.window ?: return
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                w.setBackgroundBlurRadius(if (Settings.glass && Settings.windowBlur) 90 else 0)
            }
        } catch (e: Exception) {
        }
    }

    private fun autoCap() {
        if (state.shift == Shift.LOCK) return
        val caps = currentInputConnection
            ?.getCursorCapsMode(currentInputEditorInfo?.inputType ?: 0) ?: 0
        state.shift = if (caps != 0) Shift.ONCE else Shift.OFF
    }

    private fun refreshSuggestions() {
        val swiped = lastSwipe
        if (swiped != null) {
            val tail = currentInputConnection?.getTextBeforeCursor(swiped.length + 1, 0)?.toString()
            if (tail == swiped + " ") return
            lastSwipe = null
        }
        if (!suggestionsOk || !Settings.suggestions) {
            state.currentWord = ""
            state.suggestions = emptyList()
            return
        }
        val before = currentInputConnection?.getTextBeforeCursor(40, 0) ?: ""
        val w = currentWord(before)
        state.currentWord = w
        state.suggestions = Dictionary.suggest(w, state.lang)
    }

    private fun learnCurrent() {
        if (learnOk && Settings.suggestions && state.currentWord.length >= 2) {
            Dictionary.learn(state.currentWord)
        }
    }

    // ---- KeyboardActions ----

    override fun type(text: String) {
        val ic = currentInputConnection ?: return
        if (text.length == 1 && !(text[0].isLetter() || text[0] == '\'')) learnCurrent()
        ic.commitText(text, 1)
        autoCap()
        refreshSuggestions()
    }

    override fun backspace() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        refreshSuggestions()
    }

    override fun enter() {
        learnCurrent()
        val ei = currentInputEditorInfo
        val action = ei?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = ((ei?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (ei == null || noEnterAction ||
            action == EditorInfo.IME_ACTION_NONE ||
            action == EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        } else {
            currentInputConnection?.performEditorAction(action)
        }
        autoCap()
        refreshSuggestions()
    }

    override fun toggleShift() {
        state.shift = when (state.shift) {
            Shift.OFF -> Shift.ONCE
            Shift.ONCE -> Shift.LOCK
            Shift.LOCK -> Shift.OFF
        }
    }

    override fun toggleLayer() {
        state.layer = if (state.layer == Layer.LETTERS) Layer.SYMBOLS else Layer.LETTERS
    }

    override fun switchLang() {
        state.lang = state.lang.next()
        refreshSuggestions()
    }

    override fun pickSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        var del = state.currentWord.length
        val swiped = lastSwipe
        if (swiped != null) {
            val tail = ic.getTextBeforeCursor(swiped.length + 1, 0)?.toString()
            if (tail == swiped + " ") del = swiped.length + 1
            lastSwipe = null
        }
        ic.beginBatchEdit()
        if (del > 0) ic.deleteSurroundingText(del, 0)
        ic.commitText("$word ", 1)
        ic.endBatchEdit()
        if (learnOk && Settings.suggestions) Dictionary.learn(word)
        autoCap()
        refreshSuggestions()
    }

    override fun emoji(e: String) {
        currentInputConnection?.commitText(e, 1)
        EmojiStore.add(e)
        refreshSuggestions()
    }

    override fun toggleEmoji() {
        state.layer = if (state.layer == Layer.EMOJI) Layer.LETTERS else Layer.EMOJI
    }

    override fun swipeWords(words: List<String>) {
        val ic = currentInputConnection ?: return
        if (words.isEmpty()) return
        val styled = words.map {
            when (state.shift) {
                Shift.OFF -> it
                Shift.ONCE -> it.replaceFirstChar { c -> c.uppercase() }
                Shift.LOCK -> it.uppercase()
            }
        }
        val b = ic.getTextBeforeCursor(1, 0)
        ic.beginBatchEdit()
        if (b != null && b.isNotEmpty() && b[0].isLetterOrDigit()) ic.commitText(" ", 1)
        ic.commitText(styled[0] + " ", 1)
        ic.endBatchEdit()
        if (learnOk && Settings.suggestions) Dictionary.learn(words[0])
        autoCap()
        if (suggestionsOk && Settings.suggestions) {
            lastSwipe = styled[0]
            state.currentWord = ""
            state.suggestions = styled.take(3)
        } else {
            refreshSuggestions()
        }
    }

    override fun toggleClipboard() {
        state.layer = if (state.layer == Layer.CLIPBOARD) Layer.LETTERS else Layer.CLIPBOARD
    }

    private fun onClipChanged() {
        if (!Settings.clipboard) return
        if (ignoreClip) { ignoreClip = false; return }
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return
        if (clip.itemCount == 0) return
        val desc = clip.description
        if (desc.extras?.getBoolean("android.content.extra.IS_SENSITIVE") == true) return
        val item = clip.getItemAt(0)
        var imageMime: String? = null
        for (i in 0 until desc.mimeTypeCount) {
            if (desc.getMimeType(i).startsWith("image/")) { imageMime = desc.getMimeType(i); break }
        }
        val uri = item.uri
        if (imageMime != null && uri != null) {
            ClipStore.addImage(this, uri, imageMime)
        } else {
            val t = item.coerceToText(this)?.toString()?.trim()
            if (!t.isNullOrEmpty()) ClipStore.addText(t)
        }
    }

    override fun pasteClip(item: ClipItem) {
        if (item.type == "text") {
            currentInputConnection?.commitText(item.text, 1)
            ClipStore.consume(item)
            autoCap()
            refreshSuggestions()
            return
        }
        val ic = currentInputConnection ?: return
        val ei = currentInputEditorInfo ?: return
        val uri = FileProvider.getUriForFile(this, "$packageName.clips", ClipStore.file(item))
        val supported = EditorInfoCompat.getContentMimeTypes(ei)
        val ok = supported.any { ClipDescription.compareMimeTypes(item.mime, it) }
        if (ok) {
            val info = InputContentInfoCompat(uri, ClipDescription("image", arrayOf(item.mime)), null)
            InputConnectionCompat.commitContent(
                ic, ei, info, InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null,
            )
            ClipStore.consume(item)
        } else {
            ignoreClip = true
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newUri(contentResolver, "image", uri))
            ClipStore.consume(item)
            Toast.makeText(this, "Поле не принимает картинки: скопировано, вставь долгим нажатием", Toast.LENGTH_LONG).show()
        }
    }
}
