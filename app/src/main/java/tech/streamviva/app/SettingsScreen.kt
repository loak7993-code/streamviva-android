package tech.streamviva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    onSwitchProfile: (String) -> Unit,
    onEditProfile: (Store.Profile) -> Unit,
    onAddProfile: () -> Unit,
) {
    val session = Store.session
    var me by remember { mutableStateOf<org.json.JSONObject?>(null) }

    LaunchedEffect(session?.token) {
        val t = session?.token ?: return@LaunchedEffect
        me = Auth.me(t)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back",
                tint = Text2,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(6.dp),
            )
            Spacer(Modifier.width(14.dp))
            Wordmark(size = 21)
            Spacer(Modifier.width(10.dp))
            Kicker("settings", color = Text2)
        }

        /* ------------------------- account ------------------------- */
        Spacer(Modifier.height(14.dp))
        SettingsGroup("Account") {
            if (session != null) {
                // profile circle with account color
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarView(avatar = Store.active.icon, size = 46, modifier = Modifier.size(46.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            me?.getJSONObject("user")?.optString("nickname")?.takeIf { it.isNotBlank() }
                                ?: "StreamViva member",
                            color = Text1, fontSize = 15.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "ID ${session.userId.take(8)} · ${session.device}",
                            color = Text3, fontSize = 11.5.sp, fontFamily = Sans,
                        )
                        if (me != null) {
                            Text(
                                "synced with streamviva.app",
                                color = IrisSoft, fontSize = 11.sp, fontFamily = Sans,
                            )
                        }
                    }
                }
                SettingsAction("Sign out", danger = false) {
                    Store.clearSession()
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "You're browsing as a guest",
                        color = Text1, fontSize = 14.5.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Your library lives only on this device. Create an account to keep it forever, synced with the website.",
                        color = Text3, fontSize = 12.sp, fontFamily = Sans, lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onSignUp,
                            colors = ButtonDefaults.buttonColors(containerColor = Iris, contentColor = White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(42.dp),
                        ) { Text("Sign up", fontSize = 13.5.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold) }
                        OutlinedButton(
                            onClick = onLogin,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Text1),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceLine),
                            modifier = Modifier.weight(1f).height(42.dp),
                        ) { Text("Sign in", fontSize = 13.5.sp, fontFamily = Sans) }
                    }
                }
            }
        }

        /* ------------------------- profiles ------------------------- */
        Spacer(Modifier.height(20.dp))
        SettingsGroup("Profiles") {
            // current + all profiles quick switch
            Store.profiles.forEach { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSwitchProfile(p.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarView(avatar = p.icon, size = 38, modifier = Modifier.size(38.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        p.name + if (p.id == Store.activeProfileId) "  ·" else "",
                        color = if (p.id == Store.activeProfileId) Store.accent else Text1,
                        fontSize = 14.sp,
                        fontFamily = Sans,
                        fontWeight = if (p.id == Store.activeProfileId) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "edit",
                        tint = Text3,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onEditProfile(p) }
                            .padding(6.dp)
                            .size(15.dp),
                    )
                }
            }
            HorizontalLine()
            SettingsAction("Add profile") { onAddProfile() }
            SettingsToggle(
                "Skip profile picker",
                "jump straight into the last profile",
                checked = Store.skipProfilePicker,
            ) { Store.updateSkipProfilePicker(it) }
        }

        /* ------------------------- appearance ------------------------- */
        Spacer(Modifier.height(20.dp))
        SettingsGroup("Appearance") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Accent color", color = Text1, fontSize = 14.sp, fontFamily = Sans)
                Text("tints buttons, progress bars, highlights", color = Text3, fontSize = 11.5.sp, fontFamily = Sans)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Store.ACCENTS.forEach { (name, hexes) ->
                        val selected = Store.accentHex.equals(hexes.first, ignoreCase = true)
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.verticalGradient(listOf(hexColor(hexes.first), hexColor(hexes.second))))
                                .border(
                                    2.dp,
                                    if (selected) White else Color.Transparent,
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { Store.setAccent(hexes.first, hexes.second) },
                        )
                    }
                }
            }
        }

        /* ------------------------- playback ------------------------- */
        Spacer(Modifier.height(20.dp))
        SettingsGroup("Playback") {
            SettingsToggle(
                "Autoplay next episode",
                "start the next episode when one ends",
                checked = Store.autoplayNext,
            ) { Store.updateAutoplayNext(it) }
            SettingsToggle(
                "Show ratings",
                "star scores on posters and rows",
                checked = Store.showRatings,
            ) { Store.updateShowRatings(it) }
        }

        /* ------------------------- data ------------------------- */
        Spacer(Modifier.height(20.dp))
        SettingsGroup("Your data") {
            SettingsStat("In progress", "${Store.progress.size}")
            SettingsStat("Favorites", "${Store.favorites.size}")
            SettingsStat("Watch history", "${Store.history.size}")
            HorizontalLine()
            SettingsAction("Clear continue watching") {
                Store.clearAllProgress()
            }
            SettingsAction("Clear watch history") {
                Store.clearHistory()
            }
            SettingsAction("Clear favorites") {
                Store.clearFavorites()
            }
        }

        /* ------------------------- about ------------------------- */
        Spacer(Modifier.height(20.dp))
        SettingsGroup("About") {
            SettingsStat("Version", "1.6.0")
            SettingsStat("Streams", "vidsrc universal · 3 mirrors")
            SettingsStat("Account backend", "streamviva\n.satisfying-discovery\n.workers.dev")
            SettingsStat("Made with", "Kotlin · Compose · ExoPlayer")
        }

        Spacer(Modifier.height(90.dp))
    }
}

/* --------------------------- settings parts --------------------------- */

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 14.dp)) {
        Kicker(title, color = Text3)
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface.copy(alpha = 0.6f)),
        ) { content() }
    }
}

@Composable
fun SettingsStat(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Text1, fontSize = 14.sp, fontFamily = Sans, modifier = Modifier.weight(1f))
        Text(value, color = Text3, fontSize = 12.5.sp, fontFamily = Sans, textAlign = TextAlign2)
    }
}

private val TextAlign2 = androidx.compose.ui.text.style.TextAlign.End

@Composable
fun SettingsToggle(label: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Text1, fontSize = 14.sp, fontFamily = Sans)
            Text(desc, color = Text3, fontSize = 11.5.sp, fontFamily = Sans)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = Iris,
                uncheckedThumbColor = Text2,
                uncheckedTrackColor = SurfaceHi,
            ),
        )
    }
}

@Composable
fun SettingsAction(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Text(
            label,
            color = if (danger) Rose else Text1,
            fontSize = 14.sp,
            fontFamily = Sans,
        )
    }
}

@Composable
fun HorizontalLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(SurfaceLine.copy(alpha = 0.5f)),
    )
}
