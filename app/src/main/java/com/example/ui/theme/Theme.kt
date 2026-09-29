package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkColorScheme =
  darkColorScheme(
    primary = PinkPrimaryDark,
    onPrimary = PinkOnPrimaryDark,
    primaryContainer = PinkPrimaryContainerDark,
    onPrimaryContainer = PinkOnPrimaryContainerDark,
    secondary = PinkSecondaryDark,
    onSecondary = PinkOnSecondaryDark,
    secondaryContainer = PinkSecondaryContainerDark,
    onSecondaryContainer = PinkOnSecondaryContainerDark,
    tertiary = PinkTertiaryDark,
    onTertiary = PinkOnTertiaryDark,
    tertiaryContainer = PinkTertiaryContainerDark,
    onTertiaryContainer = PinkOnTertiaryContainerDark,
    background = PinkBackgroundDark,
    onBackground = PinkOnBackgroundDark,
    surface = PinkSurfaceDark,
    onSurface = PinkOnSurfaceDark,
    surfaceVariant = PinkSurfaceVariantDark,
    onSurfaceVariant = PinkOnSurfaceVariantDark,
    outline = PinkOutlineDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PinkPrimary,
    onPrimary = PinkOnPrimary,
    primaryContainer = PinkPrimaryContainer,
    onPrimaryContainer = PinkOnPrimaryContainer,
    secondary = PinkSecondary,
    onSecondary = PinkOnSecondary,
    secondaryContainer = PinkSecondaryContainer,
    onSecondaryContainer = PinkOnSecondaryContainer,
    tertiary = PinkTertiary,
    onTertiary = PinkOnTertiary,
    tertiaryContainer = PinkTertiaryContainer,
    onTertiaryContainer = PinkOnTertiaryContainer,
    background = PinkBackground,
    onBackground = PinkOnBackground,
    surface = PinkSurface,
    onSurface = PinkOnSurface,
    surfaceVariant = PinkSurfaceVariant,
    onSurfaceVariant = PinkOnSurfaceVariant,
    outline = PinkOutline
  )

class AppDimensions(
    val smallPadding: Dp,
    val mediumPadding: Dp,
    val largePadding: Dp,
    val iconSmall: Dp,
    val iconMedium: Dp,
    val iconLarge: Dp,
    val buttonHeight: Dp
)

val LocalAppDimensions = compositionLocalOf {
    AppDimensions(
        smallPadding = 8.dp,
        mediumPadding = 16.dp,
        largePadding = 24.dp,
        iconSmall = 16.dp,
        iconMedium = 24.dp,
        iconLarge = 32.dp,
        buttonHeight = 48.dp
    )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp
  
  // Scale factor based on standard phone width (approx 360dp)
  val scale = when {
      screenWidth > 0 && screenWidth < 360 -> 0.9f
      screenWidth > 600 -> 1.2f
      else -> 1.0f
  }

  val dimensions = AppDimensions(
      smallPadding = (8 * scale).dp,
      mediumPadding = (16 * scale).dp,
      largePadding = (24 * scale).dp,
      iconSmall = (16 * scale).dp,
      iconMedium = (24 * scale).dp,
      iconLarge = (32 * scale).dp,
      buttonHeight = (48 * scale).dp
  )

  // Use a copy of our defined AppTypography with scaled font sizes
  val responsiveTypography = Typography(
      displayLarge = AppTypography.displayLarge.copy(fontSize = (AppTypography.displayLarge.fontSize.value * scale).sp),
      displayMedium = AppTypography.displayMedium.copy(fontSize = (AppTypography.displayMedium.fontSize.value * scale).sp),
      displaySmall = AppTypography.displaySmall.copy(fontSize = (AppTypography.displaySmall.fontSize.value * scale).sp),
      headlineLarge = AppTypography.headlineLarge.copy(fontSize = (AppTypography.headlineLarge.fontSize.value * scale).sp),
      headlineMedium = AppTypography.headlineMedium.copy(fontSize = (AppTypography.headlineMedium.fontSize.value * scale).sp),
      headlineSmall = AppTypography.headlineSmall.copy(fontSize = (AppTypography.headlineSmall.fontSize.value * scale).sp),
      titleLarge = AppTypography.titleLarge.copy(fontSize = (AppTypography.titleLarge.fontSize.value * scale).sp),
      titleMedium = AppTypography.titleMedium.copy(fontSize = (AppTypography.titleMedium.fontSize.value * scale).sp),
      titleSmall = AppTypography.titleSmall.copy(fontSize = (AppTypography.titleSmall.fontSize.value * scale).sp),
      bodyLarge = AppTypography.bodyLarge.copy(fontSize = (AppTypography.bodyLarge.fontSize.value * scale).sp),
      bodyMedium = AppTypography.bodyMedium.copy(fontSize = (AppTypography.bodyMedium.fontSize.value * scale).sp),
      bodySmall = AppTypography.bodySmall.copy(fontSize = (AppTypography.bodySmall.fontSize.value * scale).sp),
      labelLarge = AppTypography.labelLarge.copy(fontSize = (AppTypography.labelLarge.fontSize.value * scale).sp),
      labelMedium = AppTypography.labelMedium.copy(fontSize = (AppTypography.labelMedium.fontSize.value * scale).sp),
      labelSmall = AppTypography.labelSmall.copy(fontSize = (AppTypography.labelSmall.fontSize.value * scale).sp)
  )

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  CompositionLocalProvider(LocalAppDimensions provides dimensions) {
      MaterialTheme(
          colorScheme = colorScheme,
          typography = responsiveTypography,
          content = content
      )
  }
}

val MaterialTheme.dimens: AppDimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalAppDimensions.current
