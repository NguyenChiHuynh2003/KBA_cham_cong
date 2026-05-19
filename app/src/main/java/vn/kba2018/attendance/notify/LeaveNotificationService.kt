package vn.kba2018.attendance.notify

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.serialization.json.*
import okhttp3.*
import vn.kba2018.attendance.Config
import vn.kba2018.attendance.MainActivity
import vn.kba2018.attendance.R
import java.util.concurrent.TimeUnit

class LeaveNotificationService : Service() {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Đảm bảo socket không tự ngắt theo thời gian thực tế
        .build()

    companion object {
        private var currentEmployeeId: String? = null

        fun startService(context: Context, employeeId: String) {
            currentEmployeeId = employeeId
            val intent = Intent(context, LeaveNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "leave_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(channelId) == null) {
                val ch = NotificationChannel(channelId, "Đồng bộ đơn từ", NotificationManager.IMPORTANCE_MIN)
                nm.createNotificationChannel(ch)
            }
        }

        // Tạo thông báo Foreground cố định tránh bị Android quét dọn bộ nhớ kill app
        val foregroundNotification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground_img)
            .setContentTitle("KBA Attendance")
            .setContentText("Hệ thống đang kết nối dữ liệu đơn từ thời gian thực...")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(2002, foregroundNotification)

        if (webSocket == null && currentEmployeeId != null) {
            connectWebSocket()
        }

        return START_STICKY
    }

    private fun connectWebSocket() {
        val wsUrl = Config.SUPABASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/realtime/v1/websocket?apikey=${Config.SUPABASE_ANON_KEY}"

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Duy trì nhịp đập Heartbeat gửi lên Phoenix Server của Supabase mỗi 30 giây
                Thread {
                    while (this@LeaveNotificationService.webSocket != null) {
                        try {
                            Thread.sleep(30000)
                            webSocket.send("""{"topic":"phoenix","event":"heartbeat","payload":{},"ref":"1"}""")
                        } catch (_: Exception) {}
                    }
                }.start()

                // Đăng ký bắt luồng thay đổi của bảng leave_requests thuộc riêng employee_id này
                val joinTopic = "realtime:public:leave_requests:employee_id=eq.${currentEmployeeId}"
                val subscribeMsg = """
                    {
                      "topic": "$joinTopic",
                      "event": "phx_join",
                      "payload": {},
                      "ref": "2"
                    }
                """.trimIndent()
                webSocket.send(subscribeMsg)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = Json.parseToJsonElement(text).jsonObject
                    val event = json["event"]?.jsonPrimitive?.content

                    // Chỉ xử lý khi có tác vụ UPDATE trạng thái đơn từ từ phía quản lý
                    if (event == "UPDATE") {
                        val payload = json["payload"]?.jsonObject ?: return
                        val newData = payload["new"]?.jsonObject ?: return

                        val status = newData["status"]?.jsonPrimitive?.content
                        val leaveType = newData["leave_type"]?.jsonPrimitive?.content ?: "leave"

                        val typeLabel = when (leaveType) {
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
                            else -> "Đơn từ"
                        }

                        when (status) {
                            "approved" -> showStatusNotification("KBA - ĐƠN ĐÃ DUYỆT", "$typeLabel của bạn đã được phê duyệt thành công.")
                            "rejected" -> showStatusNotification("KBA - ĐƠN BỊ TỪ CHỐI", "$typeLabel của bạn không được phê duyệt.")
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (currentEmployeeId != null) reconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                reconnect()
            }
        })
    }

    private fun reconnect() {
        try { Thread.sleep(5000) } catch (_: Exception) {}
        if (currentEmployeeId != null) connectWebSocket()
    }

    private fun showStatusNotification(title: String, message: String) {
        val channelId = "leave_status_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(channelId) == null) {
                val ch = NotificationChannel(channelId, "Trạng thái đơn từ", NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(true)
                }
                nm.createNotificationChannel(ch)
            }
        }

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground_img)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notif)
    }

    override fun onDestroy() {
        webSocket?.close(1000, "Service Destroyed")
        webSocket = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}