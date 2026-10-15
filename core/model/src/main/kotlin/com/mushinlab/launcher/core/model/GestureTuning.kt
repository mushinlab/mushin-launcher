package com.mushinlab.launcher.core.model

/**
 * User-tunable gesture typing parameters, their defaults and allowed ranges.
 *
 * [SHIFT_PENALTY] is the cost of a drawing displaced from the keys, per key pitch of displacement.
 * [SUGGEST_MARGIN] is how far, in score units, a word may trail the best one and still be offered.
 * [STEP] is the settings slider step.
 */
object GestureTuning {
    const val SHIFT_PENALTY = 0.5f
    const val SHIFT_PENALTY_MIN = 0.1f
    const val SHIFT_PENALTY_MAX = 1.5f
    const val SUGGEST_MARGIN = 0.3f
    const val SUGGEST_MARGIN_MIN = 0f
    const val SUGGEST_MARGIN_MAX = 1f
    const val STEP = 0.05f
}
