# KBA Attendance — Android App

Ứng dụng chấm công cho hệ thống KBA, đồng bộ trực tiếp với backend (Lovable Cloud / Supabase) của dự án `kba2018.vn`.

## Chức năng

- Đăng nhập bằng tài khoản nội bộ (email + mật khẩu, cùng tài khoản dùng trên web `kba2018.vn/noi-bo`).
- Chấm công VÀO / RA cho ngày hôm nay (đồng bộ realtime với bảng `attendance_records`).
- Đánh dấu **đi công tác (ngoài site)** và ghi chú.
- Xem **đơn nghỉ phép** kèm trạng thái: Chờ duyệt / Đã duyệt / Từ chối.
- Xem **lịch sử chấm công** của tháng hiện tại.
- Tự lưu phiên đăng nhập (DataStore), tự khôi phục khi mở lại app.

## Yêu cầu

- Android Studio Hedgehog (2023.1) trở lên.
- Android SDK 34, minSdk 26 (Android 8.0+).
- JDK 17 (đi kèm Android Studio bản mới).

## Mở dự án

1. Giải nén file `kba-attendance-android.zip`.
2. Trong Android Studio chọn **File → Open…** và trỏ tới thư mục đã giải nén.
3. Chờ Gradle sync (lần đầu sẽ tải dependencies & Gradle 8.7).
4. Bấm **Run ▶** trên thiết bị thật hoặc emulator.

> Lần đầu sync Gradle sẽ tự tải `gradle-wrapper.jar`. Nếu Android Studio yêu cầu, chọn **Use Gradle Wrapper**.

## Cấu hình backend

File `app/src/main/java/vn/kba2018/attendance/Config.kt` đã chứa sẵn URL & Anon key của hệ thống KBA. Nếu đổi backend, cập nhật 2 hằng số trong file này.

## Tech stack

- Kotlin 1.9.24, Jetpack Compose (BOM 2024.06), Material 3
- Navigation Compose, DataStore, Coroutines
- OkHttp + kotlinx.serialization (gọi REST API Supabase trực tiếp, không cần SDK)

## Bảo mật

App dùng JWT do Supabase phát hành sau khi login. Mọi truy vấn đều đi qua RLS hiện có của hệ thống — nhân viên chỉ thấy & ghi được dữ liệu của chính mình.
# KBA_cham_cong
# KBA_cham_cong
