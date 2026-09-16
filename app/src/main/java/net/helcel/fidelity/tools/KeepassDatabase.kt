package net.helcel.fidelity.tools

import android.content.Context
import android.net.Uri
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.getBinaryDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.helcel.fidelity.activity.ToastHelper
import net.helcel.fidelity.tools.FidelityRepository.entries
import net.helcel.fidelity.tools.FidelityRepository.saveEntries
import net.helcel.fidelity.tools.KeePassStore.loadCredentials
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

/**
 * Standalone mode: the KDBX file opened through the bundled KeePassDX engine.
 *
 * [unlock] and [save] are the session-level operations the screens use; the rest is the
 * raw file handling underneath them.
 */
object KeepassDatabase {
    private var db: Database = Database()
    private var binaryDir: File? = null

    /** Credentials released by the user for this process, so authentication happens once. */
    var credentials: CredentialResult.Success? = null

    fun getRoot(): Group? {
        return db.rootGroup
    }

    /** Makes sure [credentials] are available, asking the user to authenticate if needed. */
    suspend fun ensureCredentials(context: Context): Boolean {
        if (credentials != null) return true
        return when (val res = loadCredentials(context)) {
            is CredentialResult.Success -> {
                credentials = res
                true
            }
            CredentialResult.AuthFailed, CredentialResult.NoData -> {
                ToastHelper.show(context, "Unable to Load Credentials")
                false
            }
        }
    }

    /** Opens the file with [credentials] and imports its cards into [FidelityRepository]. */
    suspend fun unlock(context: Context): Boolean {
        val cred = credentials ?: return false
        val opened = withContext(Dispatchers.IO) {
            start(context, cred.db, genCredentials(context, cred))
        }
        if (!opened) {
            ToastHelper.show(context, "Unable to open the database")
            return false
        }
        importDB(context)
        return true
    }

    /** Writes the database back to its file. */
    suspend fun save(context: Context): Boolean {
        val cred = credentials ?: return false
        val saved = try {
            withContext(Dispatchers.IO) { end(context, cred.db, genCredentials(context, cred)) }
        } catch (e: Exception) {
            println(e)
            false
        }
        if (!saved) ToastHelper.show(context, "Unable to save the database")
        return saved
    }

    fun start(ctx: Context, uri: Uri?, c: MasterCredential): Boolean {
        if (binaryDir == null) binaryDir = ctx.getBinaryDir()
        if (uri == null) return false
        try {
            val bitStream =
                ByteArrayInputStream(ctx.contentResolver.openInputStream(uri)?.readBytes())
            db.loadData(
                bitStream, c,
                { hardwareKey, seed -> retrieveResponseFromChallenge(hardwareKey, seed) },
                readOnly=false, allowUserVerification = false,binaryDir!!,
                { BinaryData.canMemoryBeAllocatedInRAM(ctx, it) },
                false, null
            )
            return true
        } catch (e: Exception) {
            println(e)
            return false
        }
    }

    fun end(ctx: Context, uri: Uri?, c: MasterCredential): Boolean {
        if (uri == null) return false
        db.saveData(
            File(binaryDir, db.binaryCache.hashCode().toString()),{  ctx.contentResolver.openOutputStream(uri) },
            false, c, { hardwareKey, seed -> retrieveResponseFromChallenge(hardwareKey, seed) })
        return true
    }

    fun genCredentials(
        ctx: Context,
        cred: CredentialResult.Success,
        hardwareKey: HardwareKey? = null
    ): MasterCredential {
        return MasterCredential(
            cred.password.toCharArray(),
            cred.key?.let { ctx.contentResolver.openInputStream(cred.key)?.readBytes() },
            hardwareKey
        )
    }

    /** Replaces the card list with the cards found in the database, keeping per-device flags. */
    fun importDB(context: Context) {
        val seenID= arrayListOf<String>()
        fun importDBRec(group: Group) {
            group.getChildEntries().forEach {
                val fields = it.getExtraFields()
                val code = fields.firstOrNull { e -> e.name == FidelityKeepassFields.FIDELITYCODE }
                val format =
                    fields.firstOrNull { e -> e.name == FidelityKeepassFields.FIDELITYFORMAT }
                if (code == null || format == null) return@forEach

                val newEntry = FidelityEntry(
                    uid=it.nodeId.id.toString(),
                    title=it.title,
                    code=code.protectedValue.toString(),
                    format=format.protectedValue.toString(),
                    protected=code.protectedValue.isProtected,
                )
                val idx = entries.indexOfFirst { e -> e.uid == newEntry.uid }
                seenID.add(newEntry.uid!!)
                if (idx >= 0) {
                    val oldEntry = entries[idx]
                    entries[idx] = newEntry.copy(
                        pinned = oldEntry.pinned,
                        hidden = oldEntry.hidden,
                        lastUse = oldEntry.lastUse
                    )
                } else {
                    entries.add(newEntry)
                }
            }
            group.getChildGroups().forEach { importDBRec(it) }
        }
        if (db.rootGroup != null)
            importDBRec(db.rootGroup!!)
        entries.removeAll { !seenID.contains(it.uid)}
        val distinct = entries.distinctBy { it.uid }
        entries.clear()
        entries.addAll(distinct)
        saveEntries(context)
    }

    /**
     * Writes the card into the loaded database. [FidelityEntry.uid] is either the id of an
     * existing entry, which is updated in place, or the id of the group to create it in.
     */
    fun addEntry(ctx: Context, entry: FidelityEntry) {
        val id = NodeIdUUID(UUID.fromString(entry.uid))
        val existing = db.getEntryById(id)
        val dbEntry = existing ?: db.createEntry() ?: return
        dbEntry.apply {
            title = entry.title
            // Keepass2Android lists entries for this app by URL, so cards written here stay
            // reachable from KP2A mode as well.
            if (url.isBlank()) url = Kp2a.appUrl(ctx)
            putExtraField(
                Field(
                    FidelityKeepassFields.FIDELITYCODE,
                    ProtectedString(entry.protected, entry.code)
                )
            )
            putExtraField(
                Field(
                    FidelityKeepassFields.FIDELITYFORMAT,
                    ProtectedString(true, entry.format.toCharArray())
                )
            )
        }
        if (existing != null) db.updateEntry(dbEntry)
        else db.addEntryTo(dbEntry, db.getGroupById(id) ?: db.rootGroup ?: return)
        entries.removeIf {it.uid == entry.uid}
        entries.add(entry.copy(uid=dbEntry.nodeId.id.toString()))
        saveEntries(ctx)
    }
}
