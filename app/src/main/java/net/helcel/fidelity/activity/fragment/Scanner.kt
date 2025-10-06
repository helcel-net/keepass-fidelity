@file:Suppress("PreviewAnnotationInFunctionWithParameters",
    "PreviewAnnotationInFunctionWithParameters"
)

package net.helcel.fidelity.activity.fragment

import android.Manifest
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.helcel.fidelity.activity.fragment.ScannerEventHandler.onResult
import net.helcel.fidelity.tools.BarcodeScanner
import net.helcel.fidelity.tools.BarcodeScanner.analysisUseCase
import net.helcel.fidelity.tools.FidelityRepository.activeEntry

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun ScannerScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }
    var camera: Camera? by remember { mutableStateOf(null) }
    var torchOn by remember { mutableStateOf(false) }

    val done = remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                val cameraProvider = cameraProviderFuture.get()
                val previewUseCase = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysisUseCase = analysisUseCase { detectedCode, detectedFormat ->
                    if (detectedCode.isNullOrEmpty() || detectedFormat.isNullOrEmpty()) return@analysisUseCase
                    if(done.value) return@analysisUseCase
                    scope.launch(Dispatchers.Main) {
                        activeEntry.value =
                            activeEntry.value.copy(code = detectedCode, format = detectedFormat)
                        done.value = true
                        onResult(navController)
                    }
                    return@analysisUseCase
                }
                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        previewUseCase,
                        analysisUseCase
                    )
                } catch (e: Exception) {
                    Log.e("ScannerScreen", "Camera bind failed: ${e.message}")
                }
            } else {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
                scope.launch(Dispatchers.Main){
                    onResult(navController)
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        ScannerOverlay(
            modifier = Modifier.fillMaxSize()
        )
        Button(onClick = {
            torchOn = !torchOn
            camera?.cameraControl?.enableTorch(torchOn)
        }, modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
        ) {
            Icon(Icons.Default.FlashOn, contentDescription = null)
        }

        if(!done.value)
            CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter) // same spot as buttons
                .padding(bottom =80.dp),
            )
    }
}


@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val widthF = size.width
        val heightF = size.height

        drawRect(
            color = Color(0x80000000), // semi-transparent black
            size = size
        )

        val squareSize = 0.75f * minOf(widthF, heightF)
        val left = (widthF - squareSize) / 2
        val top = (heightF - squareSize) / 2

        drawRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(squareSize, squareSize),
            blendMode = BlendMode.Clear
        )
    }
}


@Composable
fun FileScanner(navController: NavHostController) {
    val context = LocalContext.current

    rememberCoroutineScope()
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            Toast.makeText(context, "No file selected", Toast.LENGTH_SHORT).show()
            onResult(navController)
            return@rememberLauncherForActivityResult
        }
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            BarcodeScanner.bitmapUseCase(bitmap) { code, format ->
                if (!code.isNullOrEmpty() && !format.isNullOrEmpty()) {
                    activeEntry.value = activeEntry.value.copy(code=code, format=format)
                    onResult(navController)
                } else {
                    Toast.makeText(context, "No barcode found", Toast.LENGTH_SHORT).show()
                    onResult(navController)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            onResult(navController)
        }

    }

    LaunchedEffect(Unit) {
        pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    BackHandler {
        onResult(navController)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

object ScannerEventHandler {
    fun onResult(navController: NavController) {
        navController.popBackStack()
    }
}
