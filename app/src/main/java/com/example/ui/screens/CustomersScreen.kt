package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Customer
import com.example.data.model.CustomerFilter
import com.example.domain.SalesAnalytics
import com.example.ui.SalesUiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.StatusTag
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueSoft
import com.example.ui.theme.LossRed
import com.example.util.Formatters

@Composable
fun CustomersScreen(
    uiState: SalesUiState,
    filter: CustomerFilter,
    onFilterChange: (CustomerFilter) -> Unit,
    onSaveCustomer: (String?, String, String, String, String, String) -> Unit,
    onDeleteCustomer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Customer?>(null) }

    val summaries = remember(uiState.customers, uiState.orders, filter, query) {
        val q = query.trim().lowercase()
        SalesAnalytics.buildCustomerCareList(uiState.customers, uiState.orders)
            .filter { s ->
                when (filter) {
                    CustomerFilter.ALL -> true
                    CustomerFilter.D15 -> (s.daysSinceLastOrder ?: -1) >= 15
                    CustomerFilter.D30 -> (s.daysSinceLastOrder ?: -1) >= 30
                    CustomerFilter.D60 -> (s.daysSinceLastOrder ?: -1) >= 60
                    CustomerFilter.NEVER -> s.daysSinceLastOrder == null
                }
            }
            .filter { s ->
                if (q.isEmpty()) true
                else "${s.customer.name} ${s.customer.phone} ${s.customer.area} ${s.customer.address}".lowercase().contains(q)
            }
            .sortedByDescending { it.daysSinceLastOrder ?: -1 }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Lọc theo tên, SĐT, khu vực...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("customers_search_input")
            )
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_customer_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Thêm")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomerFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { onFilterChange(f) },
                    label = { Text(f.label) }
                )
            }
            Text(
                text = "Hiển thị ${summaries.size} khách",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            if (summaries.isEmpty()) {
                EmptyPlaceholder("Không có khách hàng trong nhóm này")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    items(summaries, key = { it.customer.id }) { s ->
                        val c = s.customer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingCustomer = c }
                                .padding(vertical = 12.dp)
                                .testTag("customer_row_${c.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(BrandBlueSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlue)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = c.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "📞 ${c.phone.ifBlank { "Không có" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val loc = listOf(c.area, c.address).filter { it.isNotBlank() }.joinToString(" · ")
                                Text(
                                    text = "🏠 ${loc.ifBlank { "Không có" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${s.orders.size} đơn · ${Formatters.money(s.totalSpent)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    when {
                                        s.daysSinceLastOrder == null -> StatusTag("Chưa có đơn", isWarning = true)
                                        s.daysSinceLastOrder >= 30 -> StatusTag("${s.daysSinceLastOrder} ngày chưa nhập", isWarning = true)
                                        else -> Text(
                                            text = "${s.daysSinceLastOrder} ngày trước",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { deleteTarget = c }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Xóa khách hàng",
                                    tint = LossRed
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CustomerFormDialog(
            title = "Thêm khách hàng",
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, area, address, note ->
                onSaveCustomer(null, name, phone, area, address, note)
                showAddDialog = false
            }
        )
    }

    editingCustomer?.let { c ->
        CustomerFormDialog(
            title = "Chỉnh sửa khách hàng",
            initial = c,
            onDismiss = { editingCustomer = null },
            onSave = { name, phone, area, address, note ->
                onSaveCustomer(c.id, name, phone, area, address, note)
                editingCustomer = null
            }
        )
    }

    deleteTarget?.let { c ->
        ConfirmDeleteDialog(
            message = "Bạn chắc chắn muốn xóa khách hàng \"${c.name}\"?",
            onConfirm = {
                onDeleteCustomer(c.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun CustomerFormDialog(
    title: String,
    initial: Customer?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var area by remember { mutableStateOf(initial?.area.orEmpty()) }
    var address by remember { mutableStateOf(initial?.address.orEmpty()) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên khách hàng *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Khu vực / tỉnh thành") },
                    placeholder = { Text("Ví dụ: Từ Sơn, Bắc Ninh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Địa chỉ chi tiết") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, area, address, note) },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text(if (initial == null) "Lưu khách hàng" else "Lưu thay đổi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
