package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.SalesUiState
import com.example.ui.components.SummaryStatRow
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueSoft
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: SalesUiState,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit,
    onSaveConfig: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onRefreshCloud: () -> Unit,
    onGenerateBackupJson: suspend () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onGenerateOrdersCsv: suspend () -> String,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cfg = uiState.cloudPrefs

    var sbUrl by remember(cfg.supabaseUrl) { mutableStateOf(cfg.supabaseUrl) }
    var sbKey by remember(cfg.supabaseKey) { mutableStateOf(cfg.supabaseKey) }
    var email by remember(cfg.userEmail) { mutableStateOf(cfg.userEmail) }
    var password by remember { mutableStateOf("") }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val content = onGenerateBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(content.toByteArray(Charsets.UTF_8))
                        }
                    }
                }.onSuccess {
                    onNotify("✅ Đã xuất file sao lưu JSON!")
                }.onFailure {
                    onNotify("⚠ Lỗi xuất JSON: ${it.message}")
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                if (!text.isNullOrBlank()) {
                    pendingRestoreJson = text
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val content = onGenerateOrdersCsv()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(content.toByteArray(Charsets.UTF_8))
                        }
                    }
                }.onSuccess {
                    onNotify("✅ Đã xuất đơn hàng CSV!")
                }.onFailure {
                    onNotify("⚠ Lỗi xuất CSV: ${it.message}")
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kích thước chữ & ô giao diện", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${(uiScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            "Rất nhỏ" to 0.80f,
                            "Gọn" to 0.90f,
                            "Vừa" to 1.00f,
                            "Lớn" to 1.12f
                        )
                        presets.forEach { (label, value) ->
                            FilterChip(
                                selected = kotlin.math.abs(uiScale - value) < 0.03f,
                                onClick = { onUiScaleChange(value) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Slider(
                        value = uiScale,
                        onValueChange = { onUiScaleChange((it * 20).roundToInt() / 20f) },
                        valueRange = 0.75f..1.20f,
                        steps = 8,
                        modifier = Modifier.testTag("ui_scale_slider")
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Trạng thái đồng bộ", style = MaterialTheme.typography.titleMedium)
                    val lastTime = if (cfg.lastSyncOkMs > 0L) {
                        " · Đồng bộ gần nhất: " + SimpleDateFormat("HH:mm:ss", Locale("vi", "VN")).format(Date(cfg.lastSyncOkMs))
                    } else ""
                    Text(
                        text = (if (cfg.isSignedIn) "Đã đăng nhập: ${cfg.userEmail}" else "Chưa đăng nhập") + lastTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.syncMessage.isNotBlank()) {
                        Text(
                            text = uiState.syncMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = onRefreshCloud,
                        modifier = Modifier.testTag("refresh_cloud_button")
                    ) {
                        Text("🔄 Tải dữ liệu mới từ Cloud")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kết nối Supabase (Đồng bộ tách bảng siêu tốc)", style = MaterialTheme.typography.titleMedium)
                    Surface(
                        color = BrandBlueSoft,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (cfg.isSignedIn) {
                                "Đã đăng nhập: ${cfg.userEmail} · Đang dùng chế độ đồng bộ từng mục riêng biệt."
                            } else {
                                "Nhập Project URL và publishable key rồi đăng nhập."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandBlue,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = sbUrl,
                        onValueChange = { sbUrl = it },
                        label = { Text("Supabase Project URL") },
                        placeholder = { Text("https://xxxxxxxx.supabase.co") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sb_url_input")
                    )
                    OutlinedTextField(
                        value = sbKey,
                        onValueChange = { sbKey = it },
                        label = { Text("Supabase publishable key") },
                        placeholder = { Text("sb_publishable_... hoặc anon key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sb_key_input")
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        placeholder = { Text("email của bạn") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sb_email_input")
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        placeholder = { Text("Mật khẩu tài khoản") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sb_password_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSaveConfig(sbUrl, sbKey) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Lưu cấu hình")
                        }
                        OutlinedButton(
                            onClick = { onSignUp(email, password) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tạo tài khoản")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSignIn(email, password) },
                            modifier = Modifier.weight(1f).testTag("sb_signin_button")
                        ) {
                            Text("Đăng nhập & tải")
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Đăng xuất")
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sao lưu và khôi phục dữ liệu", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                exportJsonLauncher.launch("mysales-backup-${Formatters.todayIso()}.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("↓ Xuất JSON")
                        }
                        OutlinedButton(
                            onClick = {
                                importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("↑ Nhập JSON")
                        }
                    }

                    OutlinedButton(
                        onClick = { exportCsvLauncher.launch("mysales-don-hang.csv") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Xuất đơn hàng CSV")
                    }

                    Spacer(Modifier.height(4.dp))
                    SummaryStatRow("Đơn hàng", uiState.orders.size.toString())
                    SummaryStatRow("Khách hàng", uiState.customers.size.toString())
                    SummaryStatRow("Sản phẩm", uiState.products.size.toString())
                    SummaryStatRow("Khoản chi", uiState.expenses.size.toString())
                }
            }
        }
    }

    pendingRestoreJson?.let { raw ->
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = { Text("Khôi phục dữ liệu") },
            text = { Text("Khôi phục sẽ nạp toàn bộ dữ liệu từ file lên máy và đồng bộ lên 4 bảng đám mây (nếu đã đăng nhập). Tiếp tục?") },
            confirmButton = {
                Button(onClick = {
                    onRestoreBackupJson(raw)
                    pendingRestoreJson = null
                }) {
                    Text("Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreJson = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}
