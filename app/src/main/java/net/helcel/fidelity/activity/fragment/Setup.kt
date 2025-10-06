package net.helcel.fidelity.activity.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.helcel.fidelity.activity.ToastHelper
import net.helcel.fidelity.activity.fragment.SetupEventHandlers.onOpen
import net.helcel.fidelity.tools.CredentialResult
import net.helcel.fidelity.tools.FidelityRepository.genCredentials
import net.helcel.fidelity.tools.FidelityRepository.start
import net.helcel.fidelity.tools.KeePassStore.loadCredentials
import net.helcel.fidelity.tools.KeePassStore.packCredentials
import net.helcel.fidelity.tools.KeePassStore.saveCredentials


class GetPersistentContent : OpenDocument() {
    @SuppressLint("InlinedApi")
    override fun createIntent(context: Context, input: Array<String>): Intent {
        return super.createIntent(context, input).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }
}

@Preview
@Composable
fun InitialScreen(
    navController: NavHostController?
) {
    var loading by remember { mutableStateOf(false) }
    var dbFile by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
    var keyFile by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dbFilePickerLauncher = rememberLauncherForActivityResult(
        contract = GetPersistentContent(),
    ) {
        if(it!=null) {
            dbFile = it
            scope.launch(Dispatchers.IO) {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
    }

    val keyFilePickerLauncher = rememberLauncherForActivityResult(
        contract = GetPersistentContent()
    ) {
        if(it!=null) {
            keyFile = it
            scope.launch(Dispatchers.IO) {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
    }
    BackHandler {
        navController!!.navigate("exit")
    }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Main) {
            when(val res = loadCredentials(context)) {
                CredentialResult.AuthFailed -> null
                CredentialResult.NoData -> null
                is CredentialResult.Success -> {
                    if (res.db != null) dbFile = res.db
                    if (res.key != null) keyFile = res.key
                    if (res.password != "" && password == "") password = res.password
                }
            }
        }
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Keypass Database Setup",
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("KDBX Database:", color = MaterialTheme.colors.onBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Checkbox(
                    enabled = !loading,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.primary,
                            RoundedCornerShape(8.dp)
                        )
                        .size(32.dp),
                    checked = dbFile != null,
                    onCheckedChange = { dbFilePickerLauncher.launch(arrayOf("*/*")) },
                    colors = CheckboxDefaults.colors(
                        uncheckedColor = MaterialTheme.colors.primary,
                        checkedColor = MaterialTheme.colors.primary,
                        checkmarkColor = MaterialTheme.colors.onPrimary
                    ),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                enabled = !loading,
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Unspecified,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                colors = TextFieldDefaults.textFieldColors(
                    textColor = MaterialTheme.colors.onBackground
                ),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("KDBX Key File:", color = MaterialTheme.colors.onBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Checkbox(
                    enabled = !loading,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.primary,
                            RoundedCornerShape(8.dp)
                        )
                        .size(32.dp),
                    checked = keyFile != null,
                    onCheckedChange = { keyFilePickerLauncher.launch(arrayOf("*/*")) },
                    colors = CheckboxDefaults.colors(
                        uncheckedColor = MaterialTheme.colors.primary,
                        checkedColor = MaterialTheme.colors.primary,
                        checkmarkColor = MaterialTheme.colors.onPrimary
                    ),
                )
            }



            Spacer(modifier = Modifier.height(16.dp))

            Button(
                enabled = !loading && password.isNotBlank() && dbFile != null ,
                onClick = {
                    loading = true
                    scope.launch {
                        if(onOpen(context, dbFile!!, password, keyFile)){
                            navController!!.popBackStack()
                            navController.navigate("init")
                        }else{
                            ToastHelper.show(context, "Auth failed...")
                            navController!!.popBackStack()
                            navController.navigate("exit")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        }
        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)){

            if(loading )
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter) // same spot as buttons
                        .padding(bottom = 80.dp),
                )
        }
    }
}

object SetupEventHandlers {
    suspend fun onOpen(context: Context, db: Uri, p: String, key: Uri?): Boolean {
        try {
            val packCred = packCredentials(db, p, key)
            withContext(Dispatchers.IO) {
                    start(context, db, genCredentials(context, packCred)
                    )
            }

            val res = withContext(Dispatchers.Main) {
                saveCredentials(context, packCred)
            }
            return when (res) {
                CredentialResult.AuthFailed, CredentialResult.NoData -> false
                is CredentialResult.Success -> true
            }
        } catch (e: Exception) {
            ToastHelper.show(context, e.message.toString())
            println("Err${e.toString()}")
            println(e.message)
            return false
        }
    }
}