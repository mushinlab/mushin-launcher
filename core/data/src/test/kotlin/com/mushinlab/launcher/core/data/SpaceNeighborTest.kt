package com.mushinlab.launcher.core.data

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.repo.neighborSpace
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.Space
import com.mushinlab.launcher.core.model.SpaceType
import org.junit.Test

class SpaceNeighborTest {
    private val normal = space(1, "Default", SpaceType.NORMAL, 0)
    private val work = space(2, "Work", SpaceType.WORK, 1)
    private val rest = space(3, "Rest", SpaceType.CUSTOM, 2)
    private val spaces = listOf(normal, work, rest)

    @Test
    fun forwardGoesToTheNextSpaceInListOrder() {
        assertThat(neighborSpace(spaces, normal.id, 1)?.id).isEqualTo(work.id)
        assertThat(neighborSpace(spaces, work.id, 1)?.id).isEqualTo(rest.id)
    }

    @Test
    fun backwardGoesToThePreviousSpaceInListOrder() {
        assertThat(neighborSpace(spaces, rest.id, -1)?.id).isEqualTo(work.id)
        assertThat(neighborSpace(spaces, work.id, -1)?.id).isEqualTo(normal.id)
    }

    @Test
    fun forwardWrapsAroundAfterTheLastSpace() {
        assertThat(neighborSpace(spaces, rest.id, 1)?.id).isEqualTo(normal.id)
    }

    @Test
    fun backwardWrapsAroundBeforeTheFirstSpace() {
        assertThat(neighborSpace(spaces, normal.id, -1)?.id).isEqualTo(rest.id)
    }

    @Test
    fun movesByMoreThanOneStep() {
        assertThat(neighborSpace(spaces, normal.id, 2)?.id).isEqualTo(rest.id)
        assertThat(neighborSpace(spaces, normal.id, -2)?.id).isEqualTo(work.id)
    }

    @Test
    fun aSingleSpaceHasNoNeighbor() {
        assertThat(neighborSpace(listOf(normal), normal.id, 1)).isNull()
        assertThat(neighborSpace(listOf(normal), normal.id, -1)).isNull()
    }

    @Test
    fun anUnknownCurrentSpaceIsResolvedToTheNormalSpace() {
        assertThat(neighborSpace(spaces, 999, 1)?.id).isEqualTo(work.id)
        assertThat(neighborSpace(spaces, null, -1)?.id).isEqualTo(rest.id)
    }

    @Test
    fun thePrivateSpaceIsSkipped() {
        val secret = space(4, "Private", SpaceType.PRIVATE, 3)
        val withPrivate = listOf(normal, work, rest, secret)
        assertThat(neighborSpace(withPrivate, rest.id, 1)?.id).isEqualTo(normal.id)
        assertThat(neighborSpace(withPrivate, normal.id, -1)?.id).isEqualTo(rest.id)
        assertThat(neighborSpace(withPrivate, secret.id, 1)).isNull()
    }

    @Test
    fun noSpacesGiveNoNeighbor() {
        assertThat(neighborSpace(emptyList(), null, 1)).isNull()
    }

    private fun space(id: Long, name: String, type: SpaceType, order: Int) = Space(
        id = id,
        name = name,
        type = type,
        sortOrder = order,
        preset = PalettePreset.GRAPHITE,
        palette = Palettes.Graphite,
        createdAt = order.toLong(),
    )
}
