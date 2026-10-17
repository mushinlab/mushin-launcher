package com.mushinlab.launcher.feature.settings

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.SpaceLimits
import com.mushinlab.launcher.core.model.SpaceType
import org.junit.Test

class CreatableTypesTest {
    private fun space(id: Long, type: SpaceType) = SpaceSummary(id, "s$id", type, PalettePreset.GRAPHITE, id == 1L)

    private val normal = space(1, SpaceType.NORMAL)

    @Test
    fun customComesFirstAndWorkIsOfferedWhileThereIsNone() {
        assertThat(creatableTypes(listOf(normal))).containsExactly(SpaceType.CUSTOM, SpaceType.WORK).inOrder()
    }

    @Test
    fun onlyOneWorkSpaceIsAllowed() {
        assertThat(creatableTypes(listOf(normal, space(2, SpaceType.WORK)))).containsExactly(SpaceType.CUSTOM)
    }

    @Test
    fun nothingIsOfferedWhenWorkExistsAndCustomLimitIsReached() {
        val custom = (1..SpaceLimits.MAX_CUSTOM).map { space(it + 10L, SpaceType.CUSTOM) }
        val spaces = listOf(normal, space(2, SpaceType.WORK)) + custom

        assertThat(creatableTypes(spaces)).isEmpty()
        assertThat(creatableTypes(spaces - space(2, SpaceType.WORK))).containsExactly(SpaceType.WORK)
    }
}
