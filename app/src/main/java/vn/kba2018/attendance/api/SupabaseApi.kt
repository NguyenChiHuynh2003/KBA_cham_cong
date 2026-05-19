package vn.kba2018.attendance.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import vn.kba2018.attendance.Config
import java.util.concurrent.TimeUnit

class AuthExpiredException : RuntimeException("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại")

@Serializable
data class CreateLeavePayload(
    val employee_id: String,
    val leave_type: String,
    val start_date: String,
    val end_date: String,
    val time_period: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val shift_info: String? = null,
    val reason: String? = null,
    val notes: String? = null,

    // CHỈNH SỬA: Chuyển sang dạng Nullable để tránh lỗi gửi chuỗi rỗng ép kiểu UUID lỗi (22P02)
    val approver_id: String? = null,

    // CHỈNH SỬA: Chuyển sang dạng Nullable để loại bỏ hoàn toàn mảng rỗng [] nếu không dùng tới
    val notification_recipients: List<String>? = null,

    val status: String = "pending"
)

object SupabaseApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // explicitNulls = false kết hợp với thuộc tính ẩn (= null) phía trên sẽ tự động loại bỏ key khỏi chuỗi JSON
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
    private val JSON_MEDIA = "application/json".toMediaType()

    /** Token providers wired from MainActivity so refresh works app-wide. */
    @Volatile var currentToken: String? = null
    @Volatile var currentRefreshToken: String? = null
    /** Called whenever a refresh produces new tokens; persist them. */
    @Volatile var onTokensRefreshed: (suspend (access: String, refresh: String?) -> Unit)? = null
    /** Called when the session is irrecoverable (refresh fails); should clear store + nav to login. */
    @Volatile var onAuthExpired: (suspend () -> Unit)? = null

    private fun authHeaders(token: String? = null) = mapOf(
        "apikey" to Config.SUPABASE_ANON_KEY,
        "Authorization" to "Bearer ${token ?: Config.SUPABASE_ANON_KEY}",
        "Content-Type" to "application/json",
    )

    private suspend fun refreshAccessToken(): String? = withContext(Dispatchers.IO) {
        val rt = currentRefreshToken ?: return@withContext null
        if (rt.isBlank()) return@withContext null
        val body = json.encodeToString(mapOf("refresh_token" to rt)).toRequestBody(JSON_MEDIA)
        val req = Request.Builder()
            .url("${Config.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token")
            .post(body)
            .apply { authHeaders().forEach { (k, v) -> addHeader(k, v) } }
            .build()
        try {
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext null
                val s = r.body?.string().orEmpty()
                val auth = json.decodeFromString(AuthResponse.serializer(), s)
                val newAccess = auth.access_token ?: return@withContext null
                currentToken = newAccess
                if (!auth.refresh_token.isNullOrBlank()) currentRefreshToken = auth.refresh_token
                onTokensRefreshed?.invoke(newAccess, auth.refresh_token)
                newAccess
            }
        } catch (_: Exception) { null }
    }

    /** Run a request; on 401 try to refresh once and retry. Throws AuthExpiredException if unrecoverable. */
    private suspend fun executeAuthed(token: String, build: (String) -> Request): Response {
        var t = token
        var r = withContext(Dispatchers.IO) { client.newCall(build(t)).execute() }
        if (r.code == 401 || r.code == 403) {
            r.close()
            val newT = refreshAccessToken() ?: run {
                onAuthExpired?.invoke()
                throw AuthExpiredException()
            }
            t = newT
            r = withContext(Dispatchers.IO) { client.newCall(build(t)).execute() }
            if (r.code == 401 || r.code == 403) {
                r.close()
                onAuthExpired?.invoke()
                throw AuthExpiredException()
            }
        }
        return r
    }

    suspend fun signIn(email: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        val body = json.encodeToString(mapOf("email" to email, "password" to password))
            .toRequestBody(JSON_MEDIA)
        val req = Request.Builder()
            .url("${Config.SUPABASE_URL}/auth/v1/token?grant_type=password")
            .post(body)
            .apply { authHeaders().forEach { (k, v) -> addHeader(k, v) } }
            .build()
        client.newCall(req).execute().use { r ->
            val s = r.body?.string().orEmpty()
            json.decodeFromString(AuthResponse.serializer(), s)
        }
    }

    suspend fun getMyEmployee(token: String, userId: String): Employee? {
        val url = "${Config.SUPABASE_URL}/rest/v1/employees?user_id=eq.$userId&select=id,full_name,employee_code,position,department,user_id,is_active&limit=1"
        return executeAuthed(token) { t ->
            Request.Builder().url(url).get()
                .apply { authHeaders(t).forEach { (k, v) -> addHeader(k, v) } }.build()
        }.use { r ->
            if (!r.isSuccessful) return@use null
            val s = r.body?.string().orEmpty()
            try { json.decodeFromString(ListSerializer(Employee.serializer()), s).firstOrNull() }
            catch (_: Exception) { null }
        }
    }

    suspend fun getAllEmployees(token: String): List<Employee> {
        val url = "${Config.SUPABASE_URL}/rest/v1/employees?is_active=eq.true&select=id,full_name,employee_code,position,department,user_id,is_active&order=full_name.asc"
        return executeAuthed(token) { t ->
            Request.Builder().url(url).get()
                .apply { authHeaders(t).forEach { (k, v) -> addHeader(k, v) } }.build()
        }.use { r ->
            if (!r.isSuccessful) return@use emptyList()
            val s = r.body?.string().orEmpty()
            try { json.decodeFromString(ListSerializer(Employee.serializer()), s) }
            catch (_: Exception) { emptyList() }
        }
    }

    suspend fun getTodayAttendance(token: String, employeeId: String, date: String): AttendanceRecord? {
        val url = "${Config.SUPABASE_URL}/rest/v1/attendance_records?employee_id=eq.$employeeId&work_date=eq.$date&select=*&limit=1"
        return executeAuthed(token) { t ->
            Request.Builder().url(url).get()
                .apply { authHeaders(t).forEach { (k, v) -> addHeader(k, v) } }.build()
        }.use { r ->
            if (!r.isSuccessful) return@use null
            val s = r.body?.string().orEmpty()
            try { json.decodeFromString(ListSerializer(AttendanceRecord.serializer()), s).firstOrNull() }
            catch (_: Exception) { null }
        }
    }

    suspend fun checkIn(token: String, record: AttendanceRecord): Boolean {
        val body = json.encodeToString(AttendanceRecord.serializer(), record).toRequestBody(JSON_MEDIA)
        return executeAuthed(token) { t ->
            Request.Builder()
                .url("${Config.SUPABASE_URL}/rest/v1/attendance_records")
                .post(body)
                .apply {
                    authHeaders(t).forEach { (k, v) -> addHeader(k, v) }
                    addHeader("Prefer", "return=minimal")
                }.build()
        }.use { r ->
            if (r.isSuccessful) true
            else throw RuntimeException("HTTP ${r.code}: ${r.body?.string().orEmpty().take(300)}")
        }
    }

    suspend fun updateCheckOut(token: String, recordId: String, checkOut: String): Boolean {
        val body = json.encodeToString(mapOf("check_out" to checkOut)).toRequestBody(JSON_MEDIA)
        return executeAuthed(token) { t ->
            Request.Builder()
                .url("${Config.SUPABASE_URL}/rest/v1/attendance_records?id=eq.$recordId")
                .patch(body)
                .apply {
                    authHeaders(t).forEach { (k, v) -> addHeader(k, v) }
                    addHeader("Prefer", "return=minimal")
                }.build()
        }.use { it.isSuccessful }
    }

    suspend fun getLeaveRequests(token: String, employeeId: String): List<LeaveRequest> {
        val url = "${Config.SUPABASE_URL}/rest/v1/leave_requests?employee_id=eq.$employeeId&select=*&order=created_at.desc&limit=50"
        return executeAuthed(token) { t ->
            Request.Builder().url(url).get()
                .apply { authHeaders(t).forEach { (k, v) -> addHeader(k, v) } }.build()
        }.use { r ->
            if (!r.isSuccessful) return@use emptyList()
            val s = r.body?.string().orEmpty()
            try { json.decodeFromString(ListSerializer(LeaveRequest.serializer()), s) }
            catch (_: Exception) { emptyList() }
        }
    }

    suspend fun createLeaveRequest(token: String, payload: CreateLeavePayload): Boolean {
        val body = json.encodeToString(CreateLeavePayload.serializer(), payload).toRequestBody(JSON_MEDIA)
        return executeAuthed(token) { t ->
            Request.Builder()
                .url("${Config.SUPABASE_URL}/rest/v1/leave_requests")
                .post(body)
                .apply {
                    authHeaders(t).forEach { (k, v) -> addHeader(k, v) }
                    addHeader("Prefer", "return=minimal")
                }.build()
        }.use { r ->
            if (r.isSuccessful) {
                true
            } else {
                val errorMsg = r.body?.string().orEmpty()
                throw RuntimeException("Lỗi tạo đơn (${r.code}): ${errorMsg.take(200)}")
            }
        }
    }

    suspend fun getAttendanceMonth(token: String, employeeId: String, from: String, to: String): List<AttendanceRecord> {
        val url = "${Config.SUPABASE_URL}/rest/v1/attendance_records?employee_id=eq.$employeeId&work_date=gte.$from&work_date=lte.$to&select=*&order=work_date.desc"
        return executeAuthed(token) { t ->
            Request.Builder().url(url).get()
                .apply { authHeaders(t).forEach { (k, v) -> addHeader(k, v) } }.build()
        }.use { r ->
            if (!r.isSuccessful) return@use emptyList()
            val s = r.body?.string().orEmpty()
            try { json.decodeFromString(ListSerializer(AttendanceRecord.serializer()), s) }
            catch (_: Exception) { emptyList() }
        }
    }
}