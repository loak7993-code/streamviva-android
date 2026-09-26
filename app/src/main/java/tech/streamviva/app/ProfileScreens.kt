package tech.streamviva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AVATARS = listOf("🎬", "🍿", "😎", "🐱", "🔥", "🌙", "⭐", "🎮", "🦊", "🎭", "👻", "🎧")
val GRADIENTS = listOf(
    "#8D6BE0" to "#5B3FA8",
    "#D9A44E" to "#8A5D24",
    "#D96B8F" to "#8A3B55",
    "#4EB8D9" to "#2E6B8A",
    "#8FD94E" to "#558A2E",
    "#D96E4E" to "#8A3E2E",
)

fun hexColor(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

/* ========================= who's watching? ========================= */

@Composable
fun ProfilePickerScreen(
    onPick: () -> Unit,
    onManage: () -> Unit,
) {
    var manageMode by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(0f to Color(0xFF161022), 0.5f to Bg, 1f to BgDeep)
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(90.dp))
            Wordmark(size = 30)
            Spacer(Modifier.height(46.dp))
            Text(
                if (manageMode) "Manage profiles" else "Who's watching?",
                color = White,
                fontSize = 26.sp,
                fontFamily = Serif,
            )
            Spacer(Modifier.height(44.dp))

            // tiles grid
            Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                Store.profiles.chunked(2).forEach { rowProfiles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        rowProfiles.forEach { p -> ProfileTile(p, manageMode, onPick, onManage) }
                        if (rowProfiles.size == 1 && !manageMode && Store.profiles.size < 5) {
                            AddTile(onManage)
                        }
                    }
                }
            }

            Spacer(Modifier.height(50.dp))

            if (manageMode) {
                Button(
                    onClick = { manageMode = false },
                    colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Bg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) { Text("Done", fontSize = 14.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold) }
            } else {
                Text(
                    "manage profiles",
                    color = Text3,
                    fontSize = 13.sp,
                    fontFamily = Sans,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { manageMode = true }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(70.dp))
        }
    }
}

@Composable
fun ProfileTile(p: Store.Profile, manageMode: Boolean, onPick: () -> Unit, onEdit: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(120.dp),
    ) {
        Box {
            AvatarView(avatar = p.icon, size = 110, modifier = Modifier.size(110.dp))
            if (manageMode) {
                Box(
                    Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x66000000))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Edit, null, tint = White, modifier = Modifier.size(30.dp))
                }
            } else {
                Box(Modifier.size(110.dp).clickable { Store.switchProfile(p.id); onPick() })
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            p.name,
            color = if (p.id == Store.activeProfileId) White else Text2,
            fontSize = 14.sp,
            fontFamily = Sans,
            fontWeight = if (p.id == Store.activeProfileId) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
fun AddTile(onAdd: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(120.dp),
    ) {
        Box(
            Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x14FFFFFF))
                .clickable { onAdd() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, null, tint = Text3, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text("Add profile", color = Text3, fontSize = 14.sp, fontFamily = Sans)
    }
}

/* ========================= profile editor ========================= */

@Composable
fun ProfileEditorScreen(
    profile: Store.Profile?,
    isNew: Boolean,
    onDone: () -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var icon by remember { mutableStateOf(profile?.icon ?: "🎬") }
    var colors by remember { mutableStateOf(profile?.let { it.colorA to it.colorB } ?: GRADIENTS.first()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val canDelete = Store.profiles.size > 1 && !isNew

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Kicker(if (isNew) "new profile" else "edit profile")
        Spacer(Modifier.height(18.dp))

        // preview
        AvatarView(avatar = icon, size = 96, modifier = Modifier.size(96.dp))

        Spacer(Modifier.height(22.dp))

        // name
        Kicker("name", color = Text3)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .padding(14.dp),
        ) {
            if (name.isEmpty()) Text("profile name", color = Text3, fontSize = 14.5.sp, fontFamily = Sans)
            BasicTextField(
                value = name,
                onValueChange = { if (it.length <= 20) name = it },
                textStyle = TextStyle(color = White, fontSize = 14.5.sp, fontFamily = Sans),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        Spacer(Modifier.height(22.dp))

        // avatar
        var avatarTab by remember { mutableStateOf(0) } // 0 presets, 1 emoji, 2 photo
        Kicker("avatar", color = Text3)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("characters", "emoji", "photo").forEachIndexed { i, label ->
                Text(
                    label,
                    color = if (avatarTab == i) White else Text3,
                    fontSize = 12.sp,
                    fontFamily = Sans,
                    fontWeight = if (avatarTab == i) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (avatarTab == i) Store.accent.copy(alpha = 0.22f) else Surface)
                        .clickable { avatarTab = i }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        when (avatarTab) {
            0 -> LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().height(190.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(FACE_COUNT) { i ->
                    val key = "face:$i"
                    AvatarView(
                        avatar = key, size = 52,
                        modifier = Modifier
                            .size(52.dp)
                            .border(
                                2.dp,
                                if (icon == key) White else Color.Transparent,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { icon = key },
                    )
                }
            }
            1 -> LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxWidth().height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(AVATARS) { a ->
                    val key = "emoji:$a"
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (icon == key) Store.accent.copy(alpha = 0.25f) else Surface)
                            .border(
                                1.dp,
                                if (icon == key) Store.accent else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { icon = key },
                        contentAlignment = Alignment.Center,
                    ) { Text(a, fontSize = 20.sp) }
                }
            }
            2 -> {
                // photo picker
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val picker = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    uri?.let {
                        try {
                            val dir = java.io.File(ctx.filesDir, "avatars").apply { mkdirs() }
                            val file = java.io.File(dir, "p${System.currentTimeMillis()}.jpg")
                            ctx.contentResolver.openInputStream(it)?.use { input ->
                                java.io.FileOutputStream(file).use { output -> input.copyTo(output) }
                            }
                            icon = "photo:avatars/${file.name}"
                        } catch (_: Exception) {}
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (icon.startsWith("photo:")) {
                        AvatarView(avatar = icon, size = 96)
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            picker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Store.accent, contentColor = White),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (icon.startsWith("photo:")) "pick another photo" else "choose from gallery", fontSize = 13.sp, fontFamily = Sans)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("photos stay on this device", color = Text3, fontSize = 10.5.sp, fontFamily = Sans)
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // colors
        Kicker("profile color", color = Text3)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GRADIENTS.forEach { (a, b) ->
                val selected = colors.first == a
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(hexColor(a), hexColor(b))))
                        .border(
                            2.dp,
                            if (selected) White else Color.Transparent,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { colors = a to b },
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = {
                val finalName = name.trim().ifEmpty { "Profile" }
                val p = Store.Profile(
                    id = profile?.id ?: "p${System.currentTimeMillis()}",
                    name = finalName, icon = icon,
                    colorA = colors.first, colorB = colors.second,
                )
                if (isNew) Store.saveProfile(p) else Store.saveProfile(p)
                if (isNew) Store.switchProfile(p.id)
                onDone()
            },
            enabled = true,
            colors = ButtonDefaults.buttonColors(containerColor = Store.accent, contentColor = White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text(if (isNew) "Create profile" else "Save changes", fontSize = 14.5.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold) }

        if (canDelete) {
            Spacer(Modifier.height(12.dp))
            if (!confirmDelete) {
                Text(
                    "delete profile",
                    color = Text3,
                    fontSize = 13.sp,
                    fontFamily = Sans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { confirmDelete = true }
                        .padding(10.dp),
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            profile?.let { Store.deleteProfile(it.id) }
                            onDelete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose, contentColor = White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                    ) { Text("Yes, delete", fontSize = 13.sp, fontFamily = Sans) }
                    OutlinedButton(
                        onClick = { confirmDelete = false },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Text2),
                        modifier = Modifier.weight(1f).height(44.dp),
                    ) { Text("Cancel", fontSize = 13.sp, fontFamily = Sans) }
                }
            }
        }

        Spacer(Modifier.height(30.dp))
        Text(
            if (Store.session != null) "changes sync to your account" else "profiles live on this device",
            color = Text3, fontSize = 11.sp, fontFamily = Sans,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(60.dp))
    }
}
