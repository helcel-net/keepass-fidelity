package net.helcel.fidelity.tools

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit

const val FIDELITY_PREFS = "fidelity_prefs"

enum class AppMode {
    /** Opens a KDBX file directly through the bundled KeePassDX engine. */
    STANDALONE,

    /** Delegates storage to the Keepass2Android app through its plugin interface. */
    KP2A,
}

object AppModeStore {
    private const val KEY_MODE = "app_mode"

    /** Null until the user has picked a mode on the selection screen. */
    val mode = mutableStateOf<AppMode?>(null)

    val isKp2a: Boolean
        get() = mode.value == AppMode.KP2A

    fun load(context: Context): AppMode? {
        val prefs = context.getSharedPreferences(FIDELITY_PREFS, Context.MODE_PRIVATE)
        mode.value = prefs.getString(KEY_MODE, null)?.let { name ->
            AppMode.entries.firstOrNull { it.name == name }
        }
        return mode.value
    }

    fun set(context: Context, newMode: AppMode) {
        context.getSharedPreferences(FIDELITY_PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_MODE, newMode.name) }
        mode.value = newMode
    }
}
