package net.helcel.fidelity.activity.fragment

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.helcel.fidelity.activity.ToastHelper
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.isSearchVisible
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onAdd
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onEdit
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onHide
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onPin
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onQuery
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onRefresh
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onView
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.searchQuery
import net.helcel.fidelity.tools.CredentialResult
import net.helcel.fidelity.tools.FidelityEntry
import net.helcel.fidelity.tools.FidelityRepository.activeEntry
import net.helcel.fidelity.tools.FidelityRepository.end
import net.helcel.fidelity.tools.FidelityRepository.entries
import net.helcel.fidelity.tools.FidelityRepository.genCredentials
import net.helcel.fidelity.tools.FidelityRepository.importDB
import net.helcel.fidelity.tools.FidelityRepository.start
import net.helcel.fidelity.tools.KeePassStore.loadCredentials

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
                    onRefresh(context, navController)
                    isRefreshingState = false
                }
            },
            isRefreshing = isRefreshingState,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
            FloatingActionButton(
                onClick = {
                    showHidden=!showHidden
                }, modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp).size(24.dp),
                backgroundColor =  if(showHidden) MaterialTheme.colors.onBackground else MaterialTheme.colors.secondary,
            ) {
                Icon(Icons.Default.HideSource,
                    tint= if(showHidden) MaterialTheme.colors.background else MaterialTheme.colors.onSecondary,
                    contentDescription = "Show Hidden")
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FidelityRow(
    navController: NavHostController,
    e: FidelityEntry
) {
    var expanded by remember { mutableStateOf(false) }

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


object LauncherEventHandlers {
    var isSearchVisible by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var CRED: CredentialResult.Success? = null

    fun onAdd(navController: NavHostController) {
        navController.navigate("edit")
    }

    fun onQuery() {
        isSearchVisible = !isSearchVisible
        if (!isSearchVisible) searchQuery = ""
    }

    suspend fun onSave(context: Context, navController: NavHostController){
        try {
            if (CRED == null) {
                when (val res = loadCredentials(context)) {
                    CredentialResult.AuthFailed, CredentialResult.NoData -> ToastHelper.show(context, "Unable to Load Credentials")
                    is CredentialResult.Success -> CRED = res
                }
            }
            CRED!!
            val cred = withContext(Dispatchers.IO) {
                genCredentials(context, CRED!!)
            }
            if (withContext(Dispatchers.IO) {
                    end(context, CRED!!.db, cred)
                })
                throw Exception("Error in saving")
        } catch (e: Exception) {
            println(e.toString())
            navController.navigate("init")
        }
    }

    suspend fun onRefresh(context: Context, navController: NavHostController) {
        try {
            if (CRED == null) {
                when (val res = loadCredentials(context)) {
                    CredentialResult.AuthFailed, CredentialResult.NoData -> ToastHelper.show(context, "Unable to Load Credentials")
                    is CredentialResult.Success -> CRED = res
                }
            }
            CRED!!
            val cred = withContext(Dispatchers.IO) {
                genCredentials(context, CRED!!)
            }
            if (withContext(Dispatchers.IO) {
                start(context, CRED!!.db, cred)
            })
                importDB(context)
        } catch (e: Exception) {
            println(e.toString())
            navController.navigate("init")
        }
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

    fun onEdit(navController: NavHostController, entry: FidelityEntry){
        activeEntry.value = entry
        navController.navigate("edit")
    }
}