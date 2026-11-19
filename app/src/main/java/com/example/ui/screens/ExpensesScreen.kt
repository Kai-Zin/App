package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Expense
import com.example.domain.SalesAnalytics
import com.example.ui.SalesUiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.MoneyInputField
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueSoft
import com.example.ui.theme.LossRed
import com.example.ui.theme.WarningAmberSoft
import com.example.ui.theme.WarningAmberText
import com.example.util.Formatters

@Composable
fun ExpensesScreen(
    uiState: SalesUiState,
    onSaveExpense: (String, String, String, String, Long, String, String) -> Unit,
    onDeleteExpense: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Expense?>(null) }

    val sortedExpenses = remember(uiState.expenses) {
        uiState.expenses.sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.updatedAtMs })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .padding(top = 10.dp)
                .testTag("add_expense_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Ghi chi phí")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            if (sortedExpenses.isEmpty()) {
                EmptyPlaceholder("Chưa có chi phí")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    items(sortedExpenses, key = { it.id }) { e ->
                        val isAds = SalesAnalytics.isAdsExpense(e)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .testTag("expense_row_${e.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (isAds) WarningAmberSoft else BrandBlueSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAds) Icons.Default.Campaign else Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = if (isAds) WarningAmberText else BrandBlue
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = e.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val typeLabel = if (isAds) "Quảng cáo" else "Chi phí khác"
                                val catExtra = if (e.category.isNotBlank() && e.category != "Quảng cáo") " · ${e.category}" else ""
                                Text(
                                    text = "${Formatters.dateVi(e.date)} · $typeLabel$catExtra",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val orderTag = if (e.orderId.isNotBlank()) "Gắn với đơn ${e.orderId}" else "Chi phí chung"
                                val noteExtra = if (e.note.isNotBlank()) " · ${e.note}" else ""
                                Text(
                                    text = "$orderTag$noteExtra",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Formatters.money(e.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                IconButton(onClick = { deleteTarget = e }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Xóa chi phí",
                                        tint = LossRed
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ExpenseFormDialog(
            uiState = uiState,
            onDismiss = { showAddDialog = false },
            onSave = { date, name, category, type, amount, orderId, note ->
                onSaveExpense(date, name, category, type, amount, orderId, note)
                showAddDialog = false
            }
        )
    }

    deleteTarget?.let { e ->
        ConfirmDeleteDialog(
            message = "Bạn chắc chắn muốn xóa khoản chi \"${e.name}\"?",
            onConfirm = {
                onDeleteExpense(e.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFormDialog(
    uiState: SalesUiState,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long, String, String) -> Unit
) {
    var date by remember { mutableStateOf(Formatters.todayIso()) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("other") }
    var category by remember { mutableStateOf("Vận chuyển") }
    var amount by remember { mutableLongStateOf(0L) }
    var orderId by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var typeExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }
    var orderExpanded by remember { mutableStateOf(false) }

    val customerMap = remember(uiState.customers) { uiState.customers.associateBy { it.id } }
    val categories = listOf("Vận chuyển", "Đóng gói", "Quảng cáo", "Đi lại", "Khác")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ghi nhận chi phí") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DatePickerField(
                    label = "Ngày *",
                    isoDate = date,
                    onDateSelected = { date = it }
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên khoản chi *") },
                    placeholder = { Text("Facebook Ads, vận chuyển, đóng gói...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_name_input")
                )
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (type == "ads") "Chi phí quảng cáo" else "Chi phí khác",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Loại chi phí") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Chi phí quảng cáo") },
                            onClick = { type = "ads"; typeExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Chi phí khác") },
                            onClick = { type = "other"; typeExpanded = false }
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Nhóm chi phí") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = { category = c; catExpanded = false }
                            )
                        }
                    }
                }

                MoneyInputField(
                    label = "Số tiền (đ) *",
                    amount = amount,
                    onAmountChange = { amount = it },
                    testTag = "expense_amount_input"
                )

                ExposedDropdownMenuBox(
                    expanded = orderExpanded,
                    onExpandedChange = { orderExpanded = it }
                ) {
                    val selectedOrderLabel = if (orderId.isBlank()) {
                        "Chi phí chung"
                    } else {
                        val ord = uiState.orders.find { it.id == orderId }
                        val cName = customerMap[ord?.customerId]?.name ?: "Khách lẻ"
                        "$orderId · $cName"
                    }
                    OutlinedTextField(
                        value = selectedOrderLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gắn với đơn hàng") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = orderExpanded,
                        onDismissRequest = { orderExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Chi phí chung") },
                            onClick = { orderId = ""; orderExpanded = false }
                        )
                        uiState.orders.forEach { o ->
                            val cName = customerMap[o.customerId]?.name ?: "Khách lẻ"
                            DropdownMenuItem(
                                text = { Text("${o.id} · $cName") },
                                onClick = { orderId = o.id; orderExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(date, name, category, type, amount, orderId, note) },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("Lưu chi phí")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
