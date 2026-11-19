package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.ui.SalesUiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.MoneyInputField
import com.example.ui.components.ProductThumbnail
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueSoft
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.util.Formatters
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProductsScreen(
    uiState: SalesUiState,
    onSaveProduct: (String?, String, Long, Long, String, String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var deleteTarget by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = BrandBlueSoft,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Text(
                text = "Ảnh sản phẩm được nén nhẹ và lưu riêng biệt, không làm chậm lúc mở ứng dụng.",
                style = MaterialTheme.typography.bodySmall,
                color = BrandBlue,
                modifier = Modifier.padding(12.dp)
            )
        }

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.testTag("add_product_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Thêm sản phẩm")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            if (uiState.products.isEmpty()) {
                EmptyPlaceholder("Chưa có sản phẩm")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    items(uiState.products, key = { it.id }) { p ->
                        val margin = p.price - p.cost
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingProduct = p }
                                .padding(vertical = 12.dp)
                                .testTag("product_row_${p.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProductThumbnail(imageDataUri = p.image, size = 56.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Bán: ${Formatters.money(p.price)} · Vốn: ${Formatters.money(p.cost)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Lãi dự kiến:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Formatters.money(margin),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (margin < 0) LossRed else ProfitGreen
                                    )
                                }
                                if (p.note.isNotBlank()) {
                                    Text(
                                        text = p.note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { deleteTarget = p }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Xóa sản phẩm",
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
        ProductFormDialog(
            title = "Thêm sản phẩm",
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, cost, price, note, image ->
                onSaveProduct(null, name, cost, price, note, image)
                showAddDialog = false
            }
        )
    }

    editingProduct?.let { p ->
        ProductFormDialog(
            title = "Chỉnh sửa sản phẩm",
            initial = p,
            onDismiss = { editingProduct = null },
            onSave = { name, cost, price, note, image ->
                onSaveProduct(p.id, name, cost, price, note, image)
                editingProduct = null
            }
        )
    }

    deleteTarget?.let { p ->
        ConfirmDeleteDialog(
            message = "Bạn chắc chắn muốn xóa sản phẩm \"${p.name}\"?",
            onConfirm = {
                onDeleteProduct(p.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun ProductFormDialog(
    title: String,
    initial: Product?,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var cost by remember { mutableLongStateOf(initial?.cost ?: 0L) }
    var price by remember { mutableLongStateOf(initial?.price ?: 0L) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var imageDataUri by remember { mutableStateOf(initial?.image.orEmpty()) }
    var compressing by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            compressing = true
            scope.launch {
                val compressed = withContext(Dispatchers.IO) {
                    ImageUtils.compressUriToDataUri(context, uri)
                }
                if (compressed != null) {
                    imageDataUri = compressed
                }
                compressing = false
            }
        }
    }

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
                    label = { Text("Tên sản phẩm *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )
                MoneyInputField(
                    label = "Giá vốn (đ)",
                    amount = cost,
                    onAmountChange = { cost = it },
                    testTag = "product_cost_input"
                )
                MoneyInputField(
                    label = "Giá bán mặc định (đ)",
                    amount = price,
                    onAmountChange = { price = it },
                    testTag = "product_price_input"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProductThumbnail(imageDataUri = imageDataUri, size = 72.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(if (compressing) " Đang nén ảnh..." else " Chọn ảnh")
                        }
                        if (imageDataUri.isNotBlank()) {
                            TextButton(onClick = { imageDataUri = "" }) {
                                Text("Xóa ảnh", color = LossRed)
                            }
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
                onClick = { onSave(name, cost, price, note, imageDataUri) },
                enabled = !compressing,
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(if (initial == null) "Lưu sản phẩm" else "Lưu thay đổi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
