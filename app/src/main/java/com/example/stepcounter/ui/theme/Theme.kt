package com.example.stepcounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Светлая цветовая схема. Основной цвет — изумрудный, акцент — оранжевый, дополнительный — голубой.
// «Container»-цвета — мягкие оттенки для фона карточек, «on...» — цвет текста поверх них.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0B8F6E),          // главный цвет (кольцо, кнопки, акценты)
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9F3E4),
    onPrimaryContainer = Color(0xFF00382A),
    secondary = Color(0xFFF28C38),        // оранжевый: линия цели, калории
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4CC),
    onSecondaryContainer = Color(0xFF4A2300),
    tertiary = Color(0xFF2FB4E0),         // голубой: второй цвет градиента
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3F1FC),
    onTertiaryContainer = Color(0xFF00313F),
    background = Color(0xFFF3F8F6),       // фон экрана
    onBackground = Color(0xFF0E1A16),
    surface = Color.White,                // фон карточек
    onSurface = Color(0xFF0E1A16),
    surfaceVariant = Color(0xFFE2EDE8),   // дорожка кольца и полосок
    onSurfaceVariant = Color(0xFF4A5C55),
    outline = Color(0xFFB5C7BF),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,       // фон нижней панели навигации
    surfaceContainerHigh = Color(0xFFEAF2EE),
)

// Тёмная схема: те же роли цветов, но приглушённые и тёмные тона.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF47D7AC),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF0F4D3B),
    onPrimaryContainer = Color(0xFFC9F3E4),
    secondary = Color(0xFFFFB074),
    onSecondary = Color(0xFF4A2300),
    secondaryContainer = Color(0xFF5E3200),
    onSecondaryContainer = Color(0xFFFFE4CC),
    tertiary = Color(0xFF6FD3F5),
    onTertiary = Color(0xFF00313F),
    tertiaryContainer = Color(0xFF004D63),
    onTertiaryContainer = Color(0xFFD3F1FC),
    background = Color(0xFF0B1210),
    onBackground = Color(0xFFE0EBE6),
    surface = Color(0xFF121C18),
    onSurface = Color(0xFFE0EBE6),
    surfaceVariant = Color(0xFF24322C),
    onSurfaceVariant = Color(0xFFA9BDB4),
    outline = Color(0xFF3A4C44),
    surfaceContainerLow = Color(0xFF121C18),
    surfaceContainer = Color(0xFF16221D),
    surfaceContainerHigh = Color(0xFF1C2A24),
)

/** Оборачивает приложение в тему: сама выбирает светлую или тёмную по настройке системы. */
@Composable
fun StepCounterTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
