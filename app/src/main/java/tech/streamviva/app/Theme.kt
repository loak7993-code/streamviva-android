package tech.streamviva.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/* ---- silk palette ---- */
val Bg = Color(0xFF0D0D10)
val BgDeep = Color(0xFF0A0A0D)
val Surface = Color(0xFF16161B)
val SurfaceHi = Color(0xFF1E1E25)
val SurfaceLine = Color(0xFF24242C)
val Text1 = Color(0xFFCBCBD5)
val Text2 = Color(0xFF8E8E9B)
val Text3 = Color(0xFF5F5F6B)
// dynamic accents live in Store (customizable in settings)
val Iris get() = Store.accent
val IrisSoft get() = Store.accentSoft
val IrisDeep get() = Store.accentDeep
val White = Color(0xFFF8F8FB)
val Gold = Color(0xFFE5C77E)
val Rose = Color(0xFFE88383)

val Serif = FontFamily(
    Font(R.font.instrument_serif, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)
val Sans = FontFamily(
    Font(R.font.instrument_sans, FontWeight.Normal),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
)

private val SilkColors = darkColorScheme(
    primary = Iris,
    onPrimary = White,
    background = Bg,
    onBackground = Text1,
    surface = Surface,
    onSurface = Text1,
    surfaceVariant = SurfaceHi,
    onSurfaceVariant = Text2,
)

@Composable
fun SilkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SilkColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
