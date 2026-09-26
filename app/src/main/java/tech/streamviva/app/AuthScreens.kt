package tech.streamviva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ============================== welcome ============================== */

@Composable
fun WelcomeScreen(
    onSignUp: () -> Unit,
    onLogin: () -> Unit,
    onGuest: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF151020),
                    0.5f to Bg,
                    1f to BgDeep,
                )
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(120.dp))
            Wordmark(size = 44)
            Spacer(Modifier.height(10.dp))
            Kicker("movies · shows · anime", color = Text2)
            Spacer(Modifier.height(16.dp))
            Text(
                "Your library, your progress,\nanywhere you sign in.",
                color = Text2,
                fontSize = 14.sp,
                fontFamily = Sans,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(60.dp))

            // primary: create account
            Button(
                onClick = onSignUp,
                colors = ButtonDefaults.buttonColors(containerColor = Iris, contentColor = White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text("Create account", fontSize = 15.5.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            // secondary: login
            OutlinedButton(
                onClick = onLogin,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Text1),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceLine),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text("I already have an account", fontSize = 15.5.sp, fontFamily = Sans)
            }

            Spacer(Modifier.height(28.dp))

            // guest below
            Text(
                "continue as guest →",
                color = Text3,
                fontSize = 13.5.sp,
                fontFamily = Sans,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onGuest() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "A guest library lives only on this device.\nAccounts sync across everything.",
                color = Color(0xFF4A4A54),
                fontSize = 11.sp,
                fontFamily = Sans,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(60.dp))
        }
    }
}

/* ============================== sign up ============================== */

@Composable
fun SignUpScreen(
    onDone: (mnemonic: String) -> Unit,
    onBack: () -> Unit,
) {
    var step by remember { mutableStateOf(1) }
    var mnemonic by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    // generate on entry
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        mnemonic = Auth.generateMnemonic(Auth.loadWordlist(context))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back",
                tint = Text2,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { if (step > 1) step-- else onBack() }
                    .padding(6.dp),
            )
            Spacer(Modifier.width(14.dp))
            Kicker(if (step == 1) "step 1 of 2 · your passphrase" else "step 2 of 2 · confirm")
        }

        Spacer(Modifier.height(24.dp))

        if (step == 1) {
            Text(
                "Your passphrase",
                color = White, fontSize = 28.sp, fontFamily = Serif,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "This is your account — there's no email and no password to reset. Save these 12 words somewhere safe. You'll need them to sign in on other devices.",
                color = Text2, fontSize = 13.sp, fontFamily = Sans, lineHeight = 19.sp,
            )
            Spacer(Modifier.height(26.dp))

            // word grid
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .border(1.dp, Iris.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                mnemonic.split(" ").chunked(2).forEachIndexed { rowIdx, pair ->
                    Row(Modifier.fillMaxWidth()) {
                        pair.forEachIndexed { i, word ->
                            val idx = rowIdx * 2 + i + 1
                            Row(
                                Modifier
                                    .weight(1f)
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    idx.toString(),
                                    color = Text3,
                                    fontSize = 11.sp,
                                    fontFamily = Sans,
                                    modifier = Modifier.width(24.dp),
                                )
                                Text(
                                    word,
                                    color = White,
                                    fontSize = 15.sp,
                                    fontFamily = Serif,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceHi)
                        .clickable { clipboard.setText(AnnotatedString(mnemonic)) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.ContentCopy, null, tint = Text2, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("copy", color = Text2, fontSize = 12.sp, fontFamily = Sans)
                }
            }

            Spacer(Modifier.height(26.dp))
            Button(
                onClick = { step = 2 },
                colors = ButtonDefaults.buttonColors(containerColor = Iris, contentColor = White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text("I've saved my passphrase", fontSize = 14.5.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold) }

        } else {
            Text(
                "Confirm your passphrase",
                color = White, fontSize = 28.sp, fontFamily = Serif,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Type your 12 words to prove you saved them. This creates your account.",
                color = Text2, fontSize = 13.sp, fontFamily = Sans, lineHeight = 19.sp,
            )
            Spacer(Modifier.height(24.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .padding(16.dp),
            ) {
                if (confirmed.isEmpty()) Text(
                    "word word word word word word word word word word word word",
                    color = Text3, fontSize = 14.sp, fontFamily = Sans,
                )
                BasicTextField(
                    value = confirmed,
                    onValueChange = { confirmed = it },
                    textStyle = TextStyle(color = White, fontSize = 14.sp, fontFamily = Sans, lineHeight = 22.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text("⚠ $error", color = Rose, fontSize = 12.5.sp, fontFamily = Sans)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val normalized = confirmed.trim().lowercase().replace(Regex("\\s+"), " ")
                    if (normalized == mnemonic) onDone(mnemonic)
                    else error = "passphrase doesn't match — check your words"
                },
                enabled = confirmed.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Iris, contentColor = White, disabledContainerColor = SurfaceHi, disabledContentColor = Text3),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text("Create my account", fontSize = 14.5.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.height(60.dp))
    }
}

/* =============================== login =============================== */

@Composable
fun LoginScreen(
    onDone: (mnemonic: String) -> Unit,
    onBack: () -> Unit,
) {
    var mnemonic by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
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
            Kicker("sign in")
        }

        Spacer(Modifier.height(24.dp))
        Text("Welcome back", color = White, fontSize = 28.sp, fontFamily = Serif)
        Spacer(Modifier.height(8.dp))
        Text(
            "Enter your 12-word passphrase to sign in.",
            color = Text2, fontSize = 13.sp, fontFamily = Sans,
        )
        Spacer(Modifier.height(24.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface)
                .padding(16.dp),
        ) {
            if (mnemonic.isEmpty()) Text(
                "your twelve word passphrase",
                color = Text3, fontSize = 14.sp, fontFamily = Sans,
            )
            BasicTextField(
                value = mnemonic,
                onValueChange = { mnemonic = it },
                textStyle = TextStyle(color = White, fontSize = 14.sp, fontFamily = Sans, lineHeight = 22.sp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text("⚠ $error", color = Rose, fontSize = 12.5.sp, fontFamily = Sans)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                busy = true; error = null
                onDone(mnemonic.trim().lowercase().replace(Regex("\\s+"), " "))
            },
            enabled = mnemonic.isNotBlank() && !busy,
            colors = ButtonDefaults.buttonColors(containerColor = Iris, contentColor = White, disabledContainerColor = SurfaceHi, disabledContentColor = Text3),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(color = White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text(if (busy) "signing in…" else "Sign in", fontSize = 14.5.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
        }
    }
}

/* ============================ account status ============================ */

@Composable
fun AccountCreatingScreen(onComplete: () -> Unit, onError: (String) -> Unit) {
    // handled by caller with LaunchedEffect; this is just a visual
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Iris, modifier = Modifier.size(34.dp), strokeWidth = 3.dp)
            Spacer(Modifier.height(18.dp))
            Text("creating your account…", color = Text2, fontSize = 13.sp, fontFamily = Sans)
        }
    }
}
