package vn.kba2018.attendance.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AuthResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val expires_in: Long? = null,
    val user: AuthUser? = null,
    val error: String? = null,
    val error_description: String? = null,
    val msg: String? = null,
)

@Serializable
data class AuthUser(val id: String, val email: String? = null)

@Serializable
data class Employee(
    val id: String,
    val full_name: String,
    val employee_code: String? = null,
    val position: String? = null,
    val department: String? = null,
    val user_id: String? = null,
    val is_active: Boolean = true,
)

@Serializable
data class AttendanceRecord(
    val id: String? = null,
    val employee_id: String,
    val work_date: String,
    val check_in: String? = null,
    val check_out: String? = null,
    val status: String = "present",
    val notes: String? = null,
    val off_site: Boolean = false,
)

@Serializable
data class LeaveRequest(
    val id: String,
    val employee_id: String,
    val leave_type: String,
    val start_date: String,
    val end_date: String,
    val total_days: Double = 1.0,
    val reason: String? = null,
    val status: String,
    val approved_at: String? = null,
    val created_at: String? = null,
)
