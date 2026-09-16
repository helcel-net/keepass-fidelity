package net.helcel.fidelity.activity.fragment

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import net.helcel.fidelity.activity.ToastHelper
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.isSearchVisible
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onAdd
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onQuery
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onRefresh
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onView
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.searchQuery
import net.helcel.fidelity.tools.AppModeStore
import net.helcel.fidelity.tools.FidelityEntry
import net.helcel.fidelity.tools.FidelityRepository.activeEntry
import net.helcel.fidelity.tools.FidelityRepository.cacheEntry
import net.helcel.fidelity.tools.FidelityRepository.entries
import net.helcel.fidelity.tools.KeepassDatabase
import net.helcel.fidelity.tools.FidelityRepository.loadEntries
import net.helcel.fidelity.tools.FidelityRepository.removeEntry
import net.helcel.fidelity.tools.Kp2a

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    navController: NavHostController?,
) {
    if(navController==null) return
    var isRefreshingState by remember { mutableStateOf(false) }
    var showHidden by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val kp2a = AppModeStore.isKp2a

    val kp2aQueryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val entry = Kp2a.entryFromIntent(result.data)
        if (entry == null) {
            ToastHelper.show(context, "Entry has no fidelity code")
            return@rememberLauncherForActivityResult
        }
        cacheEntry(context, entry)
        onView(navController, entry)
    }

    BackHandler(enabled = isSearchVisible) {
        onQuery()
    }

    val sortedEntries = remember(entries, showHidden, searchQuery) {
        derivedStateOf {
            entries.filter {
                (showHidden || !it.hidden) &&
                        (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true))
            }.sortedWith(
                compareByDescending<FidelityEntry> { it.pinned }
                    .thenBy { it.hidden }
                    .thenByDescending { it.lastUse }
            )
        }
    }


    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colors.background)) {

        PullToRefreshBox(
            onRefresh = {
                isRefreshingState = true
                scope.launch {
                    // KP2A owns the database: nothing to sync, just reload the cache.
                    if (kp2a) loadEntries(context)
                    else onRefresh(context, navController)
                    isRefreshingState = false
                }
            },
            isRefreshing = isRefreshingState,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar: hidden-cards toggle on the left, storage mode on the right.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hiddenTint =
                        if (showHidden) MaterialTheme.colors.onBackground else MaterialTheme.colors.secondary
                    TextButton(onClick = { showHidden = !showHidden }) {
                        Icon(
                            Icons.Default.HideSource,
                            contentDescription = "Show Hidden",
                            modifier = Modifier.size(16.dp),
                            tint = hiddenTint
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (showHidden) "Showing hidden" else "Show hidden",
                            style = MaterialTheme.typography.caption,
                            color = hiddenTint
                        )
                    }
                    TextButton(onClick = { navController.navigate("mode") }) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Switch mode",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (kp2a) "Keepass2Android" else "Standalone",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.secondary
                        )
                    }
                }
                if (isSearchVisible) {
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = MaterialTheme.colors.onBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .focusRequester(focusRequester),
                        label = { Text("Search") },
                        singleLine = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.clickable {
                                    searchQuery = ""
                                    onQuery()
                                }
                            )
                        }
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedEntries.value) { entry ->
                        FidelityRow(navController, entry)
                    }
                }
            }
            FloatingActionButton(
                onClick = { onQuery() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Query",
                    modifier = Modifier.size(32.dp)
                )
            }
            FloatingActionButton(
                onClick = { onAdd(navController) }, modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
            if (kp2a)
                FloatingActionButton(
                    onClick = { Kp2a.launchQuery(context, kp2aQueryLauncher) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 88.dp),
                    backgroundColor = MaterialTheme.colors.secondary,
                ) {
                    Icon(
                        Icons.Default.Key,
                        tint = MaterialTheme.colors.onSecondary,
                        contentDescription = "Fetch from Keepass2Android"
                    )
                }
        }

        if (isRefreshingState)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background.copy(alpha = 0.75f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { }
                    )
            )
    }

}

object LauncherEventHandlers {
    var isSearchVisible by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    fun onAdd(navController: NavHostController) {
        navController.navigate("edit")
    }

    fun onQuery() {
        isSearchVisible = !isSearchVisible
        if (!isSearchVisible) searchQuery = ""
    }

    /** Standalone: writes the database back. Missing credentials send the user to setup. */
    suspend fun onSave(context: Context, navController: NavHostController): Boolean {
        if (!KeepassDatabase.ensureCredentials(context)) {
            navController.navigate("init")
            return false
        }
        return KeepassDatabase.save(context)
    }

    /** Standalone: (re)opens the database and imports its cards. Missing credentials send the user to setup. */
    suspend fun onRefresh(context: Context, navController: NavHostController): Boolean {
        if (!KeepassDatabase.ensureCredentials(context)) {
            navController.navigate("init")
            return false
        }
        return KeepassDatabase.unlock(context)
    }

    fun onView(navController: NavHostController, entry: FidelityEntry) {
        navController.navigate("view/${entry.uid}")
        val index = entries.indexOfFirst { it.uid == entry.uid }
        if (index != -1)
            entries[index] = entry.copy(lastUse = System.currentTimeMillis().toInt())

    }

    fun onPin(entry: FidelityEntry){
        val index = entries.indexOfFirst { it.uid == entry.uid }
        if (index != -1)
            entries[index] = entry.copy(pinned = !entry.pinned)
    }

    fun onHide(entry: FidelityEntry){
        val index = entries.indexOfFirst { it.uid == entry.uid }
        if (index != -1)
            entries[index] = entry.copy(hidden = !entry.hidden)
    }

    fun onRemove(context: Context, entry: FidelityEntry) {
        removeEntry(context, entry)
    }

    fun onEdit(navController: NavHostController, entry: FidelityEntry){
        activeEntry.value = entry
        navController.navigate("edit")
    }
}
