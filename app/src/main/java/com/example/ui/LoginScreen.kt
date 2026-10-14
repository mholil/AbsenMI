package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

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
    var showCredentialGuide by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Hero Header Card with Madrasah Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                shape = RoundedCornerShape(20.dp),
                color = MiDarkGreen,
                shadowElevation = 4.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_madrasah_banner_1791206309307),
                        contentDescription = "Banner Madrasah Ibtidaiyah",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MiDarkGreen.copy(alpha = 0.55f),
                                        MiDarkGreen.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = "Ikon Madrasah",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ABSENSI MI",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Administrasi & Kehadiran Siswa",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = schoolName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MiEmerald,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Tahun Ajaran $academicYearLabel",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Login Form Card
            MiCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Text(
                    text = "Masuk ke Sistem Madrasah",
                    style = MaterialTheme.typography.titleLarge,
                    color = MiTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gunakan akun Admin, Kepala Madrasah, Wali Kelas, atau Guru Piket yang terdaftar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MiTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!loginError.isNullOrBlank()) {
                    Surface(
                        color = StatusAlpaBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StatusAlpaText.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = StatusAlpaText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = loginError,
                                fontSize = 13.sp,
                                color = StatusAlpaText,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        if (loginError != null) onClearError()
                    },
                    label = { Text("Username atau NIP", fontSize = 14.sp) },
                    placeholder = { Text("Masukkan username / NIP", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = MiEmerald)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_username_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (loginError != null) onClearError()
                    },
                    label = { Text("Kata Sandi", fontSize = 14.sp) },
                    placeholder = { Text("Masukkan kata sandi", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = MiEmerald)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Tampilkan Kata Sandi",
                                tint = MiTextSecondary
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                NeumorphicTactileButton(
                    onClick = { onLoginSubmit(username, password) },
                    isPrimary = true,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 13.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("login_submit_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Login,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Masuk Sekarang",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Information about registered baseline accounts (not auto-login buttons, strictly informational / helper)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showCredentialGuide = !showCredentialGuide }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MiEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lihat Daftar Akun Terdaftar (Baseline)",
                            fontSize = 13.sp,
                            color = MiEmerald,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = if (showCredentialGuide) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = MiEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showCredentialGuide) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MiSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MiBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ketuk baris akun untuk mengisi form login secara cepat:",
                                fontSize = 12.sp,
                                color = MiTextSecondary
                            )
                            CredentialHelperRow(
                                roleLabel = "Admin Madrasah",
                                user = "admin",
                                pass = "admin123",
                                onFill = { u, p -> username = u; password = p; onClearError() }
                            )
                            CredentialHelperRow(
                                roleLabel = "Kepala Sekolah",
                                user = "kepsek",
                                pass = "kepsek123",
                                onFill = { u, p -> username = u; password = p; onClearError() }
                            )
                            CredentialHelperRow(
                                roleLabel = "Wali Kelas 1A",
                                user = "wali1a",
                                pass = "wali123",
                                onFill = { u, p -> username = u; password = p; onClearError() }
                            )
                            CredentialHelperRow(
                                roleLabel = "Wali Kelas 2A",
                                user = "wali2a",
                                pass = "wali123",
                                onFill = { u, p -> username = u; password = p; onClearError() }
                            )
                            CredentialHelperRow(
                                roleLabel = "Guru Piket",
                                user = "piket",
                                pass = "piket123",
                                onFill = { u, p -> username = u; password = p; onClearError() }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Kompatibel dengan akun lama & penyimpanan lokal terenkripsi",
                fontSize = 12.sp,
                color = MiTextMuted
            )
        }
    }
}

@Composable
private fun CredentialHelperRow(
    roleLabel: String,
    user: String,
    pass: String,
    onFill: (String, String) -> Unit
) {
    Surface(
        color = MiSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MiBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFill(user, pass) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = roleLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTextPrimary
                )
                Text(
                    text = "Username: $user | Sandi: $pass",
                    fontSize = 12.sp,
                    color = MiTextSecondary
                )
            }
            Surface(
                color = MiMintSoft,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Isi Form",
                    fontSize = 11.sp,
                    color = MiEmerald,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
