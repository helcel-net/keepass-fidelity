package net.helcel.fidelity.activity

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.preference.PreferenceManager
import net.helcel.fidelity.R


object ToastHelper{
    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
}

@Composable
fun SysTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val themeKey = prefs.getString(stringResource(R.string.key_theme), stringResource(R.string.system))
    val darkTheme = when (themeKey) {
        stringResource(R.string.system) -> isSystemInDarkTheme()
        stringResource(R.string.light) -> false
        stringResource(R.string.dark) -> true
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if(darkTheme) dynamicDarkColorScheme(LocalContext.current ) else dynamicLightColorScheme(LocalContext.current )
    } else {
        if(darkTheme) darkColorScheme() else lightColorScheme()
    }
    val m2colors = Colors(
        primary = colorScheme.primary,
        primaryVariant = colorScheme.primaryContainer,
        secondary = colorScheme.secondary,
        background = colorScheme.background,
        surface = colorScheme.surface,
        onPrimary = colorScheme.onPrimary,
        onSecondary = colorScheme.onSecondary,
        onBackground = colorScheme.onBackground,
        onSurface = colorScheme.onSurface,
        secondaryVariant = colorScheme.secondary,
        error = colorScheme.error,
        onError = colorScheme.onError,
        isLight = !darkTheme,
    )

    MaterialTheme(
        colors = m2colors,
        content = content
    )
}
