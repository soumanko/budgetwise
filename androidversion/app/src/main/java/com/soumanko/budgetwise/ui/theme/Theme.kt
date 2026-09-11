package com.soumanko.budgetwise.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BudgetWiseColorScheme = darkColorScheme(
    primary = BudgetTurquoise,
    onPrimary = Color.Black,
    primaryContainer = BudgetTurquoiseVariant,
    onPrimaryContainer = SoftWhite,
    
    secondary = BudgetTurquoiseVariant,
    onSecondary = SoftWhite,
    
    background = DarkNavyBackground,
    onBackground = SoftWhite,
    
    surface = SurfaceNavy,
    onSurface = SoftWhite,
    surfaceVariant = ElevatedSurfaceNavy,
    onSurfaceVariant = MutedBlueGray,
    
    error = ExpenseRed,
    onError = SoftWhite
)

private val LightColorScheme = lightColorScheme(
    primary = BudgetTurquoise,
    onPrimary = Color.Black,
    primaryContainer = BudgetTurquoiseVariant,
    onPrimaryContainer = SoftWhite,
    
    secondary = BudgetTurquoiseVariant,
    onSecondary = SoftWhite,
    
    background = LightBackground,
    onBackground = DarkText,
    
    surface = LightSurface,
    onSurface = DarkText,
    surfaceVariant = ElevatedLightSurface,
    onSurfaceVariant = MutedBlueGray,
    
    error = ExpenseRed,
    onError = SoftWhite
)

@Composable
fun BudgetWiseTheme(
    appearance: String = com.soumanko.budgetwise.data.local.AppearancePrefs.MODE_SYSTEM,
    dynamicColor: Boolean = false, // Disable dynamic color to enforce our palette
    content: @Composable () -> Unit
) {
    val darkTheme = when (appearance) {
        com.soumanko.budgetwise.data.local.AppearancePrefs.MODE_LIGHT -> false
        com.soumanko.budgetwise.data.local.AppearancePrefs.MODE_DARK -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) BudgetWiseColorScheme else LightColorScheme
    
    val shapes = androidx.compose.material3.Shapes(
        small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),   // Buttons, etc.
        medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), // Defaults
        large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)   // Cards, Dialogs
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}