package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.RoleType
import com.example.data.UserEntity
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AbsensiMiRootApp()
            }
        }
    }
}

data class RoleNavTab(
    val title: String,
    val icon: ImageVector,
    val tag: String
)

fun getBottomTabsForRole(role: String): List<RoleNavTab> = when (role) {
    RoleType.ADMIN -> listOf(
        RoleNavTab("Dashboard", Icons.Outlined.Dashboard, "nav_dashboard"),
        RoleNavTab("Data", Icons.Outlined.Storage, "nav_data"),
        RoleNavTab("Absensi", Icons.Outlined.FactCheck, "nav_absensi"),
        RoleNavTab("Laporan", Icons.Outlined.Assessment, "nav_laporan")
    )
    RoleType.WALI_KELAS -> listOf(
        RoleNavTab("Dashboard", Icons.Outlined.Dashboard, "nav_dashboard"),
        RoleNavTab("Siswa", Icons.Outlined.Groups, "nav_siswa"),
        RoleNavTab("Absensi", Icons.Outlined.FactCheck, "nav_absensi"),
        RoleNavTab("Laporan", Icons.Outlined.Assessment, "nav_laporan")
    )
    RoleType.KEPALA_SEKOLAH -> listOf(
        RoleNavTab("Dashboard", Icons.Outlined.Dashboard, "nav_dashboard"),
        RoleNavTab("Absensi", Icons.Outlined.FactCheck, "nav_absensi"),
        RoleNavTab("Laporan", Icons.Outlined.Assessment, "nav_laporan"),
        RoleNavTab("Terlambat", Icons.Outlined.Schedule, "nav_terlambat")
    )
    RoleType.GURU_PIKET -> listOf(
        RoleNavTab("Scan Absen", Icons.Outlined.QrCodeScanner, "nav_scan"),
        RoleNavTab("Belum Hadir", Icons.Outlined.PersonOff, "nav_belum_hadir"),
        RoleNavTab("Absensi", Icons.Outlined.FactCheck, "nav_absensi"),
        RoleNavTab("Laporan", Icons.Outlined.Assessment, "nav_laporan")
    )
    else -> listOf(
        RoleNavTab("Dashboard", Icons.Outlined.Dashboard, "nav_dashboard"),
        RoleNavTab("Absensi", Icons.Outlined.FactCheck, "nav_absensi"),
        RoleNavTab("Laporan", Icons.Outlined.Assessment, "nav_laporan")
    )
}

@Composable
fun AbsensiMiRootApp(
    viewModel: MadrasahViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Back button handling for modals and non-home bottom tabs
    BackHandler(enabled = state.activeModal !is ActiveModal.None || state.selectedBottomTab != 0) {
        if (state.activeModal !is ActiveModal.None) {
            viewModel.closeModal()
        } else if (state.selectedBottomTab != 0) {
            viewModel.selectBottomTab(0)
        }
    }

    // Auto-clear toast banner after 3.5 seconds
    LaunchedEffect(state.toastMessage) {
        if (state.toastMessage != null) {
            delay(3500)
            viewModel.clearMessage()
        }
    }

    if (!state.isInitialized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MiDarkGreen)
        }
        return
    }

    val currentUser = state.currentUser
    if (currentUser == null) {
        LoginScreen(
            schoolName = state.settings.schoolName,
            academicYearLabel = "${state.settings.activeAcademicYear} • ${state.settings.activeSemester}",
            loginError = state.loginError,
            onLoginSubmit = { u, p -> viewModel.login(u, p) },
            onClearError = { viewModel.clearLoginError() }
        )
    } else {
        AuthenticatedMainScaffold(
            user = currentUser,
            state = state,
            viewModel = viewModel
        )
    }

    // Render any active modal on top
    AllModalsHost(state = state, viewModel = viewModel)
}

@Composable
private fun AuthenticatedMainScaffold(
    user: UserEntity,
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val tabs = remember(user.role) { getBottomTabsForRole(user.role) }
    val safeTabIdx = state.selectedBottomTab.coerceIn(0, tabs.lastIndex)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MiBackground,
        topBar = {
            MadrasahTopAppBar(
                user = user,
                schoolName = state.settings.schoolName,
                onOpenAdminSettings = { viewModel.openModal(ActiveModal.AdminSettings) },
                onRequestLogout = { viewModel.requestLogoutConfirmation() }
            )
        },
        bottomBar = {
            // Strictly 4 items per role; Settings is NEVER in bottom navigation
            NavigationBar(
                containerColor = MiSurface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        ambientColor = MiDarkGreen.copy(alpha = 0.08f),
                        spotColor = NeuDarkShadow.copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    NeuLightHighlight.copy(alpha = 0.95f),
                                    MiBorderColor.copy(alpha = 0.6f)
                                )
                            )
                        ),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .navigationBarsPadding()
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = safeTabIdx == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectBottomTab(index) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(21.dp)
                            )
                        },
                        label = {
                            // Bottom navigation font size: 11-12sp per specification
                            Text(
                                text = tab.title,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color(0xFF4978FF),
                            indicatorColor = Color(0xFF4978FF),
                            unselectedIconColor = MiTextSecondary,
                            unselectedTextColor = MiTextSecondary
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (user.role) {
                RoleType.ADMIN -> when (safeTabIdx) {
                    0 -> DashboardSection(state = state, viewModel = viewModel)
                    1 -> DataAndStudentsSection(state = state, viewModel = viewModel)
                    2 -> AttendanceSection(state = state, viewModel = viewModel)
                    3 -> ReportsSection(state = state, viewModel = viewModel)
                }
                RoleType.WALI_KELAS -> when (safeTabIdx) {
                    0 -> DashboardSection(state = state, viewModel = viewModel)
                    1 -> DataAndStudentsSection(state = state, viewModel = viewModel)
                    2 -> AttendanceSection(state = state, viewModel = viewModel)
                    3 -> ReportsSection(state = state, viewModel = viewModel)
                }
                RoleType.KEPALA_SEKOLAH -> when (safeTabIdx) {
                    0 -> DashboardSection(state = state, viewModel = viewModel)
                    1 -> AttendanceSection(state = state, viewModel = viewModel)
                    2 -> ReportsSection(state = state, viewModel = viewModel)
                    3 -> LateMonitoringSection(state = state, viewModel = viewModel)
                }
                RoleType.GURU_PIKET -> when (safeTabIdx) {
                    0 -> ScanQrAttendanceSection(state = state, viewModel = viewModel)
                    1 -> BelumHadirSection(state = state, viewModel = viewModel)
                    2 -> AttendanceSection(state = state, viewModel = viewModel)
                    3 -> ReportsSection(state = state, viewModel = viewModel)
                }
            }

            // Floating Feedback Toast Banner
            if (!state.toastMessage.isNullOrBlank()) {
                Surface(
                    color = MiDarkGreen,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, MiEmerald),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.toastMessage!!,
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearMessage() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Tutup Notifikasi",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MadrasahTopAppBar(
    user: UserEntity,
    schoolName: String,
    onOpenAdminSettings: () -> Unit,
    onRequestLogout: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                ambientColor = Color(0x333A4FA0),
                spotColor = Color(0x333A4FA0)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4978FF), // #4978ff
                            Color(0xFF8051E9)  // #8051e9
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(2.5.dp, RoundedCornerShape(11.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFC9A8FF),
                                        Color(0xFF72C7FF)
                                    )
                                )
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = 0.6f)
                                ),
                                RoundedCornerShape(11.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = "Logo MI",
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ABSENSI MI",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (user.role == RoleType.WALI_KELAS && user.assignedClassName.isNotBlank()) {
                            "Wali Kelas ${user.assignedClassName}"
                        } else {
                            RoleType.label(user.role)
                        },
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Settings is placed in header for Admin, never as an extra bottom navigation item
                if (user.role == RoleType.ADMIN) {
                    IconButton(
                        onClick = onOpenAdminSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(2.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.25f))
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.5f),
                                            Color.White.copy(alpha = 0.12f)
                                        )
                                    )
                                ),
                                CircleShape
                            )
                            .testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Pengaturan Madrasah",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onRequestLogout,
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(2.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.25f))
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.5f),
                                        Color.White.copy(alpha = 0.12f)
                                    )
                                )
                            ),
                            CircleShape
                        )
                        .testTag("top_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = "Keluar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
}

