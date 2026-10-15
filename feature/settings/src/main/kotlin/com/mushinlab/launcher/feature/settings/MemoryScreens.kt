package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.ShapeVertex
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.LocalMushinStroke
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.SpaceType
import com.mushinlab.launcher.core.ui.drawVectors
import com.mushinlab.launcher.core.ui.pressable

@Composable
internal fun MemoryScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    var confirm by remember { mutableStateOf<(() -> Unit)?>(null) }
    var message by remember { mutableStateOf("") }
    val forgetGestures = stringResource(R.string.memory_forget_gestures_confirm)
    val clearUsage = stringResource(R.string.memory_clear_usage_confirm)
    SettingsPage(stringResource(R.string.settings_memory), back) {
        NoteText(stringResource(R.string.memory_note))
        SectionHeader(stringResource(R.string.memory_gestures))
        NavRow(stringResource(R.string.memory_add_gesture), tag = "memory_add_gesture") {
            push(SettingsRoute.MemoryPickApp)
        }
        if (state.learned.isEmpty()) NoteText(stringResource(R.string.memory_gestures_none))
        state.learned.forEach { app ->
            NavRow(
                app.label,
                value = pluralStringResource(R.plurals.memory_drawings, app.drawings.size, app.drawings.size),
                tag = "memory_app_${app.label}",
            ) { push(SettingsRoute.MemoryApp(app.key)) }
        }
        if (state.learned.isNotEmpty()) {
            NavRow(stringResource(R.string.memory_forget_gestures), tag = "memory_forget_gestures") {
                message = forgetGestures
                confirm = actions::forgetAllGestures
            }
        }
        CardGap()
        SectionHeader(stringResource(R.string.memory_usage))
        NoteText(stringResource(R.string.memory_usage_note))
        if (state.usage.isEmpty()) NoteText(stringResource(R.string.memory_usage_none))
        state.usage.forEach { row -> ValueRow(row.label, row.launches.toString(), "memory_usage_${row.label}") }
        if (state.usage.isNotEmpty()) {
            NavRow(stringResource(R.string.memory_clear_usage), tag = "memory_clear_usage") {
                message = clearUsage
                confirm = actions::clearUsage
            }
        }
        state.spaces.firstOrNull { it.current }?.let { space ->
            CardGap()
            SectionHeader(stringResource(R.string.memory_usefulness, space.name))
            NoteText(stringResource(R.string.memory_usefulness_note))
            if (state.usefulness.isEmpty()) NoteText(stringResource(R.string.memory_usage_none))
            state.usefulness.forEach { row ->
                ValueRow(row.label, row.score.toString(), "memory_score_${row.label}")
            }
        }
    }
    confirm?.let { action ->
        ConfirmDialog(
            message = message,
            confirm = stringResource(R.string.memory_erase),
            onConfirm = {
                action()
                confirm = null
            },
            onDismiss = { confirm = null },
        )
    }
}

@Composable
internal fun MemoryAppScreen(
    route: SettingsRoute.MemoryApp,
    state: SettingsUiState,
    actions: SettingsActions,
    stack: SettingsStack,
    back: () -> Unit,
) {
    val key = route.key
    val app = state.learned.firstOrNull { it.key == key }
    val label =
        app?.label ?: state.apps.firstOrNull { it.key == key }?.label ?: stringResource(R.string.settings_memory)
    var confirmAll by remember { mutableStateOf(false) }
    var drawing by remember { mutableStateOf(route.sketch) }
    val draft = remember { stack.draft.takeIf { route.sketch } }
    SettingsPage(label, back) {
        NoteText(stringResource(R.string.memory_app_note))
        NavRow(stringResource(R.string.memory_add), tag = "memory_add") { drawing = true }
        app?.drawings?.forEachIndexed { index, drawing ->
            DrawingRow(drawing, index) { actions.forgetDrawing(key, drawing.path) }
        }
        if (app != null) {
            NavRow(stringResource(R.string.memory_forget_app), tag = "memory_forget_app") { confirmAll = true }
        }
    }
    if (drawing) {
        GestureSheet(
            label = label,
            stack = stack,
            draft = draft,
            onAdd = { sample ->
                actions.addDrawing(key, sample)
                stack.draft = null
                drawing = false
            },
            onClose = {
                stack.draft = null
                drawing = false
            },
        )
    }
    if (confirmAll && app != null) {
        ConfirmDialog(
            message = stringResource(R.string.memory_forget_app_confirm, app.label),
            confirm = stringResource(R.string.memory_erase),
            onConfirm = {
                actions.forgetApp(key)
                confirmAll = false
                back()
            },
            onDismiss = { confirmAll = false },
        )
    }
}

@Composable
private fun DrawingRow(drawing: LearnedDrawing, index: Int, onForget: () -> Unit) {
    val colors = LocalMushinColors.current
    val locale = LocalConfiguration.current.locales[0]
    Column(
        Modifier.settingsCard().padding(horizontal = CARD_PADDING, vertical = 10.dp).testTag("memory_drawing_$index"),
    ) {
        GestureThumbnail(drawing, Modifier.fillMaxWidth().height(THUMBNAIL_HEIGHT))
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SingleLineText(
                stringResource(R.string.memory_weight, String.format(locale, "%.1f", drawing.weight)),
                SpaceType.style(14.sp, colors.textSecondary),
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(CARD_RADIUS))
                    .pressable(onClick = onForget)
                    .testTag("memory_forget_$index")
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                BasicText(stringResource(R.string.memory_forget), style = SpaceType.style(15.sp, colors.accent))
            }
        }
    }
}

@Composable
internal fun GestureThumbnail(drawing: LearnedDrawing, modifier: Modifier) {
    val colors = LocalMushinColors.current
    val stroke = LocalMushinStroke.current
    val measurer = rememberTextMeasurer()
    val style = SpaceType.style(11.sp, colors.textSecondary.copy(alpha = LETTER_ALPHA))
    val letters = remember(measurer, style) {
        QWERTY.flatMap { it.first.toList() }.associateWith { measurer.measure(it.toString(), style) }
    }
    Canvas(modifier.clip(RoundedCornerShape(CARD_RADIUS)).background(colors.keyBackground)) {
        val centers = qwertyCenters(size)
        letters.forEach { (char, layout) ->
            val center = centers.getValue(char)
            drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
        }
        drawVectors(thumbnailVectors(drawing, size, centers), stroke.color(colors), STROKE.toPx(), stroke.dots)
    }
}

private fun thumbnailVectors(drawing: LearnedDrawing, size: Size, centers: Map<Char, Offset>): List<ShapeVertex> {
    if (drawing.shape.size >= 2) {
        return drawing.shape.map { it.copy(x = it.x * size.width, y = it.y * size.height) }
    }
    val keys = drawing.path.filter { it in centers }
    val doubles = keys.zipWithNext().filter { (a, b) -> a == b }.map { centers.getValue(it.first) }.toSet()
    return simplify(keys.map { centers.getValue(it) }, SIMPLIFY_TOLERANCE * size.height / QWERTY.size)
        .map { ShapeVertex(it.x, it.y, if (it in doubles) DOUBLE_LETTER_MS else 0) }
}

@Composable
private fun ValueRow(title: String, value: String, tag: String) {
    val colors = LocalMushinColors.current
    Row(
        Modifier.settingsCard().heightIn(min = 48.dp).padding(horizontal = CARD_PADDING, vertical = 10.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(title, style = SpaceType.style(17.sp, colors.textPrimary), modifier = Modifier.weight(1f))
        SingleLineText(value, SpaceType.style(15.sp, colors.textSecondary))
    }
}

internal fun simplify(points: List<Offset>, tolerance: Float): List<Offset> {
    val distinct = points.filterIndexed { i, p -> i == 0 || p != points[i - 1] }
    if (distinct.size < 3) return distinct
    val first = distinct.first()
    val last = distinct.last()
    val farthest = (1 until distinct.lastIndex).maxBy { distanceToSegment(distinct[it], first, last) }
    return if (distanceToSegment(distinct[farthest], first, last) > tolerance) {
        simplify(distinct.subList(0, farthest + 1), tolerance).dropLast(1) +
            simplify(distinct.subList(farthest, distinct.size), tolerance)
    } else {
        listOf(first, last)
    }
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val lengthSquared = ab.x * ab.x + ab.y * ab.y
    if (lengthSquared == 0f) return (p - a).getDistance()
    val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / lengthSquared).coerceIn(0f, 1f)
    return (p - (a + ab * t)).getDistance()
}

internal fun qwertyCenters(size: Size): Map<Char, Offset> {
    val keyWidth = size.width / QWERTY.first().first.length
    val rowHeight = size.height / QWERTY.size
    return buildMap {
        QWERTY.forEachIndexed { row, (letters, shift) ->
            letters.forEachIndexed { i, c -> put(c, Offset((i + shift + 0.5f) * keyWidth, (row + 0.5f) * rowHeight)) }
        }
    }
}

internal val QWERTY = listOf("qwertyuiop" to 0f, "asdfghjkl" to 0.5f, "zxcvbnm" to 1.5f)
private val THUMBNAIL_HEIGHT = 104.dp
private val STROKE = 3.dp
private const val LETTER_ALPHA = 0.6f
internal const val SIMPLIFY_TOLERANCE = 0.9f
private const val DOUBLE_LETTER_MS = 1000L
