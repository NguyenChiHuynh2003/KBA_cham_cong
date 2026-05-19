package vn.kba2018.attendance.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import vn.kba2018.attendance.MainActivity
import vn.kba2018.attendance.R
import java.util.Calendar

class AttendanceReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Lấy ngày hiện tại trong tuần
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Calendar.SUNDAY tương ứng với Chủ Nhật
        // Nếu KHÔNG PHẢI Chủ Nhật (tức là từ Thứ 2 đến Thứ 7) thì mới hiển thị thông báo
        if (dayOfWeek != Calendar.SUNDAY) {
            showNotification(context)
        }

        // Luôn luôn lập lịch cho ngày tiếp theo để vòng lặp nhắc nhở không bị dừng lại
        ReminderScheduler.scheduleNext(context)
    }

    private fun showNotification(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "attendance_reminder"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                "Nhắc chấm công",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nhắc bạn chấm công mỗi sáng"
                enableVibration(true)
            }
            nm.createNotificationChannel(ch)
        }

        val tapIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground_img)
            .setContentTitle("KBA - Nhắc chấm công")
            .setContentText("Đã quá 8:05, bạn hãy mở app để chấm công ngay.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        nm.notify(2001, notif)
    }
}