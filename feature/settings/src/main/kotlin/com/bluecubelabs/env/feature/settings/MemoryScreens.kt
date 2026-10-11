package com.bluecubelabs.env.feature.settings

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
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.SingleLineText
import com.bluecubelabs.env.core.ui.pressable

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
        state.envs.firstOrNull { it.current }?.let { env ->
            CardGap()
            SectionHeader(stringResource(R.string.memory_usefulness, env.name))
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
    key: AppKey,
    state: SettingsUiState,
    actions: SettingsActions,
    stack: SettingsStack,
    back: () -> Unit,
) {
    val app = state.learned.firstOrNull { it.key == key }
    val label =
        app?.label ?: state.apps.firstOrNull { it.key == key }?.label ?: stringResource(R.string.settings_memory)
    var confirmAll by remember { mutableStateOf(false) }
    var drawing by remember { mutableStateOf(false) }
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
            onAdd = { path ->
                actions.addDrawing(key, path)
                drawing = false
            },
            onClose = { drawing = false },
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
    val colors = LocalEnvColors.current
    val locale = LocalConfiguration.current.locales[0]
    Column(
        Modifier.settingsCard().padding(horizontal = CARD_PADDING, vertical = 10.dp).testTag("memory_drawing_$index"),
    ) {
        GestureThumbnail(drawing.path, Modifier.fillMaxWidth().height(THUMBNAIL_HEIGHT))
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SingleLineText(
                stringResource(R.string.memory_weight, String.format(locale, "%.1f", drawing.weight)),
                EnvType.style(14.sp, colors.textSecondary),
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(CARD_RADIUS))
                    .pressable(onClick = onForget)
                    .testTag("memory_forget_$index")
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                BasicText(stringResource(R.string.memory_forget), style = EnvType.style(15.sp, colors.accent))
            }
        }
    }
}

@Composable
internal fun GestureThumbnail(path: String, modifier: Modifier) {
    val colors = LocalEnvColors.current
    val measurer = rememberTextMeasurer()
    val style = EnvType.style(11.sp, colors.textSecondary.copy(alpha = LETTER_ALPHA))
    val letters = remember(measurer, style) {
        QWERTY.flatMap { it.first.toList() }.associateWith { measurer.measure(it.toString(), style) }
    }
    Canvas(modifier.clip(RoundedCornerShape(CARD_RADIUS)).background(colors.keyBackground)) {
        val centers = qwertyCenters(size)
        letters.forEach { (char, layout) ->
            val center = centers.getValue(char)
            drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
        }
        val keys = path.filter { it in centers }
        val points = simplify(keys.map { centers.getValue(it) }, SIMPLIFY_TOLERANCE * size.height / QWERTY.size)
        if (points.isEmpty()) return@Canvas
        val stroke = STROKE.toPx()
        if (points.size > 1) {
            val line = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(line, colors.accent, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawCircle(colors.accent, radius = stroke * 1.6f, center = points.first())
        val dwells = keys.zipWithNext().filter { (a, b) -> a == b }.map { centers.getValue(it.first) }
        dwells.forEach { drawCircle(colors.accent, radius = stroke * 2f, center = it, style = Stroke(stroke / 2)) }
    }
}

@Composable
private fun ValueRow(title: String, value: String, tag: String) {
    val colors = LocalEnvColors.current
    Row(
        Modifier.settingsCard().heightIn(min = 48.dp).padding(horizontal = CARD_PADDING, vertical = 10.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(title, style = EnvType.style(17.sp, colors.textPrimary), modifier = Modifier.weight(1f))
        SingleLineText(value, EnvType.style(15.sp, colors.textSecondary))
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
