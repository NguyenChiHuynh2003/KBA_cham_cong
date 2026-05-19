package vn.kba2018.attendance

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionStore(private val ctx: Context) {
    private val TOKEN = stringPreferencesKey("token")
    private val REFRESH = stringPreferencesKey("refresh_token")
    private val USER_ID = stringPreferencesKey("user_id")
    private val EMP_ID = stringPreferencesKey("emp_id")
    private val EMP_NAME = stringPreferencesKey("emp_name")
    private val EMP_POS = stringPreferencesKey("emp_pos")
    private val EMP_DEP = stringPreferencesKey("emp_dep")

    data class Session(
        val token: String?,
        val refreshToken: String?,
        val userId: String?,
        val employeeId: String?,
        val employeeName: String?,
        val position: String?,
        val department: String?,
    )

    val session: Flow<Session> = ctx.dataStore.data.map { p ->
        Session(p[TOKEN], p[REFRESH], p[USER_ID], p[EMP_ID], p[EMP_NAME], p[EMP_POS], p[EMP_DEP])
    }

    suspend fun save(token: String, refreshToken: String?, userId: String, empId: String, name: String, pos: String?, dep: String?) {
        ctx.dataStore.edit {
            it[TOKEN] = token
            it[REFRESH] = refreshToken.orEmpty()
            it[USER_ID] = userId
            it[EMP_ID] = empId
            it[EMP_NAME] = name
            it[EMP_POS] = pos.orEmpty()
            it[EMP_DEP] = dep.orEmpty()
        }
    }

    suspend fun updateTokens(token: String, refreshToken: String?) {
        ctx.dataStore.edit {
            it[TOKEN] = token
            if (!refreshToken.isNullOrBlank()) it[REFRESH] = refreshToken
        }
    }

    suspend fun clear() { ctx.dataStore.edit { it.clear() } }
}
