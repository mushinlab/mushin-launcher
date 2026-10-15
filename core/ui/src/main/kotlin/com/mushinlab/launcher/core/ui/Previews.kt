package com.mushinlab.launcher.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.ShapeVertex
import com.mushinlab.launcher.core.model.SwipeDirection

private val PreviewPalettes = listOf(Palettes.Graphite, Palettes.Paper)

private object LetterIcons : IconProvider {
    override fun peek(key: AppKey, sizePx: Int): AppIconImage = AppIconImage.Letter

    override suspend fun load(key: AppKey, sizePx: Int): AppIconImage = AppIconImage.Letter
}

@Composable
private fun InBothPalettes(content: @Composable () -> Unit) {
    Column {
        PreviewPalettes.forEach { palette ->
            MushinTheme(palette) {
                Box(Modifier.background(LocalMushinColors.current.background).padding(8.dp)) { content() }
            }
        }
    }
}

@Preview(widthDp = 320, heightDp = 560)
@Composable
private fun AnchoredPanelPreview() {
    InBothPalettes {
        Box(Modifier.fillMaxWidth().height(264.dp)) {
            AnchoredPanel(anchor = Offset(200f, 40f), onDismiss = {}) {
                PanelHeader("Space")
                PanelItem("Rename", tag = "rename", onClick = {})
                PanelItem("Hide", tag = "hide", bold = true, onClick = {})
                PanelItem("Graphite", tag = "graphite", radio = true, onClick = {})
                PanelItem("Paper", tag = "paper", radio = false, onClick = {})
            }
        }
    }
}

@Preview(widthDp = 320)
@Composable
private fun PressableRowsPreview() {
    InBothPalettes {
        Column(Modifier.fillMaxWidth()) {
            listOf("Calendar", "Camera", "Clock").forEachIndexed { index, label ->
                Box(
                    Modifier
                        .rowHeight(48.dp)
                        .background(LocalMushinColors.current.zebra(index))
                        .pressable(onClick = {})
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    SingleLineText(
                        label,
                        MushinType.style(MushinType.resultSize, LocalMushinColors.current.textPrimary),
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Preview
@Composable
private fun AppIconPreview() {
    InBothPalettes {
        CompositionLocalProvider(LocalIconProvider provides LetterIcons) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Maps", "Почта", "2048").forEach { label ->
                    AppIcon(AppKey("preview.$label", "Main", 0), label, 28.dp)
                }
            }
        }
    }
}

@Preview
@Composable
private fun GlyphsPreview() {
    InBothPalettes {
        val colors = LocalMushinColors.current
        Column {
            Row {
                Glyph.entries.forEach { glyph ->
                    GlyphButton(glyph, glyph.name, colors.iconTint, glyph.name, onClick = {})
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SwipeMarks(SwipeDirection.entries, MushinType.resultSize, colors.textSecondary, shortcut = true)
                VerticalDivider(Modifier.padding(horizontal = 8.dp).height(18.dp))
                RadioMark(selected = true)
                RadioMark(selected = false, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Preview
@Composable
private fun VectorsPreview() {
    InBothPalettes {
        val stroke = MushinStroke.Default
        val color = stroke.color(LocalMushinColors.current)
        Canvas(Modifier.size(160.dp, 96.dp)) {
            val vertices = listOf(
                ShapeVertex(0.1f * size.width, 0.8f * size.height),
                ShapeVertex(0.35f * size.width, 0.2f * size.height, dwellMs = VectorDots.DWELL_MARK_MS),
                ShapeVertex(0.6f * size.width, 0.7f * size.height),
                ShapeVertex(0.9f * size.width, 0.3f * size.height),
            )
            drawVectors(vertices, color, stroke.width.toPx(), stroke.dots)
        }
    }
}
