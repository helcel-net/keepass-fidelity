package net.helcel.fidelity.activity.fragment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import net.helcel.fidelity.tools.AppMode
import net.helcel.fidelity.tools.AppModeStore
import net.helcel.fidelity.tools.FidelityRepository.loadEntries
import net.helcel.fidelity.tools.KeePassStore.hasCredentials
import net.helcel.fidelity.tools.Kp2a

@Preview
@Composable
fun ModeSelectScreen(navController: NavHostController?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val current = AppModeStore.mode.value
    val kp2aInstalled = remember { Kp2a.isAvailable(context) }

    BackHandler {
        // Nothing to go back to before the first choice has been made.
        if (current == null) navController!!.navigate("exit")
        else navController!!.popBackStack()
    }

    fun select(mode: AppMode) {
        if (mode != current) {
            AppModeStore.set(context, mode)
            loadEntries(context)
        }
        navController!!.popBackStack("launcher", inclusive = false)
        scope.launch {
            if (mode == AppMode.STANDALONE && !hasCredentials(context))
                navController.navigate("init")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Storage Mode",
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Where should your fidelity cards be stored?",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            ModeCard(
                icon = Icons.Default.FolderOpen,
                title = "Standalone",
                description = "Open a KeePass database (.kdbx) directly. " +
                        "Entries are read and written by this app.",
                selected = current == AppMode.STANDALONE,
            ) { select(AppMode.STANDALONE) }
            Spacer(modifier = Modifier.height(12.dp))
            ModeCard(
                icon = Icons.Default.Extension,
                title = "Keepass2Android plugin",
                description = "Use the Keepass2Android app as the database. " +
                        "Entries are queried from and created in Keepass2Android.",
                selected = current == AppMode.KP2A,
            ) { select(AppMode.KP2A) }
            if (!kp2aInstalled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Keepass2Android does not seem to be installed.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.error
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface
    val content = if (selected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content
        ),
        border = if (selected) null else CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = content)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.h6, color = content)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.body2, color = content)
            }
        }
    }
}
