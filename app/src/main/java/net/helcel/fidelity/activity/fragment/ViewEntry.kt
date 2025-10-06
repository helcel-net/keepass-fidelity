package net.helcel.fidelity.activity.fragment

import android.app.Activity
import android.graphics.Bitmap
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import net.helcel.fidelity.tools.BarcodeGenerator.generateBarcode
import net.helcel.fidelity.tools.FidelityEntry
import kotlin.let
import kotlin.math.min


@Preview
@Composable
fun PreviewEntryScreen(){
  ViewEntryScreen(null, FidelityEntry("Title","AAA","QR"))
}

@Composable
fun ViewEntryScreen(
    navController: NavHostController?,
    entry: FidelityEntry
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isFull by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    SideEffect {
        activity?.window?.attributes = activity.window?.attributes?.apply {
            screenBrightness = if (isFull) 1f else BRIGHTNESS_OVERRIDE_NONE
        }
        try {
            bitmap = generateBarcode(entry.code, entry.format, 1024)
        } catch (_: Exception) {
            bitmap = null
            Toast.makeText(context, "Invalid barcode format", Toast.LENGTH_SHORT).show()
        }
    }
    BackHandler {
        isFull=false
        navController!!.popBackStack()
    }
    

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                onClick = { isFull = !isFull },
                indication = null, // remove ripple effect
                interactionSource = remember { MutableInteractionSource() }
            ),
            contentAlignment = Alignment.TopCenter
    ) {
        if (!isFull) {
            Text(
                text = entry.title,
                color = Color.White,
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(32.dp)
            )
        }
    }


    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
            bitmap?.let {


                val modifier = Modifier
                    .fillMaxSize()
                    .width(maxWidth)
                    .height(maxHeight)
                    .padding(16.dp)
                    .aspectRatio(it.width.toFloat()/it.height.toFloat())
                    .rotate(if (isFull) 90f else 0f)
                    .scale(if(isFull) min(it.width.dp/maxHeight,it.height.dp/maxWidth) else 1f)

                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Barcode",
                    modifier = modifier,
                    contentScale = ContentScale.Fit,
                )
            } ?: CircularProgressIndicator(color = Color.White)
        }
}