package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.AppThemePalette
import com.example.data.ThemeMode

val LocalHeirloomPalette = compositionLocalOf { AppThemePalette.HEARTH_TERRACOTTA }

fun getPaletteColorScheme(palette: AppThemePalette, isDark: Boolean): ColorScheme {
    return when (palette) {
        AppThemePalette.HEARTH_TERRACOTTA -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFFFFB59E),
                    onPrimary = Color(0xFF5C1B05),
                    primaryContainer = Color(0xFF7C2E14),
                    onPrimaryContainer = Color(0xFFFFDBD0),
                    secondary = Color(0xFFF9BC53),
                    onSecondary = Color(0xFF432C00),
                    secondaryContainer = Color(0xFF604100),
                    onSecondaryContainer = Color(0xFFFFDEAE),
                    tertiary = Color(0xFFAED0A5),
                    onTertiary = Color(0xFF193616),
                    tertiaryContainer = Color(0xFF304D2B),
                    onTertiaryContainer = Color(0xFFCAECC0),
                    background = Color(0xFF1E1510),
                    onBackground = Color(0xFFEDE0D8),
                    surface = Color(0xFF1E1510),
                    onSurface = Color(0xFFEDE0D8),
                    surfaceVariant = Color(0xFF352720),
                    onSurfaceVariant = Color(0xFFD7C2B8),
                    surfaceContainerHigh = Color(0xFF2B201A),
                    surfaceContainerLowest = Color(0xFF150E0A),
                    outline = Color(0xFF9E8B83),
                    outlineVariant = Color(0xFF51433D)
                )
            } else {
                lightColorScheme(
                    primary = TerracottaPrimary,
                    onPrimary = OnPrimary,
                    primaryContainer = TerracottaPrimaryContainer,
                    onPrimaryContainer = OnPrimaryContainer,
                    secondary = MustardSecondary,
                    onSecondary = OnSecondary,
                    secondaryContainer = MustardSecondaryContainer,
                    onSecondaryContainer = OnSecondaryContainer,
                    tertiary = SageTertiary,
                    onTertiary = OnTertiary,
                    tertiaryContainer = SageTertiaryContainer,
                    onTertiaryContainer = OnTertiaryContainer,
                    background = WarmBackground,
                    onBackground = WarmOnBackground,
                    surface = WarmSurface,
                    onSurface = WarmOnSurface,
                    surfaceVariant = SurfaceContainerHighest,
                    onSurfaceVariant = WarmOnSurfaceVariant,
                    surfaceContainerHigh = SurfaceContainerHigh,
                    surfaceContainerLowest = SurfaceContainerLowest,
                    outline = WarmOutline,
                    outlineVariant = WarmOutlineVariant,
                    inverseSurface = InverseSurface,
                    inverseOnSurface = InverseOnSurface
                )
            }
        }

        AppThemePalette.AVOCADO_OLIVE -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFFA0D387),
                    onPrimary = Color(0xFF143703),
                    primaryContainer = Color(0xFF284615),
                    onPrimaryContainer = Color(0xFFBCF0A3),
                    secondary = Color(0xFFF8BD4A),
                    onSecondary = Color(0xFF452F00),
                    secondaryContainer = Color(0xFF634500),
                    onSecondaryContainer = Color(0xFFFFDE9F),
                    tertiary = Color(0xFFFFB590),
                    onTertiary = Color(0xFF4E2002),
                    tertiaryContainer = Color(0xFF6B3210),
                    onTertiaryContainer = Color(0xFFFFDBD0),
                    background = Color(0xFF131710),
                    onBackground = Color(0xFFE2E5DC),
                    surface = Color(0xFF131710),
                    onSurface = Color(0xFFE2E5DC),
                    surfaceVariant = Color(0xFF2B3326),
                    onSurfaceVariant = Color(0xFFC3C9BC),
                    surfaceContainerHigh = Color(0xFF20261D),
                    surfaceContainerLowest = Color(0xFF0C100A),
                    outline = Color(0xFF8D9387),
                    outlineVariant = Color(0xFF434A3E)
                )
            } else {
                lightColorScheme(
                    primary = AvocadoPrimary,
                    onPrimary = Color.White,
                    primaryContainer = AvocadoPrimaryContainer,
                    onPrimaryContainer = Color(0xFFF6FFF0),
                    secondary = OliveBrassSecondary,
                    onSecondary = Color.White,
                    secondaryContainer = OliveBrassSecondaryContainer,
                    onSecondaryContainer = Color(0xFF452F00),
                    tertiary = ToastedPecanTertiary,
                    onTertiary = Color.White,
                    tertiaryContainer = ToastedPecanTertiaryContainer,
                    onTertiaryContainer = Color(0xFFFFECE3),
                    background = AvocadoBackground,
                    onBackground = Color(0xFF1B1E17),
                    surface = AvocadoSurface,
                    onSurface = Color(0xFF1B1E17),
                    surfaceVariant = Color(0xFFDFE6D5),
                    onSurfaceVariant = Color(0xFF43493E),
                    surfaceContainerHigh = Color(0xFFE4ECD9),
                    surfaceContainerLowest = Color(0xFFFFFFFF),
                    outline = Color(0xFF737A6D),
                    outlineVariant = Color(0xFFC3CCCB)
                )
            }
        }

        AppThemePalette.NAVY_COPPER -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFF96CCFA),
                    onPrimary = Color(0xFF003353),
                    primaryContainer = Color(0xFF17476C),
                    onPrimaryContainer = Color(0xFFCCE5FF),
                    secondary = Color(0xFFFFB596),
                    onSecondary = Color(0xFF581D02),
                    secondaryContainer = Color(0xFF7A2E0C),
                    onSecondaryContainer = Color(0xFFFFDBD0),
                    tertiary = Color(0xFFA1D2BC),
                    onTertiary = Color(0xFF053525),
                    tertiaryContainer = Color(0xFF1E4C3B),
                    onTertiaryContainer = Color(0xFFBCEEDA),
                    background = Color(0xFF10141A),
                    onBackground = Color(0xFFE1E5ED),
                    surface = Color(0xFF10141A),
                    onSurface = Color(0xFFE1E5ED),
                    surfaceVariant = Color(0xFF232B36),
                    onSurfaceVariant = Color(0xFFC1CBD7),
                    surfaceContainerHigh = Color(0xFF1B232E),
                    surfaceContainerLowest = Color(0xFF0B0E13),
                    outline = Color(0xFF8B95A2),
                    outlineVariant = Color(0xFF3E4752)
                )
            } else {
                lightColorScheme(
                    primary = IndigoPrimary,
                    onPrimary = Color.White,
                    primaryContainer = IndigoPrimaryContainer,
                    onPrimaryContainer = Color(0xFFF0F6FF),
                    secondary = CopperSecondary,
                    onSecondary = Color.White,
                    secondaryContainer = CopperSecondaryContainer,
                    onSecondaryContainer = Color(0xFF4A1900),
                    tertiary = CedarTertiary,
                    onTertiary = Color.White,
                    tertiaryContainer = CedarTertiaryContainer,
                    onTertiaryContainer = Color(0xFFE8F6EE),
                    background = ManorBackground,
                    onBackground = Color(0xFF141920),
                    surface = ManorSurface,
                    onSurface = Color(0xFF141920),
                    surfaceVariant = Color(0xFFDCE3ED),
                    onSurfaceVariant = Color(0xFF3F4854),
                    surfaceContainerHigh = Color(0xFFDEE5EF),
                    surfaceContainerLowest = Color(0xFFFFFFFF),
                    outline = Color(0xFF6E7887),
                    outlineVariant = Color(0xFFC0CAD7)
                )
            }
        }

        AppThemePalette.FRENCH_LAVENDER -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFFDCB8EF),
                    onPrimary = Color(0xFF412250),
                    primaryContainer = Color(0xFF573766),
                    onPrimaryContainer = Color(0xFFF5E0FF),
                    secondary = Color(0xFFF6BD58),
                    onSecondary = Color(0xFF452C00),
                    secondaryContainer = Color(0xFF644300),
                    onSecondaryContainer = Color(0xFFFFDEA3),
                    tertiary = Color(0xFFA6D1B1),
                    onTertiary = Color(0xFF10361F),
                    tertiaryContainer = Color(0xFF274D34),
                    onTertiaryContainer = Color(0xFFC2ECD0),
                    background = Color(0xFF18121C),
                    onBackground = Color(0xFFEBE0EE),
                    surface = Color(0xFF18121C),
                    onSurface = Color(0xFFEBE0EE),
                    surfaceVariant = Color(0xFF2F2435),
                    onSurfaceVariant = Color(0xFFCBC0D2),
                    surfaceContainerHigh = Color(0xFF261D2E),
                    surfaceContainerLowest = Color(0xFF100B13),
                    outline = Color(0xFF95889C),
                    outlineVariant = Color(0xFF4C4052)
                )
            } else {
                lightColorScheme(
                    primary = LavenderPrimary,
                    onPrimary = Color.White,
                    primaryContainer = LavenderPrimaryContainer,
                    onPrimaryContainer = Color(0xFFFCF5FF),
                    secondary = OchreSecondary,
                    onSecondary = Color.White,
                    secondaryContainer = OchreSecondaryContainer,
                    onSecondaryContainer = Color(0xFF402800),
                    tertiary = RosemaryTertiary,
                    onTertiary = Color.White,
                    tertiaryContainer = RosemaryTertiaryContainer,
                    onTertiaryContainer = Color(0xFFF0FDF3),
                    background = ProvenceBackground,
                    onBackground = Color(0xFF201624),
                    surface = ProvenceSurface,
                    onSurface = Color(0xFF201624),
                    surfaceVariant = Color(0xFFEAE0F0),
                    onSurfaceVariant = Color(0xFF4C4152),
                    surfaceContainerHigh = Color(0xFFEDE0F2),
                    surfaceContainerLowest = Color(0xFFFFFFFF),
                    outline = Color(0xFF7D7084),
                    outlineVariant = Color(0xFFCDC1D5)
                )
            }
        }

        AppThemePalette.ROSEMARY_SAGE -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFF90D6A2),
                    onPrimary = Color(0xFF00391A),
                    primaryContainer = Color(0xFF184E29),
                    onPrimaryContainer = Color(0xFFABF2BC),
                    secondary = Color(0xFFF6BE48),
                    onSecondary = Color(0xFF442F00),
                    secondaryContainer = Color(0xFF624500),
                    onSecondaryContainer = Color(0xFFFFDE9F),
                    tertiary = Color(0xFFFAB1C4),
                    onTertiary = Color(0xFF51182A),
                    tertiaryContainer = Color(0xFF6A2F40),
                    onTertiaryContainer = Color(0xFFFFD9E2),
                    background = Color(0xFF0F1711),
                    onBackground = Color(0xFFDFE6E1),
                    surface = Color(0xFF0F1711),
                    onSurface = Color(0xFFDFE6E1),
                    surfaceVariant = Color(0xFF243026),
                    onSurfaceVariant = Color(0xFFBFCBC1),
                    surfaceContainerHigh = Color(0xFF1A261D),
                    surfaceContainerLowest = Color(0xFF0A0F0B),
                    outline = Color(0xFF89958C),
                    outlineVariant = Color(0xFF404C42)
                )
            } else {
                lightColorScheme(
                    primary = BotanicalPrimary,
                    onPrimary = Color.White,
                    primaryContainer = BotanicalPrimaryContainer,
                    onPrimaryContainer = Color(0xFFF2FFF4),
                    secondary = HoneycombSecondary,
                    onSecondary = Color.White,
                    secondaryContainer = HoneycombSecondaryContainer,
                    onSecondaryContainer = Color(0xFF402D00),
                    tertiary = MulberryTertiary,
                    onTertiary = Color.White,
                    tertiaryContainer = MulberryTertiaryContainer,
                    onTertiaryContainer = Color(0xFFFFECEF),
                    background = BotanicalBackground,
                    onBackground = Color(0xFF131D16),
                    surface = BotanicalSurface,
                    onSurface = Color(0xFF131D16),
                    surfaceVariant = Color(0xFFD6E4D9),
                    onSurfaceVariant = Color(0xFF3F4D43),
                    surfaceContainerHigh = Color(0xFFDCEBDD),
                    surfaceContainerLowest = Color(0xFFFFFFFF),
                    outline = Color(0xFF6F7E73),
                    outlineVariant = Color(0xFFBCCBC0)
                )
            }
        }

        AppThemePalette.MIDNIGHT_HEARTH -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFFFF8E72),
                    onPrimary = Color(0xFF551909),
                    primaryContainer = Color(0xFF752B18),
                    onPrimaryContainer = Color(0xFFFFDBD1),
                    secondary = Color(0xFFF9C052),
                    onSecondary = Color(0xFF422C00),
                    secondaryContainer = Color(0xFF5F4100),
                    onSecondaryContainer = Color(0xFFFFDE9F),
                    tertiary = Color(0xFFA1D0AF),
                    onTertiary = Color(0xFF08381D),
                    tertiaryContainer = Color(0xFF224F33),
                    onTertiaryContainer = Color(0xFFBCEDCA),
                    background = CastIronBackground,
                    onBackground = Color(0xFFECE0DC),
                    surface = CastIronSurface,
                    onSurface = Color(0xFFECE0DC),
                    surfaceVariant = Color(0xFF2C2320),
                    onSurfaceVariant = Color(0xFFD1C2BD),
                    surfaceContainerHigh = Color(0xFF382D29),
                    surfaceContainerLowest = Color(0xFF120E0C),
                    outline = Color(0xFF9B8C86),
                    outlineVariant = Color(0xFF4F433E)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFB54327),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFD35E42),
                    onPrimaryContainer = Color(0xFFFFFBF9),
                    secondary = Color(0xFF996A00),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFF5BD4A),
                    onSecondaryContainer = Color(0xFF493000),
                    tertiary = Color(0xFF386045),
                    onTertiary = Color.White,
                    tertiaryContainer = Color(0xFF4E7B5C),
                    onTertiaryContainer = Color(0xFFF0FBF3),
                    background = Color(0xFFFAF7F5),
                    onBackground = Color(0xFF201B18),
                    surface = Color(0xFFFAF7F5),
                    onSurface = Color(0xFF201B18),
                    surfaceVariant = Color(0xFFE7DDD8),
                    onSurfaceVariant = Color(0xFF4E4440),
                    surfaceContainerHigh = Color(0xFFECE1DC),
                    surfaceContainerLowest = Color(0xFFFFFFFF),
                    outline = Color(0xFF81746F),
                    outlineVariant = Color(0xFFD0C3BE)
                )
            }
        }
    }
}

@Composable
fun HeirloomTheme(
    palette: AppThemePalette = AppThemePalette.HEARTH_TERRACOTTA,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> if (palette.isDarkDefault) true else isSystemInDarkTheme()
    },
    content: @Composable () -> Unit
) {
    val colorScheme = getPaletteColorScheme(palette, darkTheme)

    CompositionLocalProvider(
        LocalHeirloomPalette provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Backward compatibility overloads for existing tests and composables
@Composable
fun HeirloomKitchenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    HeirloomTheme(
        palette = AppThemePalette.HEARTH_TERRACOTTA,
        darkTheme = darkTheme,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    HeirloomTheme(
        palette = AppThemePalette.HEARTH_TERRACOTTA,
        darkTheme = darkTheme,
        content = content
    )
}
