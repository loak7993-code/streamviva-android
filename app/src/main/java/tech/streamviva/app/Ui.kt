package tech.streamviva.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

const val IMG = "https://image.tmdb.org/t/p/w342"
const val IMG_DETAIL = "https://image.tmdb.org/t/p/w500"
const val IMG_W780 = "https://image.tmdb.org/t/p/w780"

/* ------------------------------ brand ------------------------------ */

@Composable
fun Wordmark(size: Int = 26, alignBottom: Boolean = true) {
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
        fontSize = 10.sp,
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.6.sp,
    )
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            title,
            color = White,
            fontSize = 14.5.sp,
            fontFamily = Sans,
            fontWeight = FontWeight.SemiBold,
        )
        if (subtitle != null) {
            Spacer(Modifier.width(8.dp))
            Text(subtitle, color = Text3, fontSize = 11.sp, fontFamily = Sans)
        }
    }
}

/* --------------------------- netflix cards --------------------------- */

/** compact poster card — netflix row density */
@Composable
fun CompactCard(m: Tmdb.Media, onOpen: (Tmdb.Media) -> Unit) {
    Column(
        Modifier
            .width(104.dp)
            .clickable { onOpen(m) },
    ) {
        AsyncImage(
            model = m.poster?.let { IMG + it } ?: R.drawable.placeholder,
            contentDescription = m.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Surface)
                .border(
                    1.dp,
                    Store.accent.copy(alpha = 0.35f),
                    RoundedCornerShape(6.dp),
                ),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            m.title,
            color = Text2,
            fontSize = 10.5.sp,
            fontFamily = Sans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** top-10 card — big outlined number + poster, the netflix signature */
@Composable
fun Top10Card(m: Tmdb.Media, rank: Int, onOpen: (Tmdb.Media) -> Unit) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .width(152.dp)
            .clickable { onOpen(m) },
    ) {
        Text(
            rank.toString(),
            color = Color.White,
            fontSize = 84.sp,
            fontFamily = Serif,
            lineHeight = 64.sp,
            style = androidx.compose.ui.text.TextStyle(
                drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.5f,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                )
            ),
            modifier = Modifier
                .padding(end = 0.dp)
                .offset(x = (-8).dp),
        )
        AsyncImage(
            model = m.poster?.let { IMG + it } ?: R.drawable.placeholder,
            contentDescription = m.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(96.dp)
                .height(144.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Surface),
        )
    }
}

/* ------------------------------ skeletons ------------------------------ */

@Composable
fun Shimmer(modifier: Modifier, radius: Int = 8) {
    val t = rememberInfiniteTransition(label = "sh")
    val a by t.animateFloat(
        initialValue = 0.28f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "a",
    )
    Box(modifier.clip(RoundedCornerShape(radius.dp)).background(SurfaceHi.copy(alpha = a)))
}

@Composable
fun RowSkeleton() {
    Row(
        Modifier.padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(4) {
            Column {
                Shimmer(Modifier.size(width = 104.dp, height = 156.dp), radius = 6)
                Spacer(Modifier.height(6.dp))
                Shimmer(Modifier.size(width = 70.dp, height = 9.dp), radius = 4)
            }
        }
    }
}

@Composable
fun HomeSkeleton() {
    Column {
        Spacer(Modifier.height(300.dp))
        Shimmer(Modifier.padding(horizontal = 12.dp).size(width = 130.dp, height = 14.dp), radius = 6)
        Spacer(Modifier.height(12.dp))
        RowSkeleton()
        Spacer(Modifier.height(24.dp))
        Shimmer(Modifier.padding(horizontal = 12.dp).size(width = 150.dp, height = 14.dp), radius = 6)
        Spacer(Modifier.height(12.dp))
        RowSkeleton()
    }
}

/* ------------------------------ gradients ------------------------------ */

fun bottomFade(background: Color = Bg): Brush = Brush.verticalGradient(
    0f to Color(0x550D0D10),
    0.4f to Color.Transparent,
    0.75f to background.copy(alpha = 0.85f),
    1f to background,
)
