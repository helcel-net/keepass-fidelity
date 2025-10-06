package net.helcel.fidelity.tools

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream
import kotlinx.serialization.json.Json
import androidx.core.content.edit
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.getBinaryDir
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.util.UUID

object FidelityKeepassFields {
    const val FIDELITYFORMAT = "FidelityFormat"
    const val FIDELITYCODE = "FidelityCode"
}

@Serializable
data class FidelityEntry(
    val uid: String? = null,
    val title: String = "",
    val code: String = "",
    val format: String = "",
    val protected: Boolean = false,

    val hidden: Boolean = false,
    val pinned: Boolean = false,
    val lastUse: Int = 0,
)

object FidelityRepository {
    private var db: Database = Database()
    private var binaryDir: File? = null
    val entries = mutableStateListOf<FidelityEntry>()
    val activeEntry = mutableStateOf(FidelityEntry())


    fun getRoot(): Group? {
        return db.rootGroup
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
                false, binaryDir!!,
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
            cred.password,
            cred.key?.let { ctx.contentResolver.openInputStream(cred.key)?.readBytes() },
            hardwareKey
        )
    }

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
                    code=code.protectedValue.stringValue,
                    format=format.protectedValue.stringValue,
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

    fun saveEntries(context: Context) {
        val prefs = context.getSharedPreferences("fidelity_prefs", Context.MODE_PRIVATE)
        prefs.edit { putString("entries", Json.encodeToString(
            ListSerializer(FidelityEntry.serializer()),
            entries
        )) }
    }

    fun loadEntries(context: Context) {
        val prefs = context.getSharedPreferences("fidelity_prefs", Context.MODE_PRIVATE)
        try {
            val json = prefs.getString("entries", null) ?: return
            val list = Json.decodeFromString(
                ListSerializer(FidelityEntry.serializer()),
                json
            )

            entries.clear()
            entries.addAll(list)
        }catch(_: Exception){
            prefs.edit{ putString("entries",Json.encodeToString(
                ListSerializer(FidelityEntry.serializer()),emptyList()))
            }
        }
    }

    fun addEntry(ctx: Context, entry: FidelityEntry) {
        val dbEntry = db.getEntryById(NodeIdUUID(UUID.fromString(entry.uid))) ?: db.createEntry()
        val dbParent = db.getGroupById(NodeIdUUID(UUID.fromString(entry.uid)))
        dbEntry?.apply {
            putExtraField(
                Field(
                    FidelityKeepassFields.FIDELITYCODE,
                    ProtectedString(entry.protected, entry.code)
                )
            )
            putExtraField(
                Field(
                    FidelityKeepassFields.FIDELITYFORMAT,
                    ProtectedString(string= entry.format)
                )
            )
            if(dbParent!=null) title = entry.title
            dbParent?.addChildEntry(dbEntry)
        }
        entries.removeIf {it.uid == entry.uid}
        entries.add(entry.copy(uid=dbEntry?.nodeId?.id.toString()))
        saveEntries(ctx)
    }
}
