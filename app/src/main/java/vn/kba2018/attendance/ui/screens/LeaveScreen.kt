@file:Suppress("SpellCheckingInspection")

package vn.kba2018.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import vn.kba2018.attendance.SessionStore
import vn.kba2018.attendance.api.LeaveRequest
import vn.kba2018.attendance.api.SupabaseApi
import vn.kba2018.attendance.notify.LeaveNotificationService

internal fun fmtDate(s: String): String = try {
    val parts = s.substring(0, 10).split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (_: Exception) { s }

private fun statusLabel(s: String) = when (s) {
    "approved" -> "Đã duyệt" to Color(0xFF16A34A)
    "rejected" -> "Từ chối" to Color(0xFFDC2626)
    "pending" -> "Chờ duyệt" to Color(0xFFD97706)
    else -> s to Color.Gray
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveScreen(store: SessionStore, nav: NavController) {
    val context = LocalContext.current
    val session by store.session.collectAsState(initial = SessionStore.Session(null, null, null, null, null, null, null))
    var items by remember { mutableStateOf<List<LeaveRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var refreshKey by remember { mutableIntStateOf(0) }

    // 🌟 KHẮC PHỤC: Lắng nghe làm mới an toàn qua savedStateHandle (Không gây loop Coroutine)
    val navBackStackEntry = nav.currentBackStackEntry
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("should_refresh")?.observeForever { shouldRefresh ->
            if (shouldRefresh == true) {
                refreshKey++
                navBackStackEntry.savedStateHandle.remove<Boolean>("should_refresh")
            }
        }
    }

    // Khởi chạy Realtime Service ngầm đồng bộ tình trạng duyệt đơn từ
    LaunchedEffect(session.employeeId) {
        val empId = session.employeeId
        if (empId != null) {
            LeaveNotificationService.startService(context, empId)
        }
    }

    // Tải dữ liệu từ API Supabase
    LaunchedEffect(session.employeeId, refreshKey) {
        val token = session.token; val emp = session.employeeId
        if (token != null && emp != null) {
            loading = true
            error = null // Reset lỗi trước khi load
            try {
                items = SupabaseApi.getLeaveRequests(token, emp)
            }
            catch (e: Exception) {
                error = e.message ?: "Lỗi kết nối hệ thống"
            }
            finally {
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đơn nghỉ phép") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { nav.navigate("create_leave") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tạo đơn mới",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center))
                error != null -> Text(error!!, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                items.isEmpty() -> Text("Chưa có đơn nghỉ phép nào", Modifier.padding(16.dp))
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items, key = { it.id }) { lr ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    val readableType = when (lr.leave_type) {
                                        "leave" -> "Đơn xin nghỉ"
                                        "absence" -> "Đơn vắng mặt"
                                        "overtime" -> "Đơn tăng ca"
                                        "extra_work" -> "Đơn làm thêm"
                                        "business_trip" -> "Đơn công tác"
                                        "benefits" -> "Đơn làm chế độ"
                                        "shift_change" -> "Đơn đổi ca"
                                        "attendance_explain" -> "Đơn giải trình chấm công"
                                        "comp_leave" -> "Đơn nghỉ bù"
                                        "attendance_confirm" -> "Xác nhận dữ liệu chấm công"
                                        else -> lr.leave_type.replaceFirstChar { it.uppercase() }
                                    }

                                    Text(readableType, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    val (label, color) = statusLabel(lr.status)
                                    AssistChip(onClick = {}, label = { Text(label, color = color) })
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("Từ ${fmtDate(lr.start_date)} → ${fmtDate(lr.end_date)} (${lr.total_days} ngày)")
                                if (!lr.reason.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Lý do: ${lr.reason}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}