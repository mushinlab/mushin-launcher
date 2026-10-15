package com.mushinlab.launcher.core.data.db

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.GestureBinding
import com.mushinlab.launcher.core.model.ShapeVertex
import com.mushinlab.launcher.core.model.SwipeDirection
import org.junit.Test

class StorageCodecsTest {
    @Test
    fun shapeSurvivesStorage() {
        val shape = listOf(ShapeVertex(0.1f, 0.25f, 300), ShapeVertex(0.9f, 0.5f))

        assertThat(ShapeCodec.decode(ShapeCodec.encode(shape))).isEqualTo(shape)
    }

    @Test
    fun emptyOrDamagedShapesDecodeToWhatIsReadable() {
        assertThat(ShapeCodec.encode(emptyList())).isNull()
        assertThat(ShapeCodec.decode(null)).isEmpty()
        assertThat(ShapeCodec.decode("0.5,0.5,0;x,1,0;1,2")).containsExactly(ShapeVertex(0.5f, 0.5f))
    }

    @Test
    fun everyGestureActionSurvivesStorage() {
        val actions = listOf(
            GestureAction.None,
            GestureAction.Notifications,
            GestureAction.Search,
            GestureAction.AllApps,
            GestureAction.LaunchApp(AppKey("org.telegram", "org.telegram.Main", 10)),
        )

        actions.forEach { action ->
            val binding = GestureBinding(SwipeDirection.UP, action)
            assertThat(GestureCodec.toModel(GestureCodec.toEntity(binding))).isEqualTo(binding)
        }
    }

    @Test
    fun unknownDirectionIsDroppedAndBrokenAppBindingIsCleared() {
        val unknown = GestureBindingEntity("SIDEWAYS", "SEARCH", null, null, null)
        val broken = GestureBindingEntity("LEFT", "LAUNCH_APP", "org.telegram", null, null)

        assertThat(GestureCodec.toModel(unknown)).isNull()
        assertThat(GestureCodec.toModel(broken)?.action).isEqualTo(GestureAction.None)
    }
}
