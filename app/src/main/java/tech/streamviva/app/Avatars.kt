package tech.streamviva.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * Netflix-style preset avatars, drawn natively:
 * bold flat backgrounds + geometric faces.
 */

data class FaceDef(val bg: Color, val fg: Color, val kind: Int)

private val FACE_BG = listOf(
    Color(0xFFE50914), Color(0xFF221F1F), Color(0xFF1F80E0), Color(0xFFE87C21),
    Color(0xFF2D9E5B), Color(0xFFB565A7), Color(0xFFE0B31F), Color(0xFF1FA9A0),
    Color(0xFFD65A8A), Color(0xFF7A5AF8), Color(0xFF88B04B), Color(0xFFE05C2E),
)

const val FACE_COUNT = 12

fun faceDef(index: Int): FaceDef = FaceDef(
    bg = FACE_BG[index % FACE_BG.size],
    fg = Color(0xFFF5F5F0),
    kind = index % 12,
)

/** avatar string formats: "emoji:🎬" | "face:3" | "photo:avatars/xxx.jpg" | plain emoji (legacy) */
@Composable
fun AvatarView(avatar: String, size: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .size(size.dp)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            avatar.startsWith("photo:") -> {
                val f = File(appContext().filesDir, avatar.removePrefix("photo:"))
                AsyncImage(
                    model = f,
                    contentDescription = "profile photo",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            avatar.startsWith("face:") -> {
                val idx = avatar.removePrefix("face:").toIntOrNull() ?: 0
                val def = faceDef(idx)
                Box(Modifier.fillMaxSize().background(def.bg))
                Canvas(Modifier.fillMaxSize()) { drawFace(def) }
            }
            avatar.startsWith("emoji:") -> {
                Box(Modifier.fillMaxSize().background(Color(0xFF2A2A33)))
                androidx.compose.material3.Text(
                    avatar.removePrefix("emoji:"),
                    fontSize = (size * 0.42f).sp,
                )
            }
            else -> {
                // legacy plain emoji
                Box(Modifier.fillMaxSize().background(Color(0xFF2A2A33)))
                androidx.compose.material3.Text(avatar, fontSize = (size * 0.42f).sp)
            }
        }
    }
}

@Composable
private fun appContext() = androidx.compose.ui.platform.LocalContext.current

/* ------------------------- face drawing ------------------------- */

private fun DrawScope.drawFace(def: FaceDef) {
    val w = size.width
    val h = size.height
    val fg = def.fg
    val stroke = Stroke(width = w * 0.055f, cap = StrokeCap.Round)

    when (def.kind) {
        0 -> { // classic smile
            eyes(Offset(w * 0.34f, h * 0.42f), Offset(w * 0.66f, h * 0.42f), w * 0.045f, fg)
            arcMouth(Offset(w * 0.5f, h * 0.56f), w * 0.18f, fg, stroke)
        }
        1 -> { // sunglasses
            drawRoundRect(fg, Offset(w * 0.2f, h * 0.36f), Size(w * 0.26f, h * 0.14f), CornerRadius(w * 0.03f))
            drawRoundRect(fg, Offset(w * 0.54f, h * 0.36f), Size(w * 0.26f, h * 0.14f), CornerRadius(w * 0.03f))
            drawLine(fg, Offset(w * 0.46f, h * 0.42f), Offset(w * 0.54f, h * 0.42f), stroke.width)
            arcMouth(Offset(w * 0.5f, h * 0.58f), w * 0.16f, fg, stroke)
        }
        2 -> { // mustache
            eyes(Offset(w * 0.34f, h * 0.38f), Offset(w * 0.66f, h * 0.38f), w * 0.045f, fg)
            drawPath(
                Path().apply {
                    moveTo(w * 0.5f, h * 0.52f)
                    cubicTo(w * 0.42f, h * 0.46f, w * 0.28f, h * 0.5f, w * 0.24f, h * 0.56f)
                    cubicTo(w * 0.32f, h * 0.58f, w * 0.44f, h * 0.57f, w * 0.5f, h * 0.55f)
                    cubicTo(w * 0.56f, h * 0.57f, w * 0.68f, h * 0.58f, w * 0.76f, h * 0.56f)
                    cubicTo(w * 0.72f, h * 0.5f, w * 0.58f, h * 0.46f, w * 0.5f, h * 0.52f)
                    close()
                },
                fg,
            )
        }
        3 -> { // glasses
            drawCircle(fg, radius = w * 0.085f, center = Offset(w * 0.33f, h * 0.42f), style = stroke)
            drawCircle(fg, radius = w * 0.085f, center = Offset(w * 0.67f, h * 0.42f), style = stroke)
            drawLine(fg, Offset(w * 0.415f, h * 0.42f), Offset(w * 0.585f, h * 0.42f), stroke.width)
            arcMouth(Offset(w * 0.5f, h * 0.58f), w * 0.15f, fg, stroke)
        }
        4 -> { // crown
            drawPath(
                Path().apply {
                    moveTo(w * 0.26f, h * 0.5f)
                    lineTo(w * 0.26f, h * 0.3f)
                    lineTo(w * 0.38f, h * 0.4f)
                    lineTo(w * 0.5f, h * 0.26f)
                    lineTo(w * 0.62f, h * 0.4f)
                    lineTo(w * 0.74f, h * 0.3f)
                    lineTo(w * 0.74f, h * 0.5f)
                    close()
                },
                fg,
            )
            eyes(Offset(w * 0.36f, h * 0.6f), Offset(w * 0.64f, h * 0.6f), w * 0.04f, fg)
        }
        5 -> { // headphones
            drawArc(color = fg, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.16f, h * 0.18f), size = Size(w * 0.68f, h * 0.68f), style = stroke)
            drawRoundRect(fg, Offset(w * 0.13f, h * 0.46f), Size(w * 0.1f, h * 0.2f), CornerRadius(w * 0.02f))
            drawRoundRect(fg, Offset(w * 0.77f, h * 0.46f), Size(w * 0.1f, h * 0.2f), CornerRadius(w * 0.02f))
            eyes(Offset(w * 0.38f, h * 0.5f), Offset(w * 0.62f, h * 0.5f), w * 0.04f, fg)
        }
        6 -> { // cat ears
            drawPath(
                Path().apply {
                    moveTo(w * 0.24f, h * 0.42f); lineTo(w * 0.3f, h * 0.2f); lineTo(w * 0.42f, h * 0.34f); close()
                },
                fg,
            )
            drawPath(
                Path().apply {
                    moveTo(w * 0.76f, h * 0.42f); lineTo(w * 0.7f, h * 0.2f); lineTo(w * 0.58f, h * 0.34f); close()
                },
                fg,
            )
            eyes(Offset(w * 0.36f, h * 0.48f), Offset(w * 0.64f, h * 0.48f), w * 0.04f, fg)
            drawPath(
                Path().apply {
                    moveTo(w * 0.44f, h * 0.6f); lineTo(w * 0.5f, h * 0.64f); lineTo(w * 0.56f, h * 0.6f)
                },
                fg,
            )
            // whiskers
            drawLine(fg, Offset(w * 0.2f, h * 0.56f), Offset(w * 0.32f, h * 0.58f), stroke.width * 0.7f)
            drawLine(fg, Offset(w * 0.8f, h * 0.56f), Offset(w * 0.68f, h * 0.58f), stroke.width * 0.7f)
        }
        7 -> { // sleepy
            drawLine(fg, Offset(w * 0.28f, h * 0.42f), Offset(w * 0.4f, h * 0.42f), stroke.width)
            drawLine(fg, Offset(w * 0.6f, h * 0.42f), Offset(w * 0.72f, h * 0.42f), stroke.width)
            arcMouth(Offset(w * 0.5f, h * 0.56f), w * 0.12f, fg, stroke, startAngle = 20f)
        }
        8 -> { // blush
            drawCircle(Color(0x66FFFFFF), radius = w * 0.06f, center = Offset(w * 0.26f, h * 0.5f))
            drawCircle(Color(0x66FFFFFF), radius = w * 0.06f, center = Offset(w * 0.74f, h * 0.5f))
            eyes(Offset(w * 0.36f, h * 0.42f), Offset(w * 0.64f, h * 0.42f), w * 0.04f, fg)
            arcMouth(Offset(w * 0.5f, h * 0.55f), w * 0.14f, fg, stroke)
        }
        9 -> { // surprised
            eyes(Offset(w * 0.34f, h * 0.4f), Offset(w * 0.66f, h * 0.4f), w * 0.05f, fg)
            drawCircle(fg, radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.58f), style = stroke)
        }
        10 -> { // monocle + smile
            drawCircle(fg, radius = w * 0.09f, center = Offset(w * 0.64f, h * 0.4f), style = stroke)
            drawLine(fg, Offset(w * 0.7f, h * 0.34f), Offset(w * 0.78f, h * 0.26f), stroke.width * 0.8f)
            eyes(Offset(w * 0.34f, h * 0.4f), Offset(w * 0.34f, h * 0.4f), w * 0.045f, fg)
            arcMouth(Offset(w * 0.5f, h * 0.58f), w * 0.15f, fg, stroke)
        }
        else -> { // beard
            eyes(Offset(w * 0.34f, h * 0.38f), Offset(w * 0.66f, h * 0.38f), w * 0.045f, fg)
            drawPath(
                Path().apply {
                    moveTo(w * 0.28f, h * 0.5f)
                    cubicTo(w * 0.3f, h * 0.72f, w * 0.7f, h * 0.72f, w * 0.72f, h * 0.5f)
                    cubicTo(w * 0.6f, h * 0.62f, w * 0.4f, h * 0.62f, w * 0.28f, h * 0.5f)
                    close()
                },
                fg,
            )
        }
    }
}

private fun DrawScope.eyes(a: Offset, b: Offset, r: Float, color: Color) {
    drawCircle(color, radius = r, center = a)
    drawCircle(color, radius = r, center = b)
}

private fun DrawScope.arcMouth(center: Offset, radius: Float, color: Color, stroke: Stroke, startAngle: Float = 0f) {
    drawArc(
        color = color,
        startAngle = 20f + startAngle,
        sweepAngle = 140f - startAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = stroke,
    )
}
