package com.vibe.keyboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun BarIcon(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .width(44.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(21.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 21.sp) }
}

@Composable
fun SuggestionBar(state: KeyboardState, actions: KeyboardActions) {
    val cs = MaterialTheme.colorScheme
    val pending = ClipStore.pending
    Row(
        Modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolsButton { actions.toggleClipboard() }

        val sug = state.suggestions
        for (i in 0 until 3) {
            if (i == 1 && Settings.clipboard && pending != null && !state.sensitiveField) {
                // последнее скопированное вместо центральной подсказки, пока не вставят
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(glassify(cs.tertiaryContainer))
                        .clickable { actions.pasteClip(pending) }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (pending.type == "text") "📋 " + pending.text.replace('\n', ' ') else "🖼 Фото",
                        color = cs.onTertiaryContainer,
                        fontSize = 15.sp,
                        fontFamily = Settings.currentFont(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                val w = sug.getOrNull(i)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(21.dp))
                        .then(if (w != null) Modifier.clickable { actions.pickSuggestion(w) } else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (w != null) {
                        Text(
                            w,
                            color = if (i == 0) cs.primary else cs.onSurface,
                            fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 17.sp,
                            fontFamily = Settings.currentFont(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipCard(item: ClipItem, actions: KeyboardActions) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(glassify(cs.surfaceContainerHighest))
            .clickable { actions.pasteClip(item) },
    ) {
        if (item.type == "text") {
            Text(
                item.text,
                color = cs.onSurface,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp).padding(end = 18.dp),
            )
        } else {
            val bmp = remember(item.id) { ClipStore.thumbnail(item) }
            if (bmp != null) {
                Image(
                    bitmap = bmp, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🖼", fontSize = 28.sp) }
            }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(30.dp)
                .clickable { ClipStore.remove(item) },
            contentAlignment = Alignment.Center,
        ) { Text("✕", fontSize = 13.sp, color = cs.onSurfaceVariant) }
    }
}

@Composable
fun ClipboardBody(actions: KeyboardActions) {
    val cs = MaterialTheme.colorScheme
    val clips = ClipStore.items
    val gridHeight = (Settings.keyHeight * 3 + Settings.gap * 2).dp

    Box(
        Modifier
            .fillMaxWidth()
            .height(gridHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(glassify(cs.surfaceContainerHigh)),
    ) {
        if (clips.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Буфер пуст. Скопируй текст или картинку, и они появятся здесь",
                    color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(clips, key = { it.id }) { c -> ClipCard(c, actions) }
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
            label = "Очистить всё", weight = 6f,
            container = cs.tertiaryContainer, content = cs.onTertiaryContainer,
            fontSize = 16f,
        ) { ClipStore.clear() }
        Key(
            label = "⌫", weight = 2f,
            container = cs.secondaryContainer, content = cs.onSecondaryContainer,
            repeat = true,
        ) { actions.backspace() }
    }
}
