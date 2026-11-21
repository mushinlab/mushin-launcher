package com.mushinlab.launcher

import android.content.res.Resources
import com.mushinlab.launcher.core.data.R as CoreR
import com.mushinlab.launcher.core.model.PalettePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrivateSpaceController(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val resources: Resources,
    data: StateFlow<LauncherData?>,
    navigator: LauncherNavigator,
) {
    private val gateFlow = MutableStateFlow<PrivateGate?>(null)
    val gate: StateFlow<PrivateGate?> = gateFlow.asStateFlow()
    private val session = PrivateSession(container, scope, data, navigator)

    init {
        scope.launch { ensurePrivate() }
    }

    fun request() {
        if (gateFlow.value != null) return
        val settings = container.settings.settings.value ?: return
        val mode = if (settings.privatePinHash == null) PrivateGate.Mode.SET else PrivateGate.Mode.UNLOCK
        gateFlow.value = PrivateGate(mode)
    }

    fun digit(value: Int) {
        val gate = gateFlow.value ?: return
        if (gate.digits.size >= PrivateGate.LENGTH) return
        val next = gate.copy(digits = gate.digits + value, error = false)
        if (next.digits.size < PrivateGate.LENGTH) {
            gateFlow.value = next
            return
        }
        when (next.mode) {
            PrivateGate.Mode.SET -> gateFlow.value = next.copy(
                mode = PrivateGate.Mode.CONFIRM,
                digits = emptyList(),
                first = next.pin,
            )

            PrivateGate.Mode.CONFIRM -> scope.launch { confirm(next) }

            PrivateGate.Mode.UNLOCK -> scope.launch { unlock(next) }
        }
    }

    fun backspace() {
        val gate = gateFlow.value ?: return
        if (gate.digits.isEmpty()) return
        gateFlow.value = gate.copy(digits = gate.digits.dropLast(1), error = false)
    }

    fun cancel() {
        gateFlow.value = null
    }

    fun touched() = session.touched()

    fun stop() = session.stop()

    private suspend fun ensurePrivate() {
        container.privateSpaces.ensure(
            resources.getString(CoreR.string.space_default_private),
            PalettePreset.NIGHT,
        )
    }

    private suspend fun confirm(gate: PrivateGate) {
        if (gate.first != gate.pin) {
            gateFlow.value = PrivateGate(PrivateGate.Mode.SET, error = true)
            return
        }
        val (salt, hash) = withContext(Dispatchers.IO) { PrivatePin.create(gate.pin) }
        container.settings.update { it.copy(privatePinSalt = salt, privatePinHash = hash) }
        gateFlow.value = null
        session.enter()
    }

    private suspend fun unlock(gate: PrivateGate) {
        val settings = container.settings.settings.value ?: return
        val salt = settings.privatePinSalt ?: return
        val hash = settings.privatePinHash ?: return
        val matches = withContext(Dispatchers.IO) { PrivatePin.matches(gate.pin, salt, hash) }
        if (matches) {
            gateFlow.value = null
            session.enter()
        } else {
            gateFlow.value = gate.copy(digits = emptyList(), error = true)
        }
    }
}
