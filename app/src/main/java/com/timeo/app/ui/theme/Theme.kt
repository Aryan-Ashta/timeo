package com.timeo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.timeo.app.R

// Palette lifted from aryanashta.me: paper, ink, slate, rule and the project "plates".
val Paper = Color(0xFFEDEFEA)
val Ink = Color(0xFF1C2230)
val Slate = Color(0xFF5F6778)
val Rule = Color(0xFFD3D7D0)

val PlateColors = listOf(
    Color(0xFF2E7D5B), // green
    Color(0xFF2F5BD3), // blue
    Color(0xFFE3B448), // ochre
    Color(0xFFC4553A), // brick
    Color(0xFF7A4E9C), // plum
    Color(0xFF2D8C8C), // teal
    Color(0xFFD9798B), // rose
    Color(0xFF3B4252), // graphite
)
val PlateArgb: List<Int> = PlateColors.map { it.toArgb() }

/** Ink on light plates (ochre, rose), paper on dark ones. */
fun onPlate(plate: Color): Color = if (plate.luminance() > 0.4f) Ink else Paper

@Immutable
data class TimeoColors(
    val background: Color,
    val text: Color,
    val muted: Color,
    val rule: Color,
    val danger: Color,
)

private val LightTimeo = TimeoColors(Paper, Ink, Slate, Rule, Color(0xFFC4553A))
private val DarkTimeo = TimeoColors(Ink, Paper, Color(0xFF9AA1AE), Color(0xFF323A4C), Color(0xFFE07A5F))

val LocalTimeo = staticCompositionLocalOf { LightTimeo }

@OptIn(ExperimentalTextApi::class)
private fun archivo(weight: Int, width: Float = 100f) = Font(
    R.font.archivo,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.width(width),
    ),
)

@OptIn(ExperimentalTextApi::class)
private fun mono(weight: Int) = Font(
    R.font.jetbrains_mono,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Archivo = FontFamily(archivo(400), archivo(500), archivo(600), archivo(700))
/** Wide, heavy cut used for plate titles and big numbers. */
val ArchivoWide = FontFamily(archivo(800, 125f))
val ArchivoWordmark = FontFamily(archivo(700, 112f))
val Mono = FontFamily(mono(400), mono(500), mono(700))

object TimeoType {
    val display = TextStyle(fontFamily = ArchivoWide, fontWeight = FontWeight(800), fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-0.01).em)
    val plateTitle = TextStyle(fontFamily = ArchivoWide, fontWeight = FontWeight(800), fontSize = 30.sp, lineHeight = 32.sp, letterSpacing = (-0.01).em)
    val title = TextStyle(fontFamily = ArchivoWide, fontWeight = FontWeight(800), fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em)
    val wordmark = TextStyle(fontFamily = ArchivoWordmark, fontWeight = FontWeight(700), fontSize = 22.sp)
    val body = TextStyle(fontFamily = Archivo, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 24.sp)
    val bodyStrong = body.copy(fontWeight = FontWeight(600))
    val label = TextStyle(fontFamily = Archivo, fontWeight = FontWeight(400), fontSize = 12.sp, lineHeight = 16.sp)
    val labelStrong = label.copy(fontWeight = FontWeight(700))
    val nav = TextStyle(fontFamily = Archivo, fontWeight = FontWeight(400), fontSize = 15.sp)
    val clock = TextStyle(fontFamily = Mono, fontWeight = FontWeight(500), fontSize = 40.sp, letterSpacing = (-0.02).em)
    val monoSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight(500), fontSize = 13.sp)
}

@Composable
fun TimeoTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) DarkTimeo else LightTimeo
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.text, onPrimary = c.background, background = c.background, onBackground = c.text,
            surface = c.background, onSurface = c.text, surfaceVariant = c.rule, onSurfaceVariant = c.muted,
            surfaceContainerLow = c.background, surfaceContainer = c.background, surfaceContainerHigh = c.background,
            outline = c.muted, outlineVariant = c.rule, error = c.danger,
        )
    } else {
        lightColorScheme(
            primary = c.text, onPrimary = c.background, background = c.background, onBackground = c.text,
            surface = c.background, onSurface = c.text, surfaceVariant = c.rule, onSurfaceVariant = c.muted,
            surfaceContainerLow = c.background, surfaceContainer = c.background, surfaceContainerHigh = c.background,
            outline = c.muted, outlineVariant = c.rule, error = c.danger,
        )
    }
    val typography = MaterialTheme.typography.let { t ->
        t.copy(
            bodyLarge = TimeoType.body, bodyMedium = TimeoType.body.copy(fontSize = 14.sp),
            bodySmall = TimeoType.label, labelLarge = TimeoType.bodyStrong.copy(fontSize = 14.sp),
            labelMedium = TimeoType.label, labelSmall = TimeoType.label.copy(fontSize = 11.sp),
            titleLarge = TimeoType.title, titleMedium = TimeoType.bodyStrong, headlineSmall = TimeoType.title,
        )
    }
    CompositionLocalProvider(LocalTimeo provides c) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
