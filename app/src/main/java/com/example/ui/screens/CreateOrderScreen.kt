package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer
import com.example.data.model.OrderLine
import com.example.data.model.Product
import com.example.ui.SalesUiState
import com.example.ui.components.DatePickerField
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.MoneyInputField
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.SummaryStatRow
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueSoft
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.util.Formatters
import kotlin.math.round
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    uiState: SalesUiState,
    onClose: () -> Unit,
    onQuickAddCustomer: (String, String, String, String, (Customer) -> Unit) -> Unit,
    onSaveOrder: (String, String, List<OrderLine>, Long, Long, String) -> Unit
) {
    val lines = remember { mutableStateListOf<OrderLine>() }
    var selectedCustomerId by remember { mutableStateOf("") }
    var orderDate by remember { mutableStateOf(Formatters.todayIso()) }
    var note by remember { mutableStateOf("") }
    var surcharge by remember { mutableLongStateOf(0L) }
    var shipFee by remember { mutableLongStateOf(0L) }

    var showPicker by remember { mutableStateOf(false) }
    var askDiscard by remember { mutableStateOf(false) }
    var showInlineNewCustomer by remember { mutableStateOf(false) }
    var customerDropdownOpen by remember { mutableStateOf(false) }

    var newCustName by remember { mutableStateOf("") }
    var newCustPhone by remember { mutableStateOf("") }
    var newCustArea by remember { mutableStateOf("") }
    var newCustAddress by remember { mutableStateOf("") }

    val productMap = remember(uiState.products) { uiState.products.associateBy { it.id } }
    val selectedCustomer = remember(uiState.customers, selectedCustomerId) {
        uiState.customers.find { it.id == selectedCustomerId }
    }

    val goodsTotal = lines.sumOf { (it.price.toDouble() * it.qty) }.roundToLong()
    val costTotal = lines.sumOf { (it.cost.toDouble() * it.qty) }.roundToLong()
    val qtyTotal = lines.sumOf { it.qty }
    val grandTotal = goodsTotal + surcharge
    val estimatedProfit = grandTotal - costTotal - shipFee

    fun attemptClose() {
        if (lines.isNotEmpty()) askDiscard = true else onClose()
    }

    BackHandler(enabled = !showPicker) {
        attemptClose()
    }

    if (showPicker) {
        ProductPickerScreen(
            products = uiState.products,
            initialSelectedIds = lines.mapNotNull { it.productId.takeIf(String::isNotBlank) }.toSet(),
            onBack = { showPicker = false },
            onApply = { pickedIds ->
                val kept = lines.filter { it.productId.isBlank() || pickedIds.contains(it.productId) }.toMutableList()
                pickedIds.forEach { pid ->
                    if (kept.none { it.productId == pid }) {
                        productMap[pid]?.let { p ->
                            kept += OrderLine(
                                productId = p.id,
                                name = p.name,
                                qty = 1.0,
                                price = p.price,
                                cost = p.cost
                            )
                        }
                    }
                }
                lines.clear()
                lines.addAll(kept)
                showPicker = false
            },
            onAddCustom = { pickedIds ->
                val kept = lines.filter { it.productId.isBlank() || pickedIds.contains(it.productId) }.toMutableList()
                pickedIds.forEach { pid ->
                    if (kept.none { it.productId == pid }) {
                        productMap[pid]?.let { p ->
                            kept += OrderLine(
                                productId = p.id,
                                name = p.name,
                                qty = 1.0,
                                price = p.price,
                                cost = p.cost
                            )
                        }
                    }
                }
                kept += OrderLine(productId = "", name = "", qty = 1.0, price = 0L, cost = 0L)
                lines.clear()
                lines.addAll(kept)
                showPicker = false
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm đơn bán", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { attemptClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            onSaveOrder(orderDate, selectedCustomerId, lines.toList(), surcharge, shipFee, note)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandBlue),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_order_top_button")
                    ) {
                        Text("💾 Lưu", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tổng tiền", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = Formatters.money(grandTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandBlue
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hàng hóa", style = MaterialTheme.typography.titleMedium)
                            FilledTonalButton(
                                onClick = { showPicker = true },
                                modifier = Modifier.testTag("open_product_picker_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(" Thêm")
                            }
                        }
                        if (lines.isEmpty()) {
                            EmptyPlaceholder("Chưa có hàng hóa. Bấm “＋ Thêm” để chọn.")
                        }
                    }
                }
            }

            itemsIndexed(lines) { index, line ->
                val prod = productMap[line.productId]
                OrderLineCard(
                    index = index,
                    line = line,
                    productImage = prod?.image.orEmpty(),
                    onUpdate = { updated -> lines[index] = updated },
                    onRemove = { lines.removeAt(index) }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = customerDropdownOpen,
                                onExpandedChange = { customerDropdownOpen = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                val label = selectedCustomer?.let {
                                    if (it.area.isNotBlank()) "${it.name} · ${it.area}" else it.name
                                } ?: "Khách lẻ / chưa lưu"
                                OutlinedTextField(
                                    value = label,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Khách hàng") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownOpen) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = customerDropdownOpen,
                                    onDismissRequest = { customerDropdownOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Khách lẻ / chưa lưu") },
                                        onClick = { selectedCustomerId = ""; customerDropdownOpen = false }
                                    )
                                    uiState.customers.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text(if (c.area.isNotBlank()) "${c.name} · ${c.area}" else c.name) },
                                            onClick = { selectedCustomerId = c.id; customerDropdownOpen = false }
                                        )
                                    }
                                }
                            }
                            OutlinedButton(onClick = { showInlineNewCustomer = !showInlineNewCustomer }) {
                                Text(if (showInlineNewCustomer) "Đóng" else "＋ Thêm")
                            }
                        }

                        SummaryStatRow("📞 SĐT", selectedCustomer?.phone?.ifBlank { "Không có" } ?: "Không có")
                        val addr = listOf(selectedCustomer?.area.orEmpty(), selectedCustomer?.address.orEmpty())
                            .filter { it.isNotBlank() }.joinToString(" · ")
                        SummaryStatRow("🏠 Địa chỉ", addr.ifBlank { "Không có" })

                        AnimatedVisibility(visible = showInlineNewCustomer) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newCustName,
                                    onValueChange = { newCustName = it },
                                    label = { Text("Tên khách hàng *") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newCustPhone,
                                    onValueChange = { newCustPhone = it },
                                    label = { Text("SĐT") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newCustArea,
                                    onValueChange = { newCustArea = it },
                                    label = { Text("Khu vực") },
                                    placeholder = { Text("Từ Sơn, Bắc Ninh") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newCustAddress,
                                    onValueChange = { newCustAddress = it },
                                    label = { Text("Địa chỉ") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        onQuickAddCustomer(newCustName, newCustPhone, newCustArea, newCustAddress) { saved ->
                                            selectedCustomerId = saved.id
                                            showInlineNewCustomer = false
                                            newCustName = ""
                                            newCustPhone = ""
                                            newCustArea = ""
                                            newCustAddress = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Lưu khách hàng")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DatePickerField(
                            label = "Ngày bán",
                            isoDate = orderDate,
                            onDateSelected = { orderDate = it }
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Ghi chú") },
                            placeholder = { Text("Giao sáng trước 9h...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MoneyInputField(
                            label = "Phụ phí (thu thêm)",
                            amount = surcharge,
                            onAmountChange = { surcharge = it }
                        )
                        MoneyInputField(
                            label = "Phí vận chuyển (trừ đi)",
                            amount = shipFee,
                            onAmountChange = { shipFee = it }
                        )
                        Spacer(Modifier.height(4.dp))
                        SummaryStatRow("Số lượng", "${Formatters.qty(qtyTotal)} kg")
                        SummaryStatRow("Tiền hàng", Formatters.money(goodsTotal))
                        SummaryStatRow("Phụ phí", "+ ${Formatters.money(surcharge)}")
                        SummaryStatRow("Tổng khách trả", Formatters.money(grandTotal))
                        SummaryStatRow("Giá vốn", Formatters.money(costTotal))
                        SummaryStatRow("Phí vận chuyển", "− ${Formatters.money(shipFee)}")
                        SummaryStatRow(
                            label = "Lợi nhuận dự kiến",
                            value = Formatters.money(estimatedProfit),
                            valueColor = if (estimatedProfit < 0) LossRed else ProfitGreen,
                            boldLabel = true
                        )
                    }
                }
            }
        }
    }

    if (askDiscard) {
        AlertDialog(
            onDismissRequest = { askDiscard = false },
            title = { Text("Hủy tạo đơn?") },
            text = { Text("Bỏ đơn hàng đang tạo?") },
            confirmButton = {
                Button(
                    onClick = { askDiscard = false; onClose() },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed, contentColor = Color.White)
                ) {
                    Text("Bỏ đơn")
                }
            },
            dismissButton = {
                TextButton(onClick = { askDiscard = false }) { Text("Tiếp tục sửa") }
            }
        )
    }
}

@Composable
private fun OrderLineCard(
    index: Int,
    line: OrderLine,
    productImage: String,
    onUpdate: (OrderLine) -> Unit,
    onRemove: () -> Unit
) {
    var qtyText by remember(line.productId, line.qty) {
        mutableStateOf(Formatters.qty(line.qty).replace(',', '.'))
    }
    val lineTotal = (line.price.toDouble() * line.qty).roundToLong()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            ProductThumbnail(imageDataUri = productImage, size = 56.dp)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (line.productId.isNotBlank()) {
                        Text(
                            text = "${index + 1}. ${line.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        OutlinedTextField(
                            value = line.name,
                            onValueChange = { onUpdate(line.copy(name = it)) },
                            placeholder = { Text("Tên sản phẩm ngoài...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Xóa dòng", tint = LossRed)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val next = round((line.qty - 1.0) * 100.0) / 100.0
                            val safe = if (next <= 0.0) 0.5 else next
                            onUpdate(line.copy(qty = safe))
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("−", style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { txt ->
                            qtyText = txt
                            val parsed = Formatters.parseQty(txt)
                            if (parsed > 0.0) {
                                onUpdate(line.copy(qty = parsed))
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.width(76.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            val next = round((line.qty + 1.0) * 100.0) / 100.0
                            onUpdate(line.copy(qty = next))
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("+", style = MaterialTheme.typography.titleMedium)
                    }

                    Text("kg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.weight(1f))
                    Text(
                        text = Formatters.money(lineTotal),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandBlue
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoneyInputField(
                        label = "Đơn giá / 1kg",
                        amount = line.price,
                        onAmountChange = { onUpdate(line.copy(price = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    if (line.productId.isBlank()) {
                        MoneyInputField(
                            label = "Giá vốn / 1kg",
                            amount = line.cost,
                            onAmountChange = { onUpdate(line.copy(cost = it)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPickerScreen(
    products: List<Product>,
    initialSelectedIds: Set<String>,
    onBack: () -> Unit,
    onApply: (Set<String>) -> Unit,
    onAddCustom: (Set<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>().apply { addAll(initialSelectedIds) } }

    BackHandler { onBack() }

    val filtered = remember(products, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) products else products.filter { it.name.lowercase().contains(q) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chọn hàng hóa", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAddCustom(selected.toSet()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("＋ Sản phẩm ngoài")
                    }
                    Button(
                        onClick = { onApply(selected.toSet()) },
                        modifier = Modifier.weight(1f).testTag("picker_confirm_button")
                    ) {
                        Text("Tiếp tục (${selected.size})")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Lọc theo tên hàng...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
            if (filtered.isEmpty()) {
                EmptyPlaceholder("Chưa có sản phẩm.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { p ->
                        val isChecked = selected.contains(p.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isChecked) BrandBlueSoft else MaterialTheme.colorScheme.surface)
                                .clickable {
                                    if (isChecked) selected.remove(p.id) else selected.add(p.id)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                ProductThumbnail(imageDataUri = p.image, size = 56.dp)
                                if (isChecked) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(BrandBlue.copy(alpha = 0.8f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Đã chọn",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = p.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Formatters.money(p.price),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandBlue
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
