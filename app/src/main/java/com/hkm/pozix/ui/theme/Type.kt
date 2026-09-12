package com.hkm.pozix.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hkm.pozix.R

// Default system font
val DefaultFontFamily = FontFamily.Default

// Custom fonts
val BalooBhai2FontFamily = FontFamily(Font(R.font.baloo_bhai2_regular))
val KufamFontFamily = FontFamily(Font(R.font.kufam_regular))
val NunitoFontFamily = FontFamily(Font(R.font.nunito_regular))
val QuicksandFontFamily = FontFamily(Font(R.font.quicksand_regular))
val EBGaramondFontFamily = FontFamily(Font(R.font.eb_garamond_regular))
val SpaceMonoFontFamily = FontFamily(Font(R.font.space_mono_regular))
val ComicReliefFontFamily = FontFamily(Font(R.font.comic_relief_regular))
val UbuntuFontFamily = FontFamily(Font(R.font.ubuntu_regular))
val LibertinusSerifFontFamily = FontFamily(Font(R.font.libertinus_serif_regular))
val AlataFontFamily = FontFamily(Font(R.font.alata_regular))
val AleoFontFamily = FontFamily(Font(R.font.aleo_regular))
val ChangaFontFamily = FontFamily(Font(R.font.changa_regular))
val TenorSansFontFamily = FontFamily(Font(R.font.tenor_sans_regular))

/**
 * Get FontFamily by name
 * Returns Default if custom font is not available
 */
fun getFontFamily(fontName: String): FontFamily {
    return when (fontName) {
        "default" -> DefaultFontFamily
        "baloo_bhai2" -> BalooBhai2FontFamily
        "kufam" -> KufamFontFamily
        "nunito" -> NunitoFontFamily
        "quicksand" -> QuicksandFontFamily
        "eb_garamond" -> EBGaramondFontFamily
        "space_mono" -> SpaceMonoFontFamily
        "comic_relief" -> ComicReliefFontFamily
        "ubuntu" -> UbuntuFontFamily
        "libertinus_serif" -> LibertinusSerifFontFamily
        "alata" -> AlataFontFamily
        "aleo" -> AleoFontFamily
        "changa" -> ChangaFontFamily
        "tenor_sans" -> TenorSansFontFamily
        else -> DefaultFontFamily
    }
}

/**
 * Get display name for font
 */
fun getFontDisplayName(fontName: String): String {
    return when (fontName) {
        "default" -> "System Default"
        "baloo_bhai2" -> "Baloo Bhai 2"
        "kufam" -> "Kufam"
        "nunito" -> "Nunito"
        "quicksand" -> "Quicksand"
        "eb_garamond" -> "EB Garamond"
        "space_mono" -> "Space Mono"
        "comic_relief" -> "Comic Relief"
        "ubuntu" -> "Ubuntu"
        "libertinus_serif" -> "Libertinus Serif"
        "alata" -> "Alata"
        "aleo" -> "Aleo"
        "changa" -> "Changa"
        "tenor_sans" -> "Tenor Sans"
        else -> "System Default"
    }
}

/**
 * Get all available fonts
 */
fun getAvailableFonts(): List<String> {
    return listOf(
        "default",
        "baloo_bhai2",
        "kufam",
        "nunito",
        "quicksand",
        "eb_garamond",
        "space_mono",
        "comic_relief",
        "ubuntu",
        "libertinus_serif",
        "alata",
        "aleo",
        "changa",
        "tenor_sans"
    )
}

/**
 * Create Typography with specified font family
 */
fun createTypography(fontFamily: FontFamily = DefaultFontFamily): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

// Default Typography instance
val Typography = createTypography()
