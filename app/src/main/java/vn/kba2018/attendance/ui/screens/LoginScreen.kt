@file:Suppress("SpellCheckingInspection")

package vn.kba2018.attendance.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import vn.kba2018.attendance.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import vn.kba2018.attendance.SessionStore
import vn.kba2018.attendance.api.SupabaseApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(store: SessionStore, onSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("KBA Chấm công") }) }) { pad ->
        Column(
            Modifier.padding(pad).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_kba),
                contentDescription = "KBA Logo",
                modifier = Modifier.size(140.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Đăng nhập", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = pass, onValueChange = { pass = it },
                label = { Text("Mật khẩu") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                enabled = !loading && email.isNotBlank() && pass.isNotBlank(),
                onClick = {
                    error = null; loading = true
                    scope.launch {
                        try {
                            val res = SupabaseApi.signIn(email.trim(), pass)
                            val token = res.access_token; val uid = res.user?.id
                            if (token == null || uid == null) {
                                error = res.error_description ?: res.msg ?: "Đăng nhập thất bại"
                            } else {
                                val emp = SupabaseApi.getMyEmployee(token, uid)
                                if (emp == null) {
                                    error = "Không tìm thấy hồ sơ nhân viên"
                                } else if (!emp.is_active) {
                                    error = "Tài khoản đã bị vô hiệu hoá"
                                } else {
                                    store.save(token, res.refresh_token, uid, emp.id, emp.full_name, emp.position, emp.department)
                                    onSuccess()
                                }
                            }
                        } catch (e: Exception) { error = e.message ?: "Lỗi kết nối" }
                        finally { loading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("Đăng nhập")
            }
            Spacer(Modifier.height(16.dp))
            Text("Sử dụng tài khoản nội bộ kba2018.vn", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
