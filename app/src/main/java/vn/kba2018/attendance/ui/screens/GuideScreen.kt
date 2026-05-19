@file:Suppress("SpellCheckingInspection")

package vn.kba2018.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(nav: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hướng dẫn sử dụng") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Section("1. Đăng nhập",
                "• Mở app KBA Chấm công.",
                "• Nhập Email và Mật khẩu của tài khoản nội bộ kba2018.vn.",
                "• Bấm \"Đăng nhập\". Nếu quên mật khẩu, liên hệ Quản trị viên (Admin) để được cấp lại."
            )
            Section("2. Chấm công hằng ngày",
                "• Sau khi đăng nhập, bạn sẽ thấy thẻ \"Chấm công hôm nay\".",
                "• (Tuỳ chọn) Tích vào ô \"Đi công tác (ngoài site)\" nếu hôm đó bạn làm việc ngoài văn phòng.",
                "• (Tuỳ chọn) Nhập ghi chú nếu cần.",
                "• Bấm nút \"Chấm công\" — hệ thống tự ghi nhận:",
                "    - Giờ vào = đúng thời điểm bạn bấm (giờ Việt Nam).",
                "    - Giờ ra mặc định = 17:00.",
                "• Mỗi ngày chỉ chấm công 1 lần. Sau khi chấm xong, nút sẽ chuyển thành \"Đã chấm công hôm nay\".",
                "• Nếu cần điều chỉnh giờ ra (về sớm/về muộn), liên hệ HR để cập nhật trên hệ thống web."
            )
            Section("3. Xem đơn nghỉ phép",
                "• Trên màn hình chính, bấm \"Đơn nghỉ phép\".",
                "• Danh sách hiển thị tất cả đơn của bạn kèm trạng thái:",
                "    - Chờ duyệt: đang chờ HR/Quản lý phê duyệt.",
                "    - Đã duyệt: đơn đã được chấp nhận.",
                "    - Từ chối: đơn không được duyệt (xem lý do trong chi tiết).",
                "• Việc tạo đơn nghỉ phép mới được thực hiện trên website https://kba2018.vn/noi-bo."
            )
            Section("4. Lịch sử chấm công",
                "• Bấm \"Lịch sử chấm công\" để xem các bản ghi của tháng hiện tại.",
                "• Bao gồm cả các ngày nghỉ phép đã được duyệt (tự sinh từ hệ thống).",
                "• Dữ liệu đồng bộ realtime với hệ thống web — mọi thay đổi từ HR sẽ tự cập nhật ở đây."
            )
            Section("5. Đăng xuất",
                "• Bấm icon mũi tên (góc phải trên) để đăng xuất.",
                "• Sau khi đăng xuất, bạn cần đăng nhập lại để chấm công."
            )
            Section("6. Câu hỏi thường gặp",
                "Q: Bấm chấm công bị báo lỗi?",
                "  → Kiểm tra kết nối internet. Nếu vẫn lỗi, chụp màn hình thông báo lỗi gửi cho IT.",
                "Q: Tôi quên chấm công hôm qua?",
                "  → App chỉ cho chấm công ngày hiện tại. Liên hệ HR để bổ sung trên web.",
                "Q: Giờ ra mặc định 17:00 nhưng tôi tăng ca?",
                "  → Báo HR để điều chỉnh và ghi nhận OT trên hệ thống web.",
                "Q: App có cần cấp quyền gì không?",
                "  → Chỉ cần kết nối internet. Không truy cập vị trí, camera hay danh bạ."
            )
            Section("7. Hỗ trợ",
                "• Website nội bộ: https://kba2018.vn/noi-bo",
                "• Mọi vấn đề về tài khoản, dữ liệu chấm công: liên hệ phòng HR.",
                "• Lỗi kỹ thuật: liên hệ bộ phận IT của công ty."
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: String, vararg lines: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            lines.forEach { line ->
                Text(line, fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}
