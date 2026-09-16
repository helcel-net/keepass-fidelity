package net.helcel.fidelity.tools

import kotlinx.serialization.Serializable

/** Names of the custom KeePass fields a card is stored in. */
object FidelityKeepassFields {
    const val FIDELITYFORMAT = "FidelityFormat"
    const val FIDELITYCODE = "FidelityCode"
}

/**
 * A loyalty card as shown by the app.
 *
 * [uid] is the KeePass entry UUID in standalone mode, or the id Keepass2Android reports
 * (falling back to a title-derived one) in plugin mode. The last three fields are
 * per-device preferences that never reach the database.
 */
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
