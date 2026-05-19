package vn.kba2018.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import vn.kba2018.attendance.api.SupabaseApi
import vn.kba2018.attendance.notify.ReminderScheduler
import vn.kba2018.attendance.ui.screens.HomeScreen
import vn.kba2018.attendance.ui.screens.LeaveScreen
import vn.kba2018.attendance.ui.screens.CreateLeaveScreen // Thêm import này
import vn.kba2018.attendance.ui.screens.LoginScreen
import vn.kba2018.attendance.ui.screens.HistoryScreen
import vn.kba2018.attendance.ui.screens.GuideScreen
import vn.kba2018.attendance.ui.theme.KBATheme

class MainActivity : ComponentActivity() {

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignore */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Schedule daily 8:05 VN reminder
        ReminderScheduler.scheduleNext(this)

        setContent {
            KBATheme {
                val ctx = LocalContext.current
                val store = remember { SessionStore(ctx) }
                val nav = rememberNavController()
                var startDest by remember { mutableStateOf<String?>(null) }

                // Lấy và quan sát trạng thái session hiện tại để truyền thông tin Token/EmployeeId sang màn hình Tạo đơn
                val sessionState by store.session.collectAsState(initial = SessionStore.Session(null, null, null, null, null, null, null))

                LaunchedEffect(Unit) {
                    val s = store.session.first()
                    SupabaseApi.currentToken = s.token
                    SupabaseApi.currentRefreshToken = s.refreshToken
                    SupabaseApi.onTokensRefreshed = { access, refresh ->
                        store.updateTokens(access, refresh)
                    }
                    SupabaseApi.onAuthExpired = {
                        store.clear()
                        nav.navigate("login") { popUpTo(0) }
                    }
                    startDest = if (s.token.isNullOrBlank() || s.employeeId.isNullOrBlank()) "login" else "home"
                }

                LaunchedEffect(store) {
                    store.session.collectLatest {
                        SupabaseApi.currentToken = it.token
                        SupabaseApi.currentRefreshToken = it.refreshToken
                    }
                }

                if (startDest == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
                } else {
                    NavHost(nav, startDestination = startDest!!) {
                        composable("login") { LoginScreen(store) { nav.navigate("home") { popUpTo("login") { inclusive = true } } } }
                        composable("home") { HomeScreen(store, nav) }
                        composable("leave") { LeaveScreen(store, nav) }
                        composable("history") { HistoryScreen(store, nav) }
                        composable("guide") { GuideScreen(nav) }

                        // --- ĐĂNG KÝ ROUTE TẠO ĐƠN NGHỈ PHÉP TẠI ĐÂY ---
                        composable("create_leave") {
                            val currentToken = sessionState.token.orEmpty()
                            val currentEmpId = sessionState.employeeId.orEmpty()

                            CreateLeaveScreen(
                                currentEmployeeId = currentEmpId,
                                token = currentToken,
                                onNavigateBack = { nav.popBackStack() },
                                onSuccess = { nav.popBackStack() } // Khi tạo đơn thành công, quay về màn hình danh sách đơn (leave)
                            )
                        }
                    }
                }
            }
        }
    }
}