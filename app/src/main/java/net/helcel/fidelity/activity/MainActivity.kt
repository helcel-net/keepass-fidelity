package net.helcel.fidelity.activity

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.helcel.fidelity.activity.fragment.CreateEntryScreen
import net.helcel.fidelity.activity.fragment.FileScanner
import net.helcel.fidelity.activity.fragment.InitialScreen
import net.helcel.fidelity.activity.fragment.LauncherScreen
import net.helcel.fidelity.activity.fragment.ScannerScreen
import net.helcel.fidelity.activity.fragment.ViewEntryScreen
import net.helcel.fidelity.tools.FidelityRepository.entries
import net.helcel.fidelity.tools.FidelityRepository.loadEntries
import net.helcel.fidelity.tools.KeePassStore.hasCredentials

class MainActivity : FragmentActivity() {

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        loadEntries(this.baseContext)

        setContent {
            SysTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                BackHandler {
                    if (!navController.popBackStack()) finish()
                }
                LaunchedEffect(Unit) {
                    if(!hasCredentials(context)) navController.navigate("init")
                }
                NavHost(navController = navController, startDestination = "launcher") {
                    composable("exit") { finish() }
                    composable("launcher") { LauncherScreen(navController) }
                    composable("init"){ InitialScreen (navController)}
                    composable("scanCam") { ScannerScreen(navController) }
                    composable("scanFile") { FileScanner(navController) }
                    composable("edit"){ CreateEntryScreen(navController) }
                    composable("view/{entryId}") { e ->
                        val entry = entries.find {
                            it.uid == (e.arguments?.getString("entryId") ?: "")
                        }
                        if (entry == null) return@composable navController.navigate("launcher")
                        ViewEntryScreen(navController,entry)
                    }
                }
            }
        }
    }
}
