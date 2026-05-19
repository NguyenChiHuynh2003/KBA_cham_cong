@file:Suppress("SpellCheckingInspection")

package vn.kba2018.attendance.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Thêm cái này
import androidx.compose.foundation.verticalScroll   // Thêm cái này
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import vn.kba2018.attendance.R
import vn.kba2018.attendance.SessionStore
import vn.kba2018.attendance.api.AttendanceRecord
import vn.kba2018.attendance.api.SupabaseApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(store: SessionStore, nav: NavController) {
    val scope = rememberCoroutineScope()
    val session by store.session.collectAsState(initial = SessionStore.Session(null, null, null, null, null, null, null))
    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var today by remember { mutableStateOf<AttendanceRecord?>(null) }
    var offSite by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    // Khởi tạo trạng thái cuộn
    val scrollState = rememberScrollState()

    val vnTz = remember { TimeZone.getTimeZone("Asia/Ho_Chi_Minh") }
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = vnTz } }
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = vnTz } }
    val displayFmt = remember { SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN")).apply { timeZone = vnTz } }

    suspend fun reload() {
        val token = session.token; val emp = session.employeeId
        if (token != null && emp != null) {
            try {
                today = SupabaseApi.getTodayAttendance(token, emp, dateFmt.format(Date()))
            } catch (_: vn.kba2018.attendance.api.AuthExpiredException) {
                // MainActivity sẽ tự điều hướng về màn đăng nhập
            } catch (_: Exception) { /* lờ lỗi mạng để không crash app */ }
        }
    }

    LaunchedEffect(session.employeeId) {
        if (!session.employeeId.isNullOrBlank()) reload()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.logo_kba),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("KBA Chấm công")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch { store.clear(); nav.navigate("login") { popUpTo(0) } }
                    }) { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Đăng xuất") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                // Kích hoạt tính năng cuộn dọc tại đây
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Khoảng cách đệm phía trên cùng sau TopAppBar
            Spacer(Modifier.height(4.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Xin chào,", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(session.employeeName ?: "...", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    val sub = listOfNotNull(session.position, session.department).filter { it.isNotBlank() }.joinToString(" • ")
                    if (sub.isNotBlank()) Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(displayFmt.format(Date()).replaceFirstChar { it.uppercase() }, fontSize = 13.sp)
                }
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Chấm công hôm nay", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Vào: ${today?.check_in ?: "—"}")
                    Text("Ra:  ${today?.check_out ?: "—"}")
                    if (today?.off_site == true) Text("📍 Ngoài site", fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = offSite, onCheckedChange = { offSite = it }, enabled = today == null)
                        Text("Đi công tác (ngoài site)")
                    }
                    OutlinedTextField(
                        value = notes, onValueChange = { notes = it },
                        label = { Text("Ghi chú (tuỳ chọn)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = today == null,
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        enabled = !loading && session.employeeId != null && today == null,
                        onClick = {
                            loading = true; msg = null
                            scope.launch {
                                try {
                                    val token = session.token!!; val emp = session.employeeId!!
                                    val date = dateFmt.format(Date())
                                    val existing = SupabaseApi.getTodayAttendance(token, emp, date)
                                    if (existing != null) {
                                        today = existing
                                        msg = "Bạn đã chấm công hôm nay rồi. Mai mới được chấm tiếp."
                                    } else {
                                        val now = timeFmt.format(Date())
                                        try {
                                            SupabaseApi.checkIn(token, AttendanceRecord(
                                                employee_id = emp, work_date = date,
                                                check_in = now, check_out = "17:00:00",
                                                status = "present", off_site = offSite,
                                                notes = notes.ifBlank { null }
                                            ))
                                            msg = "Đã chấm công lúc $now (giờ ra mặc định 17:00)"
                                            reload()
                                            if (today == null) {
                                                today = AttendanceRecord(
                                                    employee_id = emp, work_date = date,
                                                    check_in = now, check_out = "17:00:00",
                                                    status = "present", off_site = offSite, notes = notes.ifBlank { null }
                                                )
                                            }
                                        } catch (e: Exception) {
                                            val em = e.message.orEmpty()
                                            if (em.contains("23505") || em.contains("duplicate", ignoreCase = true) || em.contains("409")) {
                                                today = SupabaseApi.getTodayAttendance(token, emp, date) ?: AttendanceRecord(
                                                    employee_id = emp, work_date = date,
                                                    check_in = now, check_out = "17:00:00",
                                                    status = "present", off_site = offSite, notes = null
                                                )
                                                msg = "Bạn đã chấm công hôm nay rồi. Mai mới được chấm tiếp."
                                            } else throw e
                                        }
                                    }
                                } catch (e: Exception) { msg = "Lỗi: ${e.message}" }
                                finally { loading = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text(if (today == null) "Chấm công" else "Đã chấm công hôm nay")
                    }
                    if (msg != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(msg!!, fontSize = 13.sp)
                    }
                }
            }

            ElevatedCard(onClick = { nav.navigate("leave") }, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    leadingContent = { Icon(Icons.Default.EventAvailable, null) },
                    headlineContent = { Text("Đơn nghỉ phép") },
                    supportingContent = { Text("Xem trạng thái duyệt") }
                )
            }
            ElevatedCard(onClick = { nav.navigate("history") }, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    leadingContent = { Icon(Icons.Default.History, null) },
                    headlineContent = { Text("Lịch sử chấm công") },
                    supportingContent = { Text("Tháng hiện tại") }
                )
            }
            ElevatedCard(onClick = { nav.navigate("guide") }, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null) },
                    headlineContent = { Text("Hướng dẫn sử dụng") },
                    supportingContent = { Text("Cách dùng app KBA Chấm công") }
                )
            }

            // Khoảng đệm cuối cùng để nội dung không bị sát mép dưới khi cuộn hết mức
            Spacer(Modifier.height(16.dp))
        }
    }
}