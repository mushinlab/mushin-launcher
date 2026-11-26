package com.mushinlab.launcher.core.data

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.repo.PrivateSpaceRepository
import com.mushinlab.launcher.core.data.repo.SpaceLookRepository
import com.mushinlab.launcher.core.data.repo.SpaceOutcome
import com.mushinlab.launcher.core.data.repo.SpaceRepository
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.SpaceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpaceLookRepositoryTest {
    @get:Rule
    val rule = DbTestRule()

    @Test
    fun backgroundLookIsSetPerSpace() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val looks = SpaceLookRepository(rule.db)
        val all = spaces.spaces.first { it.size >= 2 }
        val (normal, work) = all.first { it.type == SpaceType.NORMAL } to all.first { it.type == SpaceType.WORK }
        assertThat(all.all { it.backgroundBrightness == 0f && it.backgroundBlur == 0f }).isTrue()

        looks.setBackgroundLook(work.id, brightness = -0.4f, blur = 0.25f)

        val updated = spaces.spaces.first { list -> list.any { it.id == work.id && it.backgroundBlur == 0.25f } }
        assertThat(updated.first { it.id == work.id }.backgroundBrightness).isEqualTo(-0.4f)
        assertThat(updated.first { it.id == normal.id }.backgroundBrightness).isEqualTo(0f)
        assertThat(updated.first { it.id == normal.id }.backgroundBlur).isEqualTo(0f)
    }

    @Test
    fun outOfRangeValuesAreClampedAndNanIsNeutral() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val looks = SpaceLookRepository(rule.db)
        val normal = spaces.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }

        looks.setBackgroundLook(normal.id, brightness = 7f, blur = -2f)
        val clamped = spaces.spaces.first { list -> list.any { it.id == normal.id && it.backgroundBrightness == 1f } }
        assertThat(clamped.first { it.id == normal.id }.backgroundBlur).isEqualTo(0f)

        looks.setBackgroundLook(normal.id, brightness = Float.NaN, blur = 3f)
        val nan = spaces.spaces.first { list -> list.any { it.id == normal.id && it.backgroundBlur == 1f } }
        assertThat(nan.first { it.id == normal.id }.backgroundBrightness).isEqualTo(0f)
    }

    @Test
    fun rowOpacityIsStoredPerSpaceAndDefaultsTo90Percent() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val looks = SpaceLookRepository(rule.db)
        val all = spaces.spaces.first { it.size >= 2 }
        val (normal, work) = all.first { it.type == SpaceType.NORMAL } to all.first { it.type == SpaceType.WORK }
        assertThat(all.all { it.backgroundRowAlpha == 0.9f }).isTrue()

        looks.setBackgroundLook(work.id, brightness = 0.2f, blur = 0.1f, rowAlpha = 0.45f)

        val updated = spaces.spaces.first { list -> list.any { it.id == work.id && it.backgroundRowAlpha == 0.45f } }
        assertThat(updated.first { it.id == work.id }.backgroundBrightness).isEqualTo(0.2f)
        assertThat(updated.first { it.id == work.id }.backgroundBlur).isEqualTo(0.1f)
        assertThat(updated.first { it.id == normal.id }.backgroundRowAlpha).isEqualTo(0.9f)
    }

    @Test
    fun rowOpacityIsClampedTo20To100PercentAndNanIsTheDefault() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val looks = SpaceLookRepository(rule.db)
        val normal = spaces.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }

        looks.setBackgroundLook(normal.id, brightness = 0f, blur = 0f, rowAlpha = 0.05f)
        spaces.spaces.first { list -> list.any { it.id == normal.id && it.backgroundRowAlpha == 0.2f } }

        looks.setBackgroundLook(normal.id, brightness = 0f, blur = 0f, rowAlpha = 3f)
        spaces.spaces.first { list -> list.any { it.id == normal.id && it.backgroundRowAlpha == 1f } }

        looks.setBackgroundLook(normal.id, brightness = 0f, blur = 0f, rowAlpha = Float.NaN)
        val nan = spaces.spaces.first { list -> list.any { it.id == normal.id && it.backgroundRowAlpha == 0.9f } }
        assertThat(nan.first { it.id == normal.id }.backgroundRowAlpha).isEqualTo(0.9f)
    }

    @Test
    fun zeroResetsTheLook() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val looks = SpaceLookRepository(rule.db)
        val normal = spaces.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        looks.setBackgroundLook(normal.id, brightness = 0.6f, blur = 0.6f, rowAlpha = 0.3f)
        spaces.spaces.first { list -> list.any { it.id == normal.id && it.backgroundBlur == 0.6f } }

        looks.setBackgroundLook(normal.id, brightness = 0f, blur = 0f)

        val reset = spaces.spaces.first { list -> list.any { it.id == normal.id && it.backgroundBlur == 0f } }
        assertThat(reset.first { it.id == normal.id }.backgroundBrightness).isEqualTo(0f)
        assertThat(reset.first { it.id == normal.id }.backgroundRowAlpha).isEqualTo(0.9f)
    }

    @Test
    fun AC_54_1_shareThemeCopiesPaletteAndColoredIconsToEveryNonPrivateSpace() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val normal = repo.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        val custom = (repo.create("Rest", SpaceType.CUSTOM, PalettePreset.MOSS) as SpaceOutcome.Ok).value
        val private = PrivateSpaceRepository(rule.db, rule.clock).ensure("Private", PalettePreset.NIGHT)
        repo.setPalette(normal.id, PalettePreset.PAPER)
        repo.setColoredIcons(normal.id, false)
        SpaceLookRepository(rule.db).shareTheme(normal.id)
        val spaces = repo.spaces.first { list -> list.count { it.preset == PalettePreset.PAPER } == 3 }
        spaces.filter { it.id != private }.forEach {
            assertThat(it.palette).isEqualTo(Palettes.Paper)
            assertThat(it.coloredIcons).isFalse()
        }
        assertThat(spaces.first { it.id == custom }.preset).isEqualTo(PalettePreset.PAPER)
        assertThat(spaces.first { it.id == private }.preset).isEqualTo(PalettePreset.NIGHT)
    }

    @Test
    fun AC_54_1_shareThemeFromThePrivateSpaceChangesNothing() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val private = PrivateSpaceRepository(rule.db, rule.clock).ensure("Private", PalettePreset.NIGHT)
        SpaceLookRepository(rule.db).shareTheme(private)
        val spaces = repo.spaces.first { list -> list.any { it.id == private } }
        assertThat(spaces.map { it.preset }).containsExactly(
            PalettePreset.GRAPHITE,
            PalettePreset.NAVY,
            PalettePreset.NIGHT,
        ).inOrder()
    }
}
