package com.vibe.keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VibeTheme {
                var text by remember { mutableStateOf("") }
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Vibe Keyboard", style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("1. Включи клавиатуру  2. Выбери её  3. Потести ниже",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Включить в настройках") }
                    FilledTonalButton(
                        onClick = {
                            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                                .showInputMethodPicker()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Выбрать клавиатуру") }
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        label = { Text("Тестовое поле") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
