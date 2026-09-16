package net.helcel.fidelity.activity.fragment

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onEdit
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onHide
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onPin
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onRemove
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onView
import net.helcel.fidelity.tools.AppModeStore
import net.helcel.fidelity.tools.FidelityEntry

/** One card in the launcher grid: tap to view, long-press for the actions menu. */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FidelityRow(
    navController: NavHostController,
    e: FidelityEntry
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .combinedClickable(
                    onClick = { onView(navController, e) },
                    onLongClick = { expanded = true },
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.background
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                Row(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = e.title,
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onPrimary
                    )
                }
                Row(modifier = Modifier.align(Alignment.TopEnd)) {
                    if (e.hidden)
                        Icon(
                            Icons.Default.HideSource, contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.onPrimary
                        )
                    if (e.hidden && e.pinned)
                        Spacer(modifier = Modifier.width(8.dp))
                    if (e.pinned)
                        Icon(
                            Icons.Default.PushPin, contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.onPrimary
                        )

                }
            }
        }
        DropdownMenu(
            modifier = Modifier,
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // KP2A entries can only be edited in KP2A itself; offer to drop the cached copy.
            if (AppModeStore.isKp2a)
                DropdownMenuItem(onClick = {
                    expanded = false
                    onRemove(context, e)
                }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "remove",
                    )
                    Spacer(modifier= Modifier.width(8.dp))
                    Text("Remove")
                }
            else
                DropdownMenuItem(onClick = {
                    expanded = false
                    onEdit(navController, e)
                }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "edit",
                    )
                    Spacer(modifier= Modifier.width(8.dp))
                    Text("Edit")
                }
            DropdownMenuItem(onClick = {
                expanded = false
                onPin(e)
            }) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = "pin",
                )
                Spacer(modifier= Modifier.width(8.dp))
                if(e.pinned) Text("Unpin")
                else Text("Pin")
            }
            DropdownMenuItem(onClick = {
                expanded = false
                onHide(e)
            }) {
                Icon(
                    Icons.Default.HideSource,
                    contentDescription = "hide",
                )
                Spacer(modifier= Modifier.width(8.dp))
                if(e.hidden) Text("Unhide")
                else Text("Hide")
            }
        }
    }
}
