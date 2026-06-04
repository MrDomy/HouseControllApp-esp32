package com.example.makarovhouse.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandWhite, // В темной теме акцентный цвет - белый
    onPrimary = BrandBlack, // Текст на акцентном - черный
    background = BrandBlack,
    surface = BrandBlack,
    onSurface = BrandWhite
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlack,
    onPrimary = BrandWhite,
    background = BrandGrayLight,
    surface = BrandWhite,
    onSurface = BrandBlack
)

@Composable
fun MakarovHouseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Возвращаем поддержку темной темы
    dynamicColor: Boolean = false, // Отключаем динамические цвета, чтобы сохранить стиль Learn Up
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}