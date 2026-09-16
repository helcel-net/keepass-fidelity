package net.helcel.fidelity.activity.fragment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node
import net.helcel.fidelity.tools.KeepassDatabase

/**
 * Browses the loaded database. With [groupsOnly] entries are hidden, so the result is the group
 * a new card is created in; otherwise an existing entry can be picked to attach the card to.
 */
@Preview
@Composable
fun TreeSelectorDialog(groupsOnly: Boolean = false, onDismiss: (Node?) -> Unit = {}) {
    Dialog(
        onDismissRequest = {onDismiss(null)},
        content = {
            Column(
                modifier = Modifier.fillMaxWidth().background(
                    MaterialTheme.colors.background,
                    RoundedCornerShape(8.dp)
                )
            ) {
                var currentRoot by remember { mutableStateOf(KeepassDatabase.getRoot()) }
                var selection by remember { mutableStateOf<Node?>(KeepassDatabase.getRoot()) }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                selection = currentRoot
                                currentRoot = currentRoot?.parent
                            },
                            enabled = currentRoot?.parent != null
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "up")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            currentRoot?.title ?: "?",
                            color = MaterialTheme.colors.onBackground,
                            style = MaterialTheme.typography.h6
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.fillMaxHeight(0.75f)) {
                        items(currentRoot?.getChildGroups() ?: emptyList()) { entry ->
                            val isSel = (entry.nodeId == selection?.nodeId)
                            val hasChildren = entry.getChildGroups().isNotEmpty() ||
                                    (!groupsOnly && entry.getChildEntries().isNotEmpty())
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = if (isSel) MaterialTheme.colors.primary else MaterialTheme.colors.background)
                                    .clickable {
                                        selection = entry
                                        if (hasChildren) currentRoot = entry
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "group",
                                    tint = if (isSel) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onBackground
                                )
                                if (hasChildren)
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = if (isSel) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onBackground
                                    )
                                Text(
                                    entry.title,
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = if (isSel) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onBackground
                                )
                            }
                        }
                        items(if (groupsOnly) emptyList() else currentRoot?.getChildEntries() ?: emptyList()) { entry ->
                            val isSel = (entry.nodeId == selection?.nodeId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = if (isSel) MaterialTheme.colors.primary else MaterialTheme.colors.background)
                                    .clickable {
                                        selection = entry
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = "entry",
                                    tint = if (isSel) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onBackground
                                )
                                Text(
                                    entry.title,
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = if (isSel) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onBackground
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        enabled = selection != null,
                        onClick = {
                            onDismiss(selection)
                        }) {
                        Text("Select " + if (selection is Group) "Group" else "Entry")
                    }
                }
            }
        }
    )
}
