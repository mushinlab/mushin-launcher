package com.mushinlab.launcher.feature.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.search.KeyAction
import com.mushinlab.launcher.core.search.KeySpec
import com.mushinlab.launcher.core.search.KeyboardLayout
import com.mushinlab.launcher.core.search.SearchEngine
import com.mushinlab.launcher.core.search.SearchIndex
import com.mushinlab.launcher.core.search.SearchItem
import com.mushinlab.launcher.core.search.SearchOptions
import com.mushinlab.launcher.core.ui.AppIcon
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class PickerEntry(val key: AppKey, val label: String, val checked: Boolean)

/**
 * What an app picker shows: [entries] in their resting order, [note] under the title (for example how many are
 * selected; [noteAlert] colors it), and [full] disables entries that are not checked.
 */
@Immutable
data class AppPickerState(
    val title: String,
    val entries: ImmutableList<PickerEntry>,
    val note: String? = null,
    val noteAlert: Boolean = false,
    val full: Boolean = false,
)

interface AppPickerActions {
    fun onToggle(key: AppKey)

    fun onSwitchLayout()

    fun onBack()
}

@Stable
class PickerQuery {
    var text by mutableStateOf("")
        private set

    fun type(c: Char): Boolean {
        val accepted = text.length < SearchController.MAX_QUERY && !(c == ' ' && (text.isEmpty() || text.last() == ' '))
        if (accepted) text += c
        return accepted
    }

    fun backspace(): Boolean {
        if (text.isEmpty()) return false
        text = text.dropLast(1)
        return true
    }

    fun set(value: String) {
        text = value.take(SearchController.MAX_QUERY)
    }
}

/** [entries] matching [query], best first; all of them in their own order when the query is blank. */
fun pickerResults(entries: List<PickerEntry>, query: String, engine: SearchEngine): List<PickerEntry> {
    if (query.isBlank()) return entries
    val byKey = entries.associateBy { it.key }
    return engine.search(pickerIndex(entries), query, limit = entries.size).mapNotNull { byKey[it.key] }
}

private fun pickerIndex(entries: List<PickerEntry>): SearchIndex =
    SearchIndex.build(entries.map { SearchItem(it.key, it.label) })

@Composable
fun AppPickerScreen(state: AppPickerState, chrome: SearchChrome, actions: AppPickerActions) {
    val colors = LocalMushinColors.current
    val query = remember { PickerQuery() }
    val engine = remember(chrome.fuzzy, chrome.layoutSwap, chrome.translit) {
        SearchEngine(
            SearchOptions(
                fuzzy = chrome.fuzzy,
                layoutSwap = chrome.layoutSwap,
                translit = chrome.translit,
            ),
        )
    }
    val results = remember(state.entries, query.text, engine) {
        pickerResults(state.entries, query.text, engine).toImmutableList()
    }
    val pane = Modifier.clip(RoundedCornerShape(SearchDimens.paneRadius)).background(colors.surface)
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = SearchDimens.paneGap)
            .testTag("app_picker"),
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        PickerHeader(state, actions::onBack)
        Spacer(Modifier.height(SearchDimens.headerGap))
        PickerList(results, state.full, actions, Modifier.weight(1f).fillMaxWidth().then(pane))
        Spacer(Modifier.height(SearchDimens.paneGap))
        PickerInput(query, state.entries, chrome, actions)
        Spacer(Modifier.height(KeyboardDimens.bottomLift))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun PickerHeader(state: AppPickerState, onBack: () -> Unit) {
    val colors = LocalMushinColors.current
    Row(Modifier.fillMaxWidth().height(PICKER_HEADER), verticalAlignment = Alignment.CenterVertically) {
        GlyphButton(Glyph.BACK, stringResource(R.string.picker_back), colors.textPrimary, tag = "picker_back") {
            onBack()
        }
        Spacer(Modifier.width(8.dp))
        Column {
            SingleLineText(state.title, MushinType.style(20.sp, colors.textPrimary, FontWeight.Medium))
            state.note?.let { note ->
                SingleLineText(
                    note,
                    MushinType.style(14.sp, if (state.noteAlert) colors.accent else colors.textSecondary),
                    Modifier.testTag("picker_note"),
                )
            }
        }
    }
}

@Composable
private fun PickerList(
    results: ImmutableList<PickerEntry>,
    full: Boolean,
    actions: AppPickerActions,
    modifier: Modifier,
) {
    val colors = LocalMushinColors.current
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        if (results.isEmpty()) {
            BasicText(
                stringResource(R.string.search_nothing_found),
                style = MushinType.style(MushinType.secondarySize, colors.textSecondary),
                modifier = Modifier.padding(SearchDimens.padding),
            )
        }
        LazyColumn(Modifier.fillMaxSize().testTag("picker_list"), reverseLayout = true) {
            itemsIndexed(results, key = { _, entry -> entry.key.id }) { index, entry ->
                val enabled = entry.checked || !full
                PickerItem(entry, index, enabled) { actions.onToggle(entry.key) }
            }
        }
    }
}

@Composable
private fun PickerItem(entry: PickerEntry, index: Int, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalMushinColors.current
    val background = when {
        entry.checked -> colors.accent.copy(alpha = CHECKED_ALPHA)
        index % 2 == 1 -> colors.textPrimary.copy(alpha = PANE_ZEBRA_ALPHA)
        else -> Color.Transparent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(SearchDimens.resultHeight)
            .background(background)
            .pressable(enabled = enabled, onClick = onClick)
            .semantics { selected = entry.checked }
            .testTag("pick_${entry.label}")
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = SearchDimens.padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(entry.key, entry.label, SearchDimens.resultIcon)
        Spacer(Modifier.width(SearchDimens.padding))
        SingleLineText(
            entry.label,
            MushinType.style(MushinType.resultSize, colors.textPrimary),
            Modifier.weight(1f),
        )
        if (entry.checked) {
            Image(
                painterResource(Glyph.CHECK.res),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.accent),
                modifier = Modifier.size(CHECK_SIZE),
            )
        }
    }
}

@Composable
private fun PickerInput(
    query: PickerQuery,
    entries: ImmutableList<PickerEntry>,
    chrome: SearchChrome,
    actions: AppPickerActions,
) {
    val colors = LocalMushinColors.current
    val keys = remember(query, actions) { pickerKeys(query, actions) }
    val glide = remember(entries, chrome.gestureShiftPenalty, chrome.gestureSuggestMargin) {
        val vocabulary = pickerIndex(entries).gestureVocabulary
        Glide(chrome.gestureShiftPenalty, chrome.gestureSuggestMargin) { decoder, path, _ ->
            val word = decoder.decode(path, vocabulary)?.words?.firstOrNull()
            word?.let { query.set(it.text) }
            word != null
        }
    }
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth(chrome.keyboardWidth)
                .align(BiasAlignment(chrome.keyboardShift, 0f))
                .background(colors.surface, RoundedCornerShape(SearchDimens.paneRadius))
                .testTag("picker_input")
                .padding(top = SearchDimens.paneTop),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(SearchDimens.queryHeight)
                    .padding(start = SearchDimens.padding, end = KeyboardDimens.gap / 2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        query.text,
                        style = MushinType.style(SearchDimens.queryTextSize, colors.textPrimary),
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false).testTag("picker_query"),
                    )
                    BasicText(
                        "▏",
                        style = MushinType.style(SearchDimens.queryTextSize, colors.accent, FontWeight.Light),
                    )
                }
                if (query.text.isNotEmpty()) DeleteButton(drawn = false, chrome.haptics, keys)
            }
            Keyboard(
                remember(chrome.layout) { chrome.layout.withoutResize() },
                chrome.nextLayoutLabel,
                chrome.digitHints,
                chrome.haptics,
                keys,
                glide,
                clearTrail = query.text.isEmpty(),
            )
        }
    }
}

private fun KeyboardLayout.withoutResize() = copy(
    rows = rows.map { row -> row.map { if (it.action == KeyAction.RESIZE) KeySpec(weight = it.weight) else it } },
)

private fun pickerKeys(query: PickerQuery, actions: AppPickerActions) = object : KeyboardActions {
    override fun onChar(c: Char): Boolean = query.type(c)

    override fun onBackspace(): Boolean = query.backspace()

    override fun onClear() = query.set("")

    override fun onSwitchLayout() = actions.onSwitchLayout()

    override fun onClose() = actions.onBack()

    override fun onResize() = Unit
}

private val PICKER_HEADER = 56.dp
private val CHECK_SIZE = 20.dp
private const val CHECKED_ALPHA = 0.2f
private const val DISABLED_ALPHA = 0.4f
