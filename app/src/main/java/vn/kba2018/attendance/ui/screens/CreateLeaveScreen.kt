@file:Suppress("SpellCheckingInspection")

package vn.kba2018.attendance.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vn.kba2018.attendance.api.CreateLeavePayload
import vn.kba2018.attendance.api.SupabaseApi
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLeaveScreen(
    currentEmployeeId: String,
    token: String,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 1. DANH SÁCH 10 LOẠI ĐƠN TỪ ĐỒNG BỘ CHUẨN ĐỊNH DẠNG WEB GỐC
    val leaveTypes = listOf(
        "leave" to "Đơn xin nghỉ",
        "absence" to "Đơn vắng mặt",
        "overtime" to "Đơn tăng ca",
        "extra_work" to "Đơn làm thêm",
        "business_trip" to "Đơn công tác",
        "benefits" to "Đơn làm chế độ",
        "shift_change" to "Đơn đổi ca",
        "attendance_explain" to "Đơn giải trình chấm công",
        "comp_leave" to "Đơn nghỉ bù",
        "attendance_confirm" to "Xác nhận dữ liệu chấm công"
    )

    // Khai báo các State lưu trữ form dữ liệu
    var selectedLeaveType by remember { mutableStateOf(leaveTypes.first().first) }
    var selectedLeaveTypeName by remember { mutableStateOf(leaveTypes.first().second) }
    var leaveTypeDropdownExpanded by remember { mutableStateOf(false) }

    // Hình thức nghỉ: "all_day" (Cả ngày), "half_day" (Nửa ngày), "hourly" (Theo giờ)
    var timePeriodType by remember { mutableStateOf("all_day") }
    var halfDayPeriod by remember { mutableStateOf("morning") } // "morning" hoặc "afternoon"

    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var startDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var endDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("17:00") }

    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }

    // Date Picker Dialog Builder Helper
    val showDatePicker = { isStartDate: Boolean ->
        val currentVal = if (isStartDate) startDate else endDate
        val parsedDate = try { dateFormat.parse(currentVal) ?: Date() } catch(_: Exception) { Date() }
        calendar.time = parsedDate
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                val formatted = dateFormat.format(cal.time)
                if (isStartDate) {
                    startDate = formatted
                    if (endDate < formatted) endDate = formatted
                } else {
                    if (formatted >= startDate) endDate = formatted
                    else Toast.makeText(context, "Ngày kết thúc không hợp lệ", Toast.LENGTH_SHORT).show()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Time Picker Dialog Builder Helper
    val showTimePicker = { isStartTime: Boolean ->
        val currentVal = if (isStartTime) startTime else endTime
        val parts = currentVal.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, h, m ->
                val formatted = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                if (isStartTime) startTime = formatted else endTime = formatted
            },
            hour,
            minute,
            true
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo đơn công tác / nghỉ phép") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. CHỌN LOẠI ĐƠN TỪ
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedLeaveTypeName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Loại đơn từ *") },
                    trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                    modifier = Modifier.fillMaxWidth().clickable { leaveTypeDropdownExpanded = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                DropdownMenu(
                    expanded = leaveTypeDropdownExpanded,
                    onDismissRequest = { leaveTypeDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    leaveTypes.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedLeaveType = code
                                selectedLeaveTypeName = name
                                leaveTypeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 2. CHỌN HÌNH THỨC THỜI GIAN
            Text("Hình thức thời gian *", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = timePeriodType == "all_day",
                    onClick = { timePeriodType = "all_day"; endDate = startDate },
                    label = { Text("Cả ngày") }
                )
                FilterChip(
                    selected = timePeriodType == "half_day",
                    onClick = { timePeriodType = "half_day"; endDate = startDate },
                    label = { Text("Nửa ngày") }
                )
                FilterChip(
                    selected = timePeriodType == "hourly",
                    onClick = { timePeriodType = "hourly"; endDate = startDate },
                    label = { Text("Theo giờ") }
                )
            }

            // Chi tiết tùy biến theo Hình thức thời gian đã chọn
            when (timePeriodType) {
                "all_day" -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Từ ngày") },
                            trailingIcon = { Icon(Icons.Default.DateRange, null) },
                            modifier = Modifier.weight(1f).clickable { showDatePicker(true) },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Đến ngày") },
                            trailingIcon = { Icon(Icons.Default.DateRange, null) },
                            modifier = Modifier.weight(1f).clickable { showDatePicker(false) },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
                "half_day" -> {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Chọn ngày") },
                        trailingIcon = { Icon(Icons.Default.DateRange, null) },
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker(true) },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = halfDayPeriod == "morning", onClick = { halfDayPeriod = "morning" })
                            Text("Buổi sáng")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = halfDayPeriod == "afternoon", onClick = { halfDayPeriod = "afternoon" })
                            Text("Buổi chiều")
                        }
                    }
                }
                "hourly" -> {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Chọn ngày") },
                        trailingIcon = { Icon(Icons.Default.DateRange, null) },
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker(true) },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Từ giờ") },
                            modifier = Modifier.weight(1f).clickable { showTimePicker(true) },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Đến giờ") },
                            modifier = Modifier.weight(1f).clickable { showTimePicker(false) },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
            }

            // 3. LÝ DO ĐƠN TỪ
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Lý do cụ thể *") },
                placeholder = { Text("Nhập lý do thực hiện đơn...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // 4. GHI CHÚ BỔ SUNG
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Ghi chú bổ sung (Nếu có)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // NÚT GỬI ĐƠN
            Button(
                onClick = {
                    if (reason.trim().isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập lý do đơn từ", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val finalEndDate = if (timePeriodType == "all_day") endDate else startDate

                    val finalTimePeriod = when (timePeriodType) {
                        "all_day" -> "all_day"
                        "half_day" -> halfDayPeriod
                        else -> null
                    }

                    // CẬP NHẬT: Gán giá trị null cho approver_id và notification_recipients theo API mới
                    val payload = CreateLeavePayload(
                        employee_id = currentEmployeeId,
                        leave_type = selectedLeaveType,
                        start_date = startDate,
                        end_date = finalEndDate,
                        time_period = finalTimePeriod,
                        start_time = if (timePeriodType == "hourly") startTime else null,
                        end_time = if (timePeriodType == "hourly") endTime else null,
                        reason = reason.trim(),
                        notes = notes.trim().ifBlank { null },
                        approver_id = null,
                        notification_recipients = null,
                        status = "pending"
                    )

                    scope.launch {
                        isSubmitting = true
                        try {
                            val result = SupabaseApi.createLeaveRequest(token, payload)
                            if (result) {
                                Toast.makeText(context, "Gửi đơn thành công!", Toast.LENGTH_LONG).show()
                                onSuccess()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Gửi đơn thất bại", Toast.LENGTH_LONG).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Gửi đơn", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}