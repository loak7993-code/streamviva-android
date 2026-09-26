package tech.streamviva.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

const val IMG = "https://image.tmdb.org/t/p/w500"
const val IMG_W780 = "https://image.tmdb.org/t/p/w780"

/* ------------------------------ wordmark ------------------------------ */

@Composable
fun Wordmark(size: Int = 30, alignBottom: Boolean = true) {
    Row(verticalAlignment = if (alignBottom) Alignment.Bottom else Alignment.CenterVertically) {
        Text("Stream", color = White, fontSize = size.sp, fontFamily = Serif, lineHeight = size.sp * 1.05)
        Text(
            "Viva",
            color = IrisSoft,
            fontSize = size.sp,
            fontFamily = Serif,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            lineHeight = size.sp * 1.05,
        )
    }
}

@Composable
fun Kicker(text: String, color: Color = IrisSoft) {
    Text(
        text.uppercase(),
        color = color,
        fontSize = 10.5.sp,
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.8.sp,
    )
}

/* ------------------------------ sections ------------------------------ */

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(15.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Iris)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                title,
                color = White,
                fontSize = 17.sp,
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    subtitle,
                    color = Text3,
                    fontSize = 11.sp,
                    fontFamily = Sans,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/* ------------------------------- cards -------------------------------- */

@Composable
fun PosterCard(m: Tmdb.Media, onOpen: (Tmdb.Media) -> Unit, wide: Boolean = false) {
    val w = if (wide) 168.dp else 128.dp
    val h = if (wide) 252.dp else 192.dp
    Column(
        Modifier
            .width(w)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .clickable { onOpen(m) },
    ) {
        Box {
            AsyncImage(
                model = m.poster?.let { IMG + it } ?: R.drawable.placeholder,
                contentDescription = m.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(h * 0.78f),
            )
            if (m.rating > 0) {
                Row(
                    Modifier
                        .padding(7.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xB30D0D10))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "%.1f".format(m.rating),
                        color = White, fontSize = 10.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                m.title,
                color = Text1,
                fontSize = 12.5.sp,
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                "${m.year} · ${if (m.type == "tv") "show" else "film"}",
                color = Text3,
                fontSize = 10.5.sp,
                fontFamily = Sans,
            )
        }
    }
}

/* ------------------------------ skeletons ------------------------------ */

@Composable
fun Shimmer(modifier: Modifier, radius: Int = 12) {
    val t = rememberInfiniteTransition(label = "sh")
    val a by t.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "a",
    )
    Box(modifier.clip(RoundedCornerShape(radius.dp)).background(SurfaceHi.copy(alpha = a)))
}

@Composable
fun CardRowSkeleton(wide: Boolean = false) {
    val w = if (wide) 168.dp else 128.dp
    val h = if (wide) 252.dp else 192.dp
    Row(
        Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(3) {
            Column {
                Shimmer(Modifier.size(width = w, height = h))
                Spacer(Modifier.height(8.dp))
                Shimmer(Modifier.size(width = w * 0.7f, height = 11.dp), radius = 4)
            }
        }
    }
}

@Composable
fun HomeSkeleton() {
    Column {
        Spacer(Modifier.height(230.dp))
        Shimmer(Modifier.padding(horizontal = 20.dp).size(width = 140.dp, height = 18.dp), radius = 6)
        Spacer(Modifier.height(16.dp))
        CardRowSkeleton()
        Spacer(Modifier.height(26.dp))
        Shimmer(Modifier.padding(horizontal = 20.dp).size(width = 160.dp, height = 18.dp), radius = 6)
        Spacer(Modifier.height(16.dp))
        CardRowSkeleton()
    }
}

/* ------------------------------ gradients ------------------------------ */

fun bottomFade(background: Color = Bg): Brush = Brush.verticalGradient(
    0f to Color(0x550D0D10),
    0.4f to Color.Transparent,
    0.75f to background.copy(alpha = 0.85f),
    1f to background,
)
