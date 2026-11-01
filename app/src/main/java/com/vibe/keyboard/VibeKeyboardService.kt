package com.vibe.keyboard

import android.inputmethodservice.InputMethodService
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

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onCreateInputView(): View {
        // Compose внутри IME требует owner'ов на decorView окна
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
        state.layer = Layer.LETTERS
        val caps = currentInputConnection?.getCursorCapsMode(info?.inputType ?: 0) ?: 0
        state.shift = if (caps != 0) Shift.ONCE else Shift.OFF
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    // ---- KeyboardActions ----

    override fun type(text: String) {
        currentInputConnection?.commitText(text, 1)
        if (state.shift == Shift.ONCE) state.shift = Shift.OFF
    }

    override fun backspace() = sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)

    override fun enter() {
        val ei = currentInputEditorInfo
        val action = ei?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = ei?.imeOptions?.and(EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (ei == null || noEnterAction ||
            action == EditorInfo.IME_ACTION_NONE ||
            action == EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        } else {
            currentInputConnection?.performEditorAction(action)
        }
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
    }
}
