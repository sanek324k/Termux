package com.vibe.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmojiPanel(actions: KeyboardActions) {
    val cs = MaterialTheme.colorScheme
    val gap = Settings.gap.dp
    val cats = EmojiData.categories
    var tab by remember { mutableStateOf(if (EmojiStore.recent.isEmpty()) 1 else 0) }
    val list = if (tab == 0) EmojiStore.recent else cats[tab - 1].second
    val gridHeight = (Settings.keyHeight * 3 + Settings.gap * 2).dp

    Box(
        Modifier
            .fillMaxWidth()
            .height(gridHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(glassify(cs.surfaceContainerHigh)),
    ) {
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Здесь появятся недавние эмодзи", color = cs.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(46.dp),
                contentPadding = PaddingValues(6.dp),
            ) {
                items(list) { em ->
                    Box(
                        Modifier
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { actions.emoji(em) },
                        contentAlignment = Alignment.Center,
                    ) { Text(em, fontSize = 26.sp) }
                }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Settings.hGap.dp)) {
        Key(
            label = "ABC", weight = 1.5f,
            container = cs.secondaryContainer, content = cs.onSecondaryContainer,
            fontSize = 16f,
        ) { actions.toggleLayer() }

        Row(
            Modifier
                .weight(7f)
                .height(Settings.keyHeight.dp)
                .clip(RoundedCornerShape(Settings.cornerRadius.dp))
                .background(glassify(cs.surfaceContainerHighest))
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icons = listOf("🕘") + cats.map { it.first }
            val s = (Settings.keyHeight - 12f).coerceAtLeast(32f).dp
            icons.forEachIndexed { i, ic ->
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(s)
                        .clip(CircleShape)
                        .background(if (i == tab) cs.primaryContainer else Color.Transparent)
                        .clickable { tab = i },
                    contentAlignment = Alignment.Center,
                ) { Text(ic, fontSize = 20.sp) }
            }
        }

        Key(
            label = "⌫", weight = 1.5f,
            container = cs.secondaryContainer, content = cs.onSecondaryContainer,
            repeat = true,
        ) { actions.backspace() }
    }
}
