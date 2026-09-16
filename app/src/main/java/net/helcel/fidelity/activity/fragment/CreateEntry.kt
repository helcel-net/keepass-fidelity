package net.helcel.fidelity.activity.fragment

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.zxing.FormatException
import com.kunzisoft.keepass.database.element.Group
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.helcel.fidelity.R
import net.helcel.fidelity.activity.ToastHelper
import net.helcel.fidelity.activity.fragment.CreateEntryEventHandler.ensureUnlocked
import net.helcel.fidelity.activity.fragment.CreateEntryEventHandler.onCameraScan
import net.helcel.fidelity.activity.fragment.CreateEntryEventHandler.onFileScan
import net.helcel.fidelity.activity.fragment.CreateEntryEventHandler.onSaveKp2a
import net.helcel.fidelity.activity.fragment.CreateEntryEventHandler.onSaveStandalone
import net.helcel.fidelity.activity.fragment.CreateEntryEventHandler.onSubmit
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onRefresh
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onSave
import net.helcel.fidelity.tools.AppModeStore
import net.helcel.fidelity.tools.BarcodeGenerator.generateBarcode
import net.helcel.fidelity.tools.FidelityEntry
import net.helcel.fidelity.tools.FidelityRepository.activeEntry
import net.helcel.fidelity.tools.KeepassDatabase
import net.helcel.fidelity.tools.KeepassDatabase.addEntry
import net.helcel.fidelity.tools.FidelityRepository.cacheEntry
import net.helcel.fidelity.tools.Kp2a
import kotlin.time.Duration.Companion.milliseconds


@Preview
@Composable
fun CreateEntryScreen(navController: NavHostController?) {
    var entry by remember { activeEntry }
    var errorTitle by remember { mutableStateOf("") }
    var errorCode by remember { mutableStateOf("") }
    var errorFormat by remember { mutableStateOf("") }

    var barcodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isValidBarcode by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val kp2a = AppModeStore.isKp2a
    // Standalone only: where a new card is created (root unless changed). Existing cards
    // (uid set) stay where they are.
    var group by remember { mutableStateOf(KeepassDatabase.getRoot()) }
    val showGroup = !kp2a && entry.uid == null

    fun pickGroup() {
        isLoading = true
        scope.launch {
            if (ensureUnlocked(ctx, navController!!)) {
                group = group ?: KeepassDatabase.getRoot()
                showDialog = true
            }
            isLoading = false
        }
    }

    LaunchedEffect(entry) {
        isValidBarcode = false
        delay(500.milliseconds)
        if (entry.code.isEmpty()) return@LaunchedEffect
        try {
            val bmp = generateBarcode(entry.code, entry.format, 600)
            barcodeBitmap = bmp
            isValidBarcode = true
            errorCode = ""
        } catch (_: FormatException) {
            barcodeBitmap = null
            errorCode = "Invalid Format"
        } catch (e: IllegalArgumentException) {
            barcodeBitmap = null
            errorCode = if (e.message == "com.google.zxing.FormatException") "Invalid Format"
            else e.message ?: "Invalid Argument"
        } catch (e: Exception) {
            barcodeBitmap = null
            ToastHelper.show(ctx, e.message ?: e.toString())
        }
    }

    if (showDialog) {
        TreeSelectorDialog(groupsOnly = true) {
            showDialog = false
            if (it is Group) group = it
        }
    }
    val formats = stringArrayResource(R.array.format_array)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp, 32.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        )
        {
            OutlinedTextField(
                value = entry.title,
                onValueChange = {
                    entry = entry.copy(title = it)
                    errorTitle = ""
                },
                label = { Text(text = "Title") },
                isError = errorTitle.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = MaterialTheme.colors.onBackground
                ),
            )
            if (errorTitle.isNotEmpty()) {
                Text(errorTitle, color = MaterialTheme.colors.error)
            }

            OutlinedTextField(
                value = entry.code,
                onValueChange = {
                    entry = entry.copy(code = it)
                    errorCode = ""
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = MaterialTheme.colors.onBackground
                ),
                label = { Text("Code") },
                isError = errorCode.isNotEmpty(),
                maxLines = 5,
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
            if (errorCode.isNotEmpty()) {
                Text(errorCode, color = MaterialTheme.colors.error)
            }

            FormatDropdown(
                formats,
                entry.format,
                errorFormat.ifEmpty { null },
            ) {
                entry = entry.copy(format = it)
                errorFormat = ""
            }
            if (errorFormat.isNotEmpty()) {
                Text(errorFormat, color = MaterialTheme.colors.error)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = entry.protected,
                    onCheckedChange = {
                        entry = entry.copy(protected = it)
                    },
                    colors = CheckboxDefaults.colors()
                )
                Text("Protected", color = MaterialTheme.colors.onBackground)

                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { onCameraScan(navController!!) }) {
                    Icon(Icons.Default.Camera, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onFileScan(navController!!) }) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                }
            }
            if (showGroup) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Group: ", color = MaterialTheme.colors.onBackground)
                    Text(
                        group?.title ?: "(database root)",
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    Button(onClick = { pickGroup() }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Choose group")
                    }
                }
            }
            if (barcodeBitmap != null) {
                Image(
                    bitmap = barcodeBitmap!!.asImageBitmap(),
                    contentDescription = "Barcode preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    onSubmitIfValid(
                        entry,
                        setErrors = { t, c, f ->
                            errorTitle = t
                            errorCode = c
                            errorFormat = f
                        },
                        isValidBarcode,
                    ) {
                        if (kp2a) {
                            onSaveKp2a(ctx, navController!!, entry)
                        } else {
                            isLoading = true
                            scope.launch {
                                onSaveStandalone(ctx, navController!!, entry, group)
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = isValidBarcode && entry.title.isNotEmpty(),
            ) {
                Text("Save", style = MaterialTheme.typography.h6)
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background.copy(alpha = 0.75f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { }
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FormatDropdown(
    formats: Array<String>,
    format: String,
    errorFormat: String?,
    onFormatChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = format,
            onValueChange = {},
            readOnly = true, // important for dropdown
            label = { Text("Format", color=MaterialTheme.colors.onBackground) },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Expand",
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                textColor = MaterialTheme.colors.onBackground
            ),
            isError = errorFormat != null,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            formats.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onFormatChange(option)
                        expanded = false
                    }
                ) {
                    Text(option)
                }
            }
        }
    }
}



private fun onSubmitIfValid(
    entry: FidelityEntry,
    setErrors: (String, String, String) -> Unit,
    isValidBarcode: Boolean,
    onValid: (FidelityEntry) -> Unit
) {
    var tErr = ""
    var cErr = ""
    var fErr = ""
    if (entry.title.isBlank()) tErr = "Title cannot be empty"
    if (entry.code.isBlank()) cErr = "Code cannot be empty"
    if (entry.format.isBlank()) fErr = "Format cannot be empty"

    setErrors(tErr, cErr, fErr)

    if (tErr.isEmpty() && cErr.isEmpty() && fErr.isEmpty() && isValidBarcode) {
        onValid(entry.copy())
    }
}

object CreateEntryEventHandler {
    /** Standalone: the database must be open before groups can be browsed or cards written. */
    suspend fun ensureUnlocked(context: Context, navController: NavHostController): Boolean =
        KeepassDatabase.getRoot() != null || onRefresh(context, navController)

    /** Standalone: updates the existing entry, or creates one in [group] (the root when null). */
    suspend fun onSaveStandalone(
        context: Context,
        navController: NavHostController,
        entry: FidelityEntry,
        group: Group?,
    ) {
        if (!ensureUnlocked(context, navController)) return
        val root = KeepassDatabase.getRoot() ?: return
        val target =
            if (entry.uid != null) entry
            else entry.copy(uid = (group ?: root).nodeId.id.toString())
        addEntry(context, target)
        if (onSave(context, navController)) onSubmit(navController)
    }

    /**
     * KP2A creates the entry in its own task and never reports back, so the card is cached
     * right away; a later fetch replaces the local uid with KP2A's.
     */
    fun onSaveKp2a(context: Context, navController: NavHostController, entry: FidelityEntry) {
        val kpEntry = entry.copy(uid = Kp2a.localUid(entry.title))
        if (Kp2a.launchAdd(context, kpEntry)) {
            cacheEntry(context, kpEntry)
            onSubmit(navController)
        }
    }

    fun onSubmit(navController: NavHostController){
        navController.popBackStack()
        activeEntry.value = activeEntry.value.copy(
            uid = null,
            title = "",
            code = "",
            format = "",
            protected = false
        )
    }

    fun onFileScan(navController: NavHostController){
        navController.navigate("scanFile")
    }
    fun onCameraScan(navController: NavHostController){
        navController.navigate("scanCam")
    }
}