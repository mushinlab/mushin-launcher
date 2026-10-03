package com.bluecubelabs.env.feature.search

import android.view.KeyEvent

sealed interface HardwareKey {
    data class Text(val char: Char) : HardwareKey

    data object Backspace : HardwareKey

    data object Enter : HardwareKey

    data object Escape : HardwareKey

    data object Up : HardwareKey

    data object Down : HardwareKey

    data object SwitchLayout : HardwareKey
}

object KeyboardRouter {
    fun map(keyCode: Int, unicodeChar: Int, ctrl: Boolean): HardwareKey? = when {
        ctrl && keyCode == KeyEvent.KEYCODE_SPACE -> HardwareKey.SwitchLayout

        ctrl -> null

        keyCode == KeyEvent.KEYCODE_DEL -> HardwareKey.Backspace

        keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER -> HardwareKey.Enter

        keyCode == KeyEvent.KEYCODE_ESCAPE -> HardwareKey.Escape

        keyCode == KeyEvent.KEYCODE_DPAD_UP -> HardwareKey.Up

        keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> HardwareKey.Down

        unicodeChar > 0 && Character.isLetterOrDigit(
            unicodeChar,
        ) -> HardwareKey.Text(unicodeChar.toChar().lowercaseChar())

        else -> null
    }
}
