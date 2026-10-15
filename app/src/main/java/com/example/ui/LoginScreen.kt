package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties

private data class BaselineAccountItem(
    val shortLabel: String,
    val roleLabel: String,
    val username: String,
    val defaultPass: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val BaselineAccounts = listOf(
    BaselineAccountItem("Admin", "Admin Madrasah", "admin", "admin123", Icons.Outlined.Person),
    BaselineAccountItem("Kepsek", "Kepala Madrasah", "kepsek", "kepsek123", Icons.Outlined.School),
    BaselineAccountItem("Wali 1", "Wali Kelas 1", "wali1a", "wali123", Icons.Outlined.Groups),
    BaselineAccountItem("Wali 2", "Wali Kelas 2", "wali2a", "wali123", Icons.Outlined.Groups),
    BaselineAccountItem("Piket", "Guru Piket", "piket", "piket123", Icons.Outlined.FactCheck)
)

@Composable
fun LoginScreen(
    schoolName: String,
    academicYearLabel: String,
    loginError: String?,
    onLoginSubmit: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showUserOptions by remember { mutableStateOf(false) }
    var infoNotice by remember { mutableStateOf<String?>(null) }

    // Outer background with gradient 135deg (#dec3ff to #83c4ff)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFDEC3FF), // #dec3ff
                        Color(0xFF83C4FF)  // #83c4ff
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Login Container Card (.login-container: max-width 390px, border-radius 30px, bg rgba(255,255,255,0.94))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 390.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color(0x333A4FA0),
                        spotColor = Color(0x333A4FA0)
                    )
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xF0FFFFFF))
            ) {
                // Curved decorative header background (.login-container::before)
                // gradient 145deg (#c9a8ff to #72c7ff) with border-radius: 0 0 48% 12%
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w, 0f)
                        lineTo(w, h * 0.72f)
                        // Smooth organic curve matching CSS 0 0 48% 12%
                        quadraticBezierTo(
                            w * 0.46f, h * 1.14f,
                            0f, h * 0.88f
                        )
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFC9A8FF), // #c9a8ff
                                Color(0xFF72C7FF)  // #72c7ff
                            )
                        )
                    )
                }

                // Foreground Content (.login-content)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header (.header) - Phone icon removed
                    Text(
                        text = "Login",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "AbsensiQR Sekolah",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Form Fields
                    // 1. Username Input (.input-group)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val matchingAccounts = remember(username) {
                            if (username.isBlank()) {
                                BaselineAccounts
                            } else {
                                BaselineAccounts.filter {
                                    it.username.contains(username, ignoreCase = true) ||
                                    it.roleLabel.contains(username, ignoreCase = true) ||
                                    it.shortLabel.contains(username, ignoreCase = true)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                showUserOptions = true
                                if (loginError != null) onClearError()
                                if (infoNotice != null) infoNotice = null
                            },
                            placeholder = {
                                Text(
                                    text = "Username",
                                    fontSize = 14.sp,
                                    color = Color(0xFFA5B0C8)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Username",
                                    tint = Color(0xFF8999BD),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (matchingAccounts.isNotEmpty()) {
                                    IconButton(onClick = { showUserOptions = !showUserOptions }) {
                                        Icon(
                                            imageVector = if (showUserOptions) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                            contentDescription = "Pilihan Akun",
                                            tint = Color(0xFF8999BD)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(50.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF6676D9),
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color(0xFF34436B),
                                unfocusedTextColor = Color(0xFF34436B),
                                cursorColor = Color(0xFF4978FF)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(
                                    elevation = 7.dp,
                                    shape = RoundedCornerShape(50.dp),
                                    ambientColor = Color(0x1F3A4FA0),
                                    spotColor = Color(0x1F3A4FA0)
                                )
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        showUserOptions = true
                                    }
                                }
                                .testTag("login_username_input")
                        )

                        // Dropdown suggestion list
                        DropdownMenu(
                            expanded = showUserOptions && matchingAccounts.isNotEmpty(),
                            onDismissRequest = { showUserOptions = false },
                            properties = PopupProperties(focusable = false),
                            modifier = Modifier
                                .widthIn(min = 280.dp, max = 340.dp)
                                .background(Color.White)
                        ) {
                            Text(
                                text = "PILIH AKUN LOGIN OTOMATIS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6676D9),
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            HorizontalDivider(color = Color(0xFFF0F3FA))

                            matchingAccounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFF0F4FF),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = acc.icon,
                                                        contentDescription = null,
                                                        tint = Color(0xFF4978FF),
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = acc.roleLabel,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF34436B)
                                                )
                                                Text(
                                                    text = "user: ${acc.username} • pass: ${acc.defaultPass}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF8999BD)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        username = acc.username
                                        password = acc.defaultPass
                                        showUserOptions = false
                                        if (loginError != null) onClearError()
                                        if (infoNotice != null) infoNotice = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Password Input (.input-group)
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (loginError != null) onClearError()
                            if (infoNotice != null) infoNotice = null
                        },
                        placeholder = {
                            Text(
                                text = "Password",
                                fontSize = 14.sp,
                                color = Color(0xFFA5B0C8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Password",
                                tint = Color(0xFF8999BD),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = { showPassword = !showPassword },
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = if (showPassword) "Sembunyi" else "Lihat",
                                    color = Color(0xFF6676D9),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(50.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF6676D9),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color(0xFF34436B),
                            unfocusedTextColor = Color(0xFF34436B),
                            cursorColor = Color(0xFF4978FF)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = 7.dp,
                                shape = RoundedCornerShape(50.dp),
                                ambientColor = Color(0x1F3A4FA0),
                                spotColor = Color(0x1F3A4FA0)
                            )
                            .testTag("login_password_input")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. Login Button (.login-button)
                    // linear-gradient(100deg, #4978ff, #8051e9) with pill shape & glow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = 9.dp,
                                shape = RoundedCornerShape(50.dp),
                                ambientColor = Color(0x405D5BE6),
                                spotColor = Color(0x405D5BE6)
                            )
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF4978FF), // #4978ff
                                        Color(0xFF8051E9)  // #8051e9
                                    )
                                )
                            )
                            .clickable(onClick = { onLoginSubmit(username, password) })
                            .testTag("login_submit_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.4.sp
                        )
                    }

                    // Error or Info Message (.message)
                    val activeMsg = loginError ?: infoNotice
                    if (!activeMsg.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF0F0),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeMsg,
                                fontSize = 13.sp,
                                color = Color(0xFFBD3636),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // Attribution Version Text as requested
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "V.0.1 By.Holil",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8994AD),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
