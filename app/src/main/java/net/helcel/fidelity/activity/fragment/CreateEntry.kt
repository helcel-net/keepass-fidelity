package net.helcel.fidelity.activity.fragment

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.kunzisoft.keepass.database.element.Entry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.helcel.fidelity.R
import net.helcel.fidelity.activity.ToastHelper
import net.helcel.fidelity.activity.fragment.CreateEntryEventHandler.onCameraScan
import net.helcel.fidelity.activity.fragment.CreateEntryEventHandler.onFileScan
import net.helcel.fidelity.activity.fragment.CreateEntryEventHandler.onSubmit
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onRefresh
import net.helcel.fidelity.activity.fragment.LauncherEventHandlers.onSave
import net.helcel.fidelity.tools.BarcodeGenerator.generateBarcode
import net.helcel.fidelity.tools.FidelityEntry
import net.helcel.fidelity.tools.FidelityRepository
import net.helcel.fidelity.tools.FidelityRepository.activeEntry
import net.helcel.fidelity.tools.FidelityRepository.addEntry


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

    LaunchedEffect(entry) {
        isValidBarcode = false
        delay(500)
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
        TreeSelectorDialog(
            onDismiss = {
                showDialog = false
                if(it!=null){
                    entry = entry.copy(uid = it.nodeId?.id.toString())
                    if(it is Entry){
                        entry = entry.copy(title = it.title)
                    }
                }
            }
        )
    }
    val formats = stringArrayResource(R.array.format_array)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        )
        {
            OutlinedTextField(
                value = entry.title,
                enabled = entry.uid!=null,
                onValueChange = {
                    entry = entry.copy(title = it)
                    errorTitle = ""
                },
                label = { Text(text = "Title") },
                isError = errorTitle.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = if(entry.uid!=null)MaterialTheme.colors.onBackground
                    else MaterialTheme.colors.secondary
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
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
                        isValidBarcode
                    ) {
                        if (FidelityRepository.getRoot() == null) {
                            isLoading = true
                            scope.launch {
                                onRefresh(ctx, navController!!)
                                isLoading = false
                                if(entry.uid!=null){
                                    addEntry(ctx,entry)
                                    isLoading = true
                                    onSave(ctx,navController)
                                    isLoading = false
                                    onSubmit(navController)
                                }else {
                                    showDialog = true
                                }
                            }
                        } else {
                            if(entry.uid!=null){
                                addEntry(ctx,entry)
                                isLoading = true
                                scope.launch {
                                    onSave(ctx, navController!!)
                                    isLoading = false
                                    onSubmit(navController)
                                }
                            }else {
                                showDialog = true
                            }
                        }
                    }
                },
                enabled = isValidBarcode.and(entry.uid==null || entry.title.isNotEmpty()),
            ) {
                Text(if(entry.uid==null)"Select Entry" else "Save", style = MaterialTheme.typography.h6)
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
    if (entry.uid!=null && entry.title.isBlank()) tErr = "Title cannot be empty"
    if (entry.code.isBlank()) cErr = "Code cannot be empty"
    if (entry.format.isBlank()) fErr = "Format cannot be empty"

    setErrors(tErr, cErr, fErr)

    if (tErr.isEmpty() && cErr.isEmpty() && fErr.isEmpty() && isValidBarcode) {
        onValid(entry.copy())
    }
}

object CreateEntryEventHandler {
    fun onSubmit(navController: NavHostController){
        navController.popBackStack()
        activeEntry.value = activeEntry.value.copy(null,"","","",false)
    }

    fun onFileScan(navController: NavHostController){
        navController.navigate("scanFile")
    }
    fun onCameraScan(navController: NavHostController){
        navController.navigate("scanCam")
    }
}