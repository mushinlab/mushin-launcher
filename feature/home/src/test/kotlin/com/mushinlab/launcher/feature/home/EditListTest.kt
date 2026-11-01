package com.mushinlab.launcher.feature.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EditListTest {
    @Test
    fun shortListFitsWithoutScrolling() {
        assertThat(EditList.viewportRows(0)).isEqualTo(0f)
        assertThat(EditList.viewportRows(5)).isEqualTo(5f)
    }

    @Test
    fun longListShowsFiveRowsAndHalfOfTheNextToHintAtScrolling() {
        assertThat(EditList.viewportRows(6)).isEqualTo(5.5f)
        assertThat(EditList.viewportRows(20)).isEqualTo(5.5f)
    }
}
