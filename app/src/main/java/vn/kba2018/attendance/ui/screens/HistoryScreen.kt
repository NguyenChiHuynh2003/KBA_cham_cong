@file:Suppress("SpellCheckingInspection")

package vn.kba2018.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import vn.kba2018.attendance.SessionStore
import vn.kba2018.attendance.api.AttendanceRecord
import vn.kba2018.attendance.api.SupabaseApi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(store: SessionStore, nav: NavController) {
    val session by store.session.collectAsState(initial = SessionStore.Session(null, null, null, null, null, null, null))
    var items by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(session.employeeId) {
        val token = session.token; val emp = session.employeeId
        if (token != null && emp != null) {
            try {
                val cal = Calendar.getInstance()
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                cal.set(Calendar.DAY_OF_MONTH, 1); val from = fmt.format(cal.time)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); val to = fmt.format(cal.time)
                items = SupabaseApi.getAttendanceMonth(token, emp, from, to)
            } catch (_: Exception) { /* không crash app khi mất mạng / hết phiên */ }
            finally { loading = false }
        } else { loading = false }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Lịch sử chấm công") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        )
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (loading) CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center))
            else if (items.isEmpty()) Text("Chưa có dữ liệu tháng này", Modifier.padding(16.dp))
            else LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id ?: it.work_date }) { r ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(fmtDate(r.work_date), fontWeight = FontWeight.SemiBold)
                            Text("Vào: ${r.check_in ?: "—"}   Ra: ${r.check_out ?: "—"}")
                            Text("Trạng thái: ${r.status}${if (r.off_site) " • Ngoài site" else ""}", style = MaterialTheme.typography.bodySmall)
                            if (!r.notes.isNullOrBlank()) Text("Ghi chú: ${r.notes}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
