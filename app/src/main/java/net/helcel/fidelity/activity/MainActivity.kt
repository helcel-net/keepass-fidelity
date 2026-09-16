package net.helcel.fidelity.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.helcel.fidelity.activity.fragment.CreateEntryScreen
import net.helcel.fidelity.activity.fragment.FileScanner
import net.helcel.fidelity.activity.fragment.InitialScreen
import net.helcel.fidelity.activity.fragment.LauncherScreen
import net.helcel.fidelity.activity.fragment.ModeSelectScreen
import net.helcel.fidelity.activity.fragment.ScannerScreen
import net.helcel.fidelity.activity.fragment.ViewEntryScreen
import net.helcel.fidelity.tools.AppMode
import net.helcel.fidelity.tools.AppModeStore
import net.helcel.fidelity.tools.FidelityEntry
import net.helcel.fidelity.tools.FidelityRepository.cacheEntry
import net.helcel.fidelity.tools.FidelityRepository.findEntry
import net.helcel.fidelity.tools.FidelityRepository.loadEntries
import net.helcel.fidelity.tools.FidelityRepository.transientEntry
import net.helcel.fidelity.tools.KeePassStore.hasCredentials
import net.helcel.fidelity.tools.Kp2a

class MainActivity : FragmentActivity() {

    // Entry pushed by Keepass2Android when the user opens this app from an entry.
    private val incomingEntry = mutableStateOf<FidelityEntry?>(null)

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        AppModeStore.load(this)
        loadEntries(this.baseContext)
        incomingEntry.value = Kp2a.entryFromIntent(intent)

        setContent {
            SysTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                BackHandler {
                    if (!navController.popBackStack()) finish()
                }
                LaunchedEffect(Unit) {
                    when (AppModeStore.mode.value) {
                        null -> navController.navigate("mode")
                        AppMode.STANDALONE ->
                            if (!hasCredentials(context)) navController.navigate("init")
                        AppMode.KP2A -> {}
                    }
                }
                LaunchedEffect(incomingEntry.value) {
                    val entry = incomingEntry.value ?: return@LaunchedEffect
                    incomingEntry.value = null
                    if (AppModeStore.isKp2a) cacheEntry(context, entry)
                    else transientEntry.value = entry
                    navController.navigate("view/${entry.uid}")
                }
                // The app draws edge-to-edge (targetSdk >= 35): keep every screen clear of the
                // status and navigation bars, and paint the bars with the theme background.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                        .systemBarsPadding()
                ) {
                    NavHost(navController = navController, startDestination = "launcher") {
                        composable("exit") { finish() }
                        composable("mode") { ModeSelectScreen(navController) }
                        composable("launcher") { LauncherScreen(navController) }
                        composable("init") { InitialScreen(navController) }
                        composable("scanCam") { ScannerScreen(navController) }
                        composable("scanFile") { FileScanner(navController) }
                        composable("edit") { CreateEntryScreen(navController) }
                        composable("view/{entryId}") { e ->
                            val entry = findEntry(e.arguments?.getString("entryId"))
                            if (entry == null) return@composable navController.navigate("launcher")
                            ViewEntryScreen(navController, entry)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Kp2a.entryFromIntent(intent)?.let { incomingEntry.value = it }
    }
}
