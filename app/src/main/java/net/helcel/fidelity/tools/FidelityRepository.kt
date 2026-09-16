package net.helcel.fidelity.tools

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The on-device list of cards shown by the launcher, persisted in shared preferences.
 *
 * In standalone mode it mirrors the cards found in the KDBX file (see
 * [KeepassDatabase.importDB]); in Keepass2Android mode it is a cache of the entries the
 * user has fetched so far.
 */
object FidelityRepository {
    val entries = mutableStateListOf<FidelityEntry>()

    /** Card being edited on the create screen. */
    val activeEntry = mutableStateOf(FidelityEntry())

    /** Entry handed over by Keepass2Android for viewing without being cached. */
    val transientEntry = mutableStateOf<FidelityEntry?>(null)

    // Each mode keeps its own cache: standalone uids are KDBX UUIDs, KP2A ones are not.
    private fun entriesKey() = if (AppModeStore.isKp2a) "entries_kp2a" else "entries"

    fun saveEntries(context: Context) {
        val prefs = context.getSharedPreferences(FIDELITY_PREFS, Context.MODE_PRIVATE)
        // In KP2A mode "protected" means the code must never leave the KP2A database.
        val persisted = if (AppModeStore.isKp2a) entries.filter { !it.protected } else entries
        prefs.edit { putString(entriesKey(), Json.encodeToString(
            ListSerializer(FidelityEntry.serializer()),
            persisted
        )) }
    }

    fun loadEntries(context: Context) {
        val prefs = context.getSharedPreferences(FIDELITY_PREFS, Context.MODE_PRIVATE)
        entries.clear()
        try {
            val json = prefs.getString(entriesKey(), null) ?: return
            val list = Json.decodeFromString(
                ListSerializer(FidelityEntry.serializer()),
                json
            )
            entries.addAll(list)
        }catch(_: Exception){
            prefs.edit{ putString(entriesKey(),Json.encodeToString(
                ListSerializer(FidelityEntry.serializer()),emptyList()))
            }
        }
    }

    /**
     * Upserts an entry coming from Keepass2Android into the local cache, keeping the
     * per-device flags (pin/hide/last use) of the entry it replaces. Matching also
     * falls back to title+code because an optimistically cached entry gets a local uid
     * until KP2A returns it with its own.
     */
    fun cacheEntry(context: Context, entry: FidelityEntry) {
        val idx = entries.indexOfFirst {
            it.uid == entry.uid || (it.title == entry.title && it.code == entry.code)
        }
        if (idx >= 0) {
            val old = entries[idx]
            entries[idx] = entry.copy(pinned = old.pinned, hidden = old.hidden, lastUse = old.lastUse)
        } else {
            entries.add(entry)
        }
        saveEntries(context)
    }

    fun removeEntry(context: Context, entry: FidelityEntry) {
        entries.removeIf { it.uid == entry.uid }
        saveEntries(context)
    }

    fun findEntry(uid: String?): FidelityEntry? =
        entries.find { it.uid == uid } ?: transientEntry.value?.takeIf { it.uid == uid }
}
