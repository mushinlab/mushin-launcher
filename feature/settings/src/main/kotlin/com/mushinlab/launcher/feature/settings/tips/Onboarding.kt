package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.settings.R

@Composable
fun Onboarding(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    pager: OnboardingPager = rememberOnboardingPager(),
) {
    val colors = LocalMushinColors.current
    val threshold = with(LocalDensity.current) { SWIPE_DISTANCE.toPx() }
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(pager, threshold) {
                var dragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onDragEnd = { pager.swipe(dragX, threshold) },
                ) { change, amount ->
                    change.consume()
                    dragX += amount
                }
            }
            .testTag("onboarding"),
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        AnimatedContent(
            targetState = pager.page,
            transitionSpec = { slideTransition() },
            label = "onboarding",
            modifier = Modifier.weight(1f),
        ) { page -> OnboardingSlide(page) }
        OnboardingFooter(pager, onFinish)
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

private fun AnimatedContentTransitionScope<Int>.slideTransition(): ContentTransform {
    val forward = targetState > initialState
    val spec = tween<IntOffset>(Motion.TRANSITION_MS)
    return (
        slideInHorizontally(spec) { if (forward) it else -it } + fadeIn(tween(Motion.TRANSITION_MS))
        ) togetherWith (
        slideOutHorizontally(spec) { if (forward) -it else it } + fadeOut(tween(Motion.TRANSITION_MS))
        )
}

@Composable
private fun OnboardingSlide(page: Int) {
    val colors = LocalMushinColors.current
    val tip = Tip.entries.getOrNull(page)
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp).testTag("onboarding_slide_$page"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val frame = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp)
        if (tip != null) TipAnimation(tip, frame) else TipsHintAnimation(frame)
        BasicText(
            stringResource(tip?.title ?: R.string.tips_more_title),
            style = MushinType.style(24.sp, colors.textPrimary, FontWeight.Medium).copy(textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            stringResource(tip?.description ?: R.string.tips_more_text),
            style = MushinType.style(16.sp, colors.textSecondary).copy(textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OnboardingFooter(pager: OnboardingPager, onFinish: () -> Unit) {
    val colors = LocalMushinColors.current
    val dots = stringResource(R.string.onboarding_position, pager.page + 1, pager.count)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            BasicText(
                stringResource(R.string.onboarding_skip),
                style = MushinType.style(16.sp, colors.textSecondary),
                modifier = Modifier
                    .clip(RoundedCornerShape(BUTTON_RADIUS))
                    .pressable(onClick = onFinish)
                    .testTag("onboarding_skip")
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        Row(
            Modifier.semantics { contentDescription = dots }.testTag("onboarding_dots"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(pager.count) { index ->
                val color = if (index == pager.page) colors.accent else colors.textSecondary.copy(alpha = DOT_ALPHA)
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            NextButton(
                stringResource(if (pager.isLast) R.string.onboarding_done else R.string.onboarding_next),
                colors.accent,
                colors.background,
            ) { if (!pager.next()) onFinish() }
        }
    }
}

@Composable
private fun NextButton(label: String, background: Color, content: Color, onClick: () -> Unit) {
    BasicText(
        label,
        style = MushinType.style(16.sp, content, FontWeight.Medium),
        modifier = Modifier
            .clip(RoundedCornerShape(BUTTON_RADIUS))
            .background(background)
            .pressable(onClick = onClick)
            .testTag("onboarding_next")
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

private val SWIPE_DISTANCE = 56.dp
private val BUTTON_RADIUS = 24.dp
private const val DOT_ALPHA = 0.35f
