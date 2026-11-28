package com.mushinlab.launcher.feature.search

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

internal fun matchHighlight(label: String, query: String): AnnotatedString {
    val needle = query.trim()
    val start = if (needle.isEmpty()) -1 else label.indexOf(needle, ignoreCase = true)
    if (start < 0) return AnnotatedString(label)
    val end = start + needle.length
    return buildAnnotatedString {
        append(label.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(label.substring(start, end)) }
        append(label.substring(end))
    }
}
