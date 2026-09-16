package net.helcel.fidelity.tools

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import net.helcel.fidelity.activity.ToastHelper
import net.helcel.fidelity.pluginSDK.KeepassDef
import net.helcel.fidelity.pluginSDK.Kp2aControl
import net.helcel.fidelity.pluginSDK.Strings
import org.json.JSONArray
import org.json.JSONException
import java.util.UUID

/**
 * Bridge between [FidelityEntry] and the Keepass2Android plugin interface.
 *
 * Entries live in the KP2A database; this app only keeps a local cache of the
 * non-protected ones (see [FidelityRepository.cacheEntry]).
 */
object Kp2a {
    private const val CODE_FIELD = FidelityKeepassFields.FIDELITYCODE
    private const val FORMAT_FIELD = FidelityKeepassFields.FIDELITYFORMAT

    // Legacy flag written by pre-1.3 versions: "true" means "do not cache locally".
    private const val PROTECT_CODE_FIELD = "FidelityProtectedCode"

    private const val NOT_INSTALLED = "Keepass2Android Not Installed"

    /** URL that marks a database entry as belonging to this app. */
    fun appUrl(context: Context): String = "androidapp://" + context.packageName

    fun isAvailable(context: Context): Boolean =
        Kp2aControl.getQueryEntryForOwnPackageIntent()
            .resolveActivity(context.packageManager) != null

    /** Asks KP2A to pick one of the entries whose URL points at this app. */
    fun launchQuery(context: Context, launcher: ActivityResultLauncher<Intent>): Boolean {
        return try {
            launcher.launch(Kp2aControl.getQueryEntryForOwnPackageIntent())
            true
        } catch (_: ActivityNotFoundException) {
            ToastHelper.show(context, NOT_INSTALLED)
            false
        }
    }

    /**
     * Opens KP2A on a pre-filled "new entry" form. KP2A runs in its own task, so no
     * activity result comes back: the caller must cache the entry optimistically.
     */
    fun launchAdd(context: Context, entry: FidelityEntry): Boolean {
        val fields = HashMap<String, String>()
        fields[KeepassDef.TitleField] = entry.title
        fields[KeepassDef.UrlField] = appUrl(context)
        fields[CODE_FIELD] = entry.code
        fields[FORMAT_FIELD] = entry.format
        fields[PROTECT_CODE_FIELD] = entry.protected.toString()
        val protectedFields = if (entry.protected) arrayListOf(CODE_FIELD) else null

        return try {
            context.startActivity(Kp2aControl.getAddEntryIntent(fields, protectedFields))
            true
        } catch (_: ActivityNotFoundException) {
            ToastHelper.show(context, NOT_INSTALLED)
            false
        }
    }

    /** Stable id for entries KP2A did not tag with its own UUID. */
    fun localUid(title: String): String =
        UUID.nameUUIDFromBytes("kp2a:$title".toByteArray(Charsets.UTF_8)).toString()

    /**
     * Extracts a fidelity entry from an intent produced by KP2A (query result, or the
     * launch intent when the user opens this app from an entry). Returns null when the
     * intent carries no fidelity data.
     */
    fun entryFromIntent(intent: Intent?): FidelityEntry? {
        if (intent?.hasExtra(Strings.EXTRA_ENTRY_OUTPUT_DATA) != true) return null
        val fields = Kp2aControl.getEntryFieldsFromIntent(intent)
        val code = fields[CODE_FIELD] ?: return null
        val format = fields[FORMAT_FIELD] ?: return null
        val title = fields[KeepassDef.TitleField] ?: ""

        val protected = fields[PROTECT_CODE_FIELD]?.toBooleanStrictOrNull()
            ?: protectedFieldsFromIntent(intent).contains(CODE_FIELD)

        return FidelityEntry(
            uid = intent.getStringExtra(Strings.EXTRA_ENTRY_ID) ?: localUid(title),
            title = title,
            code = code,
            format = format,
            protected = protected,
        )
    }

    // KP2A sends the protected field names either as a string list or a JSON array.
    private fun protectedFieldsFromIntent(intent: Intent): List<String> {
        intent.getStringArrayListExtra(Strings.EXTRA_PROTECTED_FIELDS_LIST)?.let { return it }
        val json = intent.getStringExtra(Strings.EXTRA_PROTECTED_FIELDS_LIST) ?: return emptyList()
        return try {
            val a = JSONArray(json)
            List(a.length()) { a.optString(it) }
        } catch (_: JSONException) {
            emptyList()
        }
    }
}
